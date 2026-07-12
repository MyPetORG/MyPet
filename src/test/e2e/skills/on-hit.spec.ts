import { test } from '@drownek/plugwright';
import { expectCondition, expectScore } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged } from '../lib/world.js';

// None of the fixtures below declare a Damage skill, so getDamage() stays 0 and
// PetMeleeAttackGoal#shouldActivate() refuses to attack — the pet can acquire a target but
// never land a real swing. Every on-hit skill is dispatched generically by
// PetSkillTriggerListener#onPetDealsDamage for any EntityDamageByEntityEvent with the pet as
// damager, so `/damage <target> 1 minecraft:mob_attack by <pet>` substitutes for AI
// engagement, same as Thorns/Ranged in combat-misc.spec.ts.
//
// Victims are pigs (10 HP): zombies sun-ignite even with NoAI:1b, which both makes
// Fire/health/death assertions vacuously true and randomly eats the trigger command via burn
// i-frames. Pigs don't burn and (unlike zombies/spiders) aren't immune to Poison.
const EFFECT_SKILLS = [
  { tree: 'test-fire',   check: (v: string) => ({ score: [`data get entity ${v} Fire`, '1..'] as const }) },
  { tree: 'test-poison', check: (_v: string) => ({ nbt: `{active_effects:[{id:"minecraft:poison"}]}` }) },
  { tree: 'test-slow',   check: (_v: string) => ({ nbt: `{active_effects:[{id:"minecraft:slowness"}]}` }) },
  { tree: 'test-wither', check: (_v: string) => ({ nbt: `{active_effects:[{id:"minecraft:wither"}]}` }) },
];

for (const { tree, check } of EFFECT_SKILLS) {
  test(`${tree}: pet hit applies the effect to the victim`, async ({ player, server }) => {
    await player.makeOp();
    await setupArena(server, player);
    const pet = await createPet(server, player, 'Cow', { skilltree: tree });
    const tag = `v_${tree.replace('test-', '')}`;
    const victim = await spawnVictim(server, player, 'pig', tag);

    try {
      server.execute(`damage ${victim} 1 minecraft:mob_attack by ${pet.selector}`);

      const c = check(victim) as any;
      if (c.nbt) {
        await expectCondition(server, player, `if entity @e[tag=${tag},nbt=${c.nbt}]`, { timeout: 30000 });
      } else {
        await expectScore(server, player, c.score[0], c.score[1], { timeout: 30000 });
      }
    } finally {
      killTagged(server, tag);
      removePet(server, player);
    }
  });
}

test('test-bleed: victim keeps losing health after the hit', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-bleed' });
  const victim = await spawnVictim(server, player, 'pig', 'v_bleed');

  try {
    server.execute(`damage ${victim} 1 minecraft:mob_attack by ${pet.selector}`);
    // Pig max health 10; the trigger command alone leaves 9. BleedImpl ticks 1 dmg/s for 8s,
    // so ..7 requires at least two bleed points after the triggering hit.
    await expectScore(server, player, `data get entity ${victim} Health`, '..7', { timeout: 30000 });
  } finally {
    killTagged(server, 'v_bleed');
    removePet(server, player);
  }
});

// LightningImpl.apply() calls World#strikeLightningEffect(...), which per Bukkit's contract
// does no damage/combustion — purely cosmetic. The real, deterministic effect is the
// following target.damage(damage, pet) for the skilltree's own +10 Damage. Victim is a sheep
// (8 HP): the 1-damage trigger alone can't kill it, but 1 + the +10 strike does.
test("test-lightning: strike deals the skill's bonus damage to the victim", async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-lightning' });
  const victim = await spawnVictim(server, player, 'sheep', 'v_light');

  try {
    server.execute(`damage ${victim} 1 minecraft:mob_attack by ${pet.selector}`);
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 15000 });
  } finally {
    killTagged(server, 'v_light');
    removePet(server, player);
  }
});
