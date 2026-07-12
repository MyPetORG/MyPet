import { test, expect, waitUntil } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { msgFragment, msgPlain } from '../lib/locale.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, ARENA } from '../lib/world.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

/**
 * Flattens a decoded `anonymousNbt` chat component (the shape minecraft-protocol hands
 * back for `action_bar`/`system_chat` packets) into plain text. Reimplemented locally
 * instead of pulling in prismarine-chat since this decodes off the raw protocol client.
 */
function flattenChatComponent(node: any): string {
  if (node == null) return '';
  if (node.type === 'string') return node.value;
  if (node.type === 'compound') return flattenChatFields(node.value);
  if (node.type === 'list' && node.value && Array.isArray(node.value.value)) {
    const elementType = node.value.type;
    return node.value.value
      .map((item: any) => elementType === 'compound' ? flattenChatFields(item) : flattenChatComponent({ type: elementType, value: item }))
      .join('');
  }
  return '';
}
function flattenChatFields(fields: any): string {
  if (!fields) return '';
  let out = '';
  if (fields.text) out += flattenChatComponent(fields.text);
  if (fields['']) out += flattenChatComponent(fields['']);
  if (fields.extra) out += flattenChatComponent(fields.extra);
  return out;
}

/**
 * `Player#sendActionBar` sends a dedicated `action_bar` packet, not `system_chat` (what
 * chat/tellraw uses) -- mineflayer has no built-in handler for it, so it never reaches
 * plugwright's message buffer. Decodes it directly off the raw protocol client instead.
 */
function captureActionBarText(player: any): string[] {
  const texts: string[] = [];
  player.bot._client.on('action_bar', (packet: any) => {
    texts.push(flattenChatComponent(packet.text));
  });
  return texts;
}

test('petstop clears the attack target and survives a small owner hop', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    const since = player.getMessageBufferIndex();
    player.chat('/petstop');
    // CommandStop replies unconditionally, even with no active attack target.
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Stop.Attack'), { since });

    // Single-axis 9-block hop, not diagonal: PlayerListener's forced-respawn path
    // triggers on Euclidean distance > 10 (a diagonal +9,+9 hop would be ~12.7 and
    // force a respawn, destroying this tagged entity).
    server.execute(`tp ${player.username} ${ARENA.x + 9} ${ARENA.y} ${ARENA.z}`);
    await expectConditionHolds(server, player, `if entity @e[tag=${pet.tag}]`, { checks: 3 });
  } finally {
    removePet(server, player);
  }
});

test('petlist lists stored pets by name', async ({ player, server }) => {
  await player.makeOp();
  await createPet(server, player, 'Cow', { name: 'ListedCow' });
  player.chat('/petstore');
  await expectCondition(server, player,
    `at ${player.username} unless entity @e[type=minecraft:cow,distance=..16]`);
  await createPet(server, player, 'Pig', { name: 'ListedPig' });
  try {
    const since = player.getMessageBufferIndex();
    player.chat('/petlist');
    await expect(player).toHaveReceivedMessage('ListedCow', { since });
    await expect(player).toHaveReceivedMessage('ListedPig', { since });
  } finally {
    removePet(server, player);
  }
});

test('petcapturehelper toggles on and off', async ({ player, server }) => {
  await player.makeOp();

  // CommandCaptureHelper sends the same generic Mode message on both directions; the
  // {0} argument (Name.Enabled/Disabled) is what distinguishes them.
  const on = player.getMessageBufferIndex();
  player.chat('/petcapturehelper');
  await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.CaptureHelper.Mode'), { since: on });
  await expect(player).toHaveReceivedMessage(msgPlain('Name.Enabled'), { since: on });

  const off = player.getMessageBufferIndex();
  player.chat('/petcapturehelper');
  await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.CaptureHelper.Mode'), { since: off });
  await expect(player).toHaveReceivedMessage(msgPlain('Name.Disabled'), { since: off });
});

test('petrelease <name> releases by name without the GUI', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { name: 'ByName' });
  try {
    // executeWithName matches on the pet's own name and releases immediately -- no
    // confirm GUI (only bare `/petrelease`/the hub's Release button show one).
    const since = player.getMessageBufferIndex();
    player.chat('/petrelease ByName');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.Command.Release.Success'), { since });

    // removeAfterRelease defaults false, so the mob survives (just stripped of MyPet data).
    await expectCondition(server, player, `if entity @e[type=minecraft:cow,tag=${pet.tag}]`);
    const sinceInfo = player.getMessageBufferIndex();
    player.chat('/petinfo');
    await expect(player).toHaveReceivedMessage(msgFragment('Message.No.HasPet'), { since: sinceInfo });
  } finally {
    server.execute('kill @e[type=minecraft:cow]');
  }
});

test('/mypet help all lists commands the player can currently use', async ({ player, server }) => {
  await player.makeOp();
  // Bare `/mypet help` only prints category names, never command strings like "/petcall";
  // `/mypet help all` renders each HelpEntry via showGroupedHelp. CommandCall's entry is
  // also visibility-gated on hasActivePet, so a pet must exist for it to appear.
  const pet = await createPet(server, player, 'Cow');
  try {
    const since = player.getMessageBufferIndex();
    player.chat('/mypet help all');
    await expect(player).toHaveReceivedMessage('petcall', { since });
  } finally {
    removePet(server, player);
  }
});

test('petsettings healthbar round-trips', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow');
  try {
    // Timer.startPlayerTicking only registers the per-player schedule() task (which drives
    // the action-bar readout below) for players who already had a MyPetPlayer record on
    // join; createPet just registered this player mid-session, so rejoin picks up ticking
    // (same workaround as player-commands.spec.ts's petrespawn test).
    await player.rejoin();
    await expectCondition(server, player,
      `at ${player.username} if entity @e[type=minecraft:cow,distance=..16]`);

    // CommandSettingHealthbar sends the same generic Success reply on both directions
    // (unlike petcapturehelper) -- the real observable is MyPetPlayerImpl#schedule(),
    // which sends an action-bar health readout once/sec only while showHealthBar is true.
    const actionBarTexts = captureActionBarText(player);
    const HEALTH_READOUT = /\d+\.\d{2}\/\d+\.\d{2}/;

    const s1 = player.getMessageBufferIndex();
    player.chat('/petsettings healthbar');
    await expect(player).toHaveReceivedMessage(msgPlain('Message.Command.Success'), { since: s1 });
    // Poll a few ticks (1s period) for the readout to actually start appearing.
    await waitUntil(() => actionBarTexts.some(t => HEALTH_READOUT.test(t)),
      { timeout: 5000, message: 'no health-bar action-bar readout appeared after enabling petsettings healthbar' });

    const s2 = player.getMessageBufferIndex();
    player.chat('/petsettings healthbar');
    await expect(player).toHaveReceivedMessage(msgPlain('Message.Command.Success'), { since: s2 });
    // Let one more scheduled tick flush before checking for absence, so an
    // in-flight readout sent just before the toggle command was processed
    // server-side doesn't false-positive the "readout stopped" check below.
    await sleep(1500);
    const settledLength = actionBarTexts.length;
    let sawReadoutAfterSettle = false;
    try {
      await waitUntil(() => actionBarTexts.slice(settledLength).some(t => HEALTH_READOUT.test(t)), { timeout: 3000 });
      sawReadoutAfterSettle = true;
    } catch {
      // expected: no readout once healthbar flipped back off
    }
    expect(sawReadoutAfterSettle).toBe(false);
  } finally {
    removePet(server, player);
  }
});
