import { test, expect } from '@drownek/plugwright';
import * as fs from 'fs';
import * as http from 'http';
import * as path from 'path';
import { fileURLToPath } from 'url';
import { WebSocketServer } from 'ws';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_PATH = path.resolve(__dirname, '../../../../../build/plugwright-run/plugins/MyPet/config.yml');
const MOCK_PORT = 25598;

/**
 * Minimal bytebin+bytesocks stand-in: GET /create hands out a channel id,
 * POST /post captures the envelope, and a ws server accepts the upgrade so
 * `/mypet editor` completes without touching the real relay.
 *
 * `wss` is built with `noServer: true` and wired through the http server's own
 * `upgrade` event rather than `new WebSocketServer({ server })`. Reason: both
 * `WebEditorSocket#createChannel` (GET /create) and `BytebinClient#post` (POST
 * /post) build a plain `HttpClient` without pinning `.version(HTTP_1_1)`, so the
 * JDK defaults to HTTP_2 and probes plaintext requests with `Connection: Upgrade`
 * / `Upgrade: h2c` (prior-knowledge h2c) before falling back. Node routes *any*
 * request carrying an `Upgrade` header through the `upgrade` event once a listener
 * is attached — bypassing `request` entirely — so `{ server }`'s auto-wiring made
 * `ws` see a non-"websocket" Upgrade value on these plain HTTP calls and reject
 * them with `400 Invalid Upgrade header` / a socket reset (surfaced in-game as
 * "bytesocks create failed: HTTP 400" and, once /create was special-cased alone,
 * "HTTP/1.1 header parser received no bytes" on the follow-up POST /post).
 * Handling `upgrade` manually lets non-websocket Upgrade attempts on either route
 * fall back to the same hand-rolled HTTP/1.1 response used for a normal request.
 */
function startMockRelay(): { envelope: Promise<any>; close: () => void } {
  let resolveEnvelope: (v: any) => void;
  const envelope = new Promise<any>((resolve) => { resolveEnvelope = resolve; });

  // Shared route logic for both the normal `request` path and the h2c-probe
  // fallback off `upgrade` below — same three routes, same responses.
  function route(method: string | undefined, url: string | undefined, body: string): { status: number; json: any } | null {
    if (method === 'GET' && url === '/create') {
      return { status: 201, json: { key: 'testchannel' } };
    }
    if (method === 'POST' && url === '/post') {
      resolveEnvelope(JSON.parse(body));
      return { status: 201, json: { key: 'testenvelope' } };
    }
    return null;
  }

  const server = http.createServer((req, res) => {
    let body = '';
    req.on('data', (chunk) => { body += chunk; });
    req.on('end', () => {
      const result = route(req.method, req.url, body);
      if (result) {
        res.writeHead(result.status, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(result.json));
      } else {
        res.writeHead(404);
        res.end();
      }
    });
  });

  // Track live raw sockets so `close()` can force-drop anything still in-flight
  // (an open WebSocket upgrade, or a keep-alive HTTP socket) — `server.close()`/
  // `wss.close()` alone only stop accepting *new* connections and would otherwise
  // leave the process waiting on these to end naturally, leaking past the test.
  const liveSockets = new Set<import('net').Socket>();
  server.on('connection', (socket) => {
    liveSockets.add(socket);
    socket.on('close', () => liveSockets.delete(socket));
  });

  const wss = new WebSocketServer({ noServer: true });
  wss.on('connection', () => { /* accept and idle */ });

  server.on('upgrade', (req, socket, head) => {
    const upgradeHeader = (req.headers['upgrade'] || '').toLowerCase();
    if (upgradeHeader === 'websocket') {
      wss.handleUpgrade(req, socket, head, (ws) => wss.emit('connection', ws, req));
      return;
    }
    // Non-websocket Upgrade attempt (the h2c probe) — buffer the body (if any) off
    // `head` + further socket data until Content-Length is satisfied, then respond
    // with a hand-written HTTP/1.1 response on the raw socket.
    const chunks: Buffer[] = head && head.length ? [head] : [];
    const contentLength = parseInt(String(req.headers['content-length'] || '0'), 10);
    let received = chunks.reduce((n, b) => n + b.length, 0);

    const respond = () => {
      const result = route(req.method, req.url, Buffer.concat(chunks).toString('utf8'));
      if (!result) {
        socket.destroy();
        return;
      }
      const responseBody = JSON.stringify(result.json);
      socket.end(
        `HTTP/1.1 ${result.status} ${result.status === 201 ? 'Created' : 'OK'}\r\n` +
        'Content-Type: application/json\r\n' +
        `Content-Length: ${Buffer.byteLength(responseBody)}\r\n` +
        'Connection: close\r\n\r\n' + responseBody,
      );
    };

    if (received >= contentLength) {
      respond();
    } else {
      socket.on('data', (chunk: Buffer) => {
        chunks.push(chunk);
        received += chunk.length;
        if (received >= contentLength) {
          respond();
        }
      });
    }
  });

  server.listen(MOCK_PORT, '127.0.0.1');
  return {
    envelope,
    close: () => {
      for (const client of wss.clients) {
        client.terminate();
      }
      wss.close();
      server.close();
      for (const socket of liveSockets) {
        socket.destroy();
      }
    },
  };
}

