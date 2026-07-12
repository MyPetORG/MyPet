import { expectCondition } from './oracle.js';

export interface PetHandle {
  name: string;       // custom name, also used to derive the tag
  tag: string;        // scoreboard tag on the entity
  type: string;       // MyPet type name, e.g. "SnowGolem"
  entityType: string; // minecraft id path, e.g. "snow_golem"
  selector: string;   // @e[tag=...,limit=1]
}

let petCounter = 0;

/**
 * `petadmin create` needs a namespaced pet-type key (`minecraft:cow`, not `Cow`). Callers
 * pass the MyPet-style name; an already-namespaced `type` (e.g. `mypet:<id>`) passes
 * through verbatim, though the spawn-wait selector below still assumes a vanilla entity.
 */
export async function createPet(
  server: any, player: any, type: string,
  opts: { name?: string; skilltree?: string; baby?: boolean; flags?: string[] } = {},
): Promise<PetHandle> {
  const name = opts.name ?? `TP${++petCounter}`;
  const namespacedType = type.includes(':')
    ? type
    : `minecraft:${type.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()}`;
  const entityType = namespacedType.split(':')[1];

  // petadmin create -f <player> <type> [options...] — space-separated, order-independent;
  // `baby` is a bare flag (not `baby:true`), same shape as opts.flags entries.
  let cmd = `petadmin create -f ${player.username} ${namespacedType}`;
  if (opts.skilltree) cmd += ` skilltree:${opts.skilltree}`;
  if (opts.baby) cmd += ' baby';
  for (const flag of opts.flags ?? []) cmd += ` ${flag}`;
  cmd += ` name:${name}`;
  server.execute(cmd);

  // Name-scoped wait, not just type-scoped: `petadmin create` activates the pet inside an
  // async repository callback, so a type-only selector could match a stale same-type mob
  // and return before the new pet is actually active, racing a follow-up command.
  await expectCondition(server, player,
    `at ${player.username} if entity @e[type=minecraft:${entityType},name=${name},distance=..16]`);

  const tag = `pet_${name}`;
  server.execute(
    `execute at ${player.username} run ` +
    `tag @e[type=minecraft:${entityType},name=${name},distance=..16,limit=1,sort=nearest] add ${tag}`);

  // Await the tag before returning: it's fire-and-forget on the stdin channel, while
  // callers' next step often goes through the bot's chat channel with no ordering
  // guarantee between the two, so an early return risks the tag selector missing forever.
  await expectCondition(server, player, `if entity @e[tag=${tag}]`);

  return { name, tag, type, entityType, selector: `@e[tag=${tag},limit=1]` };
}

export function removePet(server: any, player: any): void {
  server.execute(`petadmin remove ${player.username}`);
}

export function setExp(server: any, player: any, amount: number, mode: 'set' | 'add' | 'remove' = 'set'): void {
  server.execute(`petadmin exp ${player.username} ${amount} ${mode}`);
}

export function setSkilltree(server: any, player: any, tree: string): void {
  server.execute(`petadmin skilltree ${player.username} ${tree}`);
}