function pointEditorAtMock(): void {
  let text = fs.readFileSync(CONFIG_PATH, 'utf8');

  const bytebinRe = /^(\s*BytebinUrl:\s*).*$/m;
  if (!bytebinRe.test(text)) {
    throw new Error(`BytebinUrl key not found in ${CONFIG_PATH} — has the server booted at least once?`);
  }
  text = text.replace(bytebinRe, `$1"http://127.0.0.1:${MOCK_PORT}"`);

  const bytesocksRe = /^(\s*BytesocksUrl:\s*).*$/m;
  if (!bytesocksRe.test(text)) {
    throw new Error(`BytesocksUrl key not found in ${CONFIG_PATH} — has the server booted at least once?`);
  }
  text = text.replace(bytesocksRe, `$1"ws://127.0.0.1:${MOCK_PORT}"`);

  fs.writeFileSync(CONFIG_PATH, text, 'utf8');
}

test('mypet editor envelope contains skill metadata', async ({ server, player }) => {
  // /mypet editor is admin-gated (MyPet.admin.editor / MyPet.admin bundle) — the harness's
  // bot player is not opped by default (see systems/environment.spec.ts and commands/
  // admin-commands.spec.ts, which all call makeOp() before an admin command).
  await player.makeOp();

  const mock = startMockRelay();
  try {
    pointEditorAtMock();
    const since = player.getMessageBufferIndex();
    player.chat('/mypet reload config');
    await expect(player).toHaveReceivedMessage('config reloaded!', { since, timeout: 6000 });

    player.chat('/mypet editor');
    const envelope = await mock.envelope;

    const skills = envelope.configs.skills;
    expect(Array.isArray(skills)).toBe(true);
    expect(skills.length).toBe(21);
    const ids = skills.map((s: any) => s.id);
    expect(ids).toContain('Thorns');
    expect(ids).toContain('Beacon');

    const thorns = skills.find((s: any) => s.id === 'Thorns');
    expect(thorns.label.en).toBeTruthy();
    // The harness's `expect()` has no `objectContaining`/array-of-matchers support (see
    // node_modules/@drownek/plugwright/dist/lib/expect.d.ts) — assert length + per-field
    // `toMatchObject` (partial match) instead of a single `toEqual` on the whole array.
    expect(thorns.fields.length).toBe(2);
    expect(thorns.fields[0]).toMatchObject({ name: 'chance', type: 'integer', suffix: '%', cumulative: true });
    expect(thorns.fields[1]).toMatchObject({ name: 'reflection', type: 'integer', suffix: '%', cumulative: true });
    expect(thorns.fields[0].label.en).toBe('Chance %');

    const ranged = skills.find((s: any) => s.id === 'Ranged');
    const projectile = ranged.fields.find((f: any) => f.name === 'projectile');
    expect(projectile.type).toBe('enum');
    expect(projectile.values).toEqual([
      'Arrow', 'Snowball', 'LargeFireball', 'SmallFireball', 'WitherSkull',
      'Egg', 'DragonFireball', 'Trident', 'EnderPearl', 'LlamaSpit',
    ]);

    const beacon = skills.find((s: any) => s.id === 'Beacon');
    const buffs = beacon.fields.find((f: any) => f.name === 'buffs');
    expect(buffs.type).toBe('group');
    expect(buffs.fields.map((f: any) => f.name)).toContain('haste');
  } finally {
    mock.close();
  }
});
