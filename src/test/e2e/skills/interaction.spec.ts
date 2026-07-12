import { test } from '@drownek/plugwright';
import { Vec3 } from 'vec3';
import { expectCondition } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged, ARENA } from '../lib/world.js';
import { msgPlain } from '../lib/locale.js';

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));

// Right-click mounting (RideInteractListener) requires holding the configured RIDE_ITEM (a
// lead) first. /petride (CommandPetRide) skips that item check entirely -- it still requires
// the Ride skill active (test-ride.st.json grants it), but no saddle (RequireSaddle=false).
test('test-ride: /petride mounts the bot on its pet', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Horse', { skilltree: 'test-ride' });

  try {
    player.chat('/petride');
    await sleep(1000);
    if (!(player.bot as any).vehicle) throw new Error('bot did not mount the pet');
  } finally {
    removePet(server, player);
  }
});

// PetControlGoal has a hard per-second travel budget (distance/3, floored at 3) that
// force-stops navigation if the pet doesn't arrive in time. Look straight down at a known
// platform block from directly above (deterministic raycast landing) and keep the distance
// short (6 blocks) so ordinary walking speed comfortably beats the budget.
test('test-control: lead right-click sends the pet to the target', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  const pet = await createPet(server, player, 'Cow', { skilltree: 'test-control' });

  try {
    server.execute(`give ${player.username} minecraft:lead 1`);
    await sleep(500);
    const lead: any = player.bot.inventory.items().find((i: any) => i.name === 'lead');
    if (!lead) throw new Error('bot has no lead');
    await player.bot.equip(lead, 'hand');

    const targetX = ARENA.x + 6, targetZ = ARENA.z;
    server.execute(`tp ${player.username} ${targetX} ${ARENA.y} ${targetZ}`);
    await sleep(300);
    // Aim at the center of the platform block directly beneath the owner's
    // new position (platform surface is filled one block below ARENA.y).
    await player.bot.lookAt(new Vec3(targetX + 0.5, ARENA.y - 1, targetZ + 0.5), true);
    player.bot.activateItem();

    await expectCondition(server, player,
      `positioned ${targetX} ${ARENA.y} ${targetZ} if entity @e[tag=${pet.tag},distance=..6]`,
      { timeout: 20000 });
  } finally {
    removePet(server, player);
  }
});

// PickupImpl.pickup starts disabled; must be toggled on via /petpickup first.
test('test-pickup: dropped item lands in the backpack', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { name: 'Vacuum', skilltree: 'test-pickup' });

  try {
    player.chat('/petpickup');

    server.execute(
      `summon minecraft:item ${ARENA.x + 2} ${ARENA.y + 1} ${ARENA.z} {Item:{id:"minecraft:diamond",count:1}}`);
    // picked up from the ground…
    await expectCondition(server, player,
      `positioned ${ARENA.x} ${ARENA.y} ${ARENA.z} unless entity @e[type=minecraft:item,distance=..12]`,
      { timeout: 20000 });
    // …and visible in the backpack GUI (getDisplayName() falls back to the material id).
    player.chat('/petinventory');
    const gui = await player.gui({ title: /Vacuum's Backpack/ });
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').toLowerCase().includes('diamond')).click();
  } finally {
    removePet(server, player);
  }
});

// BeaconImpl.schedule() gates on `active && !selectedBuffs.isEmpty()` -- selecting a buff
// alone never arms it, the toggle slot click is required too. The buff button's title is a
// raw translatable key (effect.minecraft.strength); getDisplayName() returns it untranslated,
// which still contains "strength" as a substring.
test('test-beacon: selected buff reaches the owner', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-beacon' });

  try {
    player.chat('/petbeacon');
    const gui = await player.gui({ title: msgPlain('Gui.Beacon.Title'), timeout: 8000 });

    await gui.locator((i: any) => String(i.getDisplayName() ?? '').toLowerCase().includes('strength')).click();
    await gui.locator((i: any) => String(i.getDisplayName() ?? '').includes(msgPlain('Gui.Beacon.Toggle.Off'))).click();

    await expectCondition(server, player,
      `if entity @a[name=${player.username},nbt={active_effects:[{id:"minecraft:strength"}]}]`,
      { timeout: 20000 });
  } finally {
    removePet(server, player);
  }
});

// Husk, not zombie: same 20 HP/hitbox but no sun-burn, avoiding a vacuous death assertion.
// PetAggressiveTargetGoal doesn't require the candidate be hostile -- any nearby non-owned
// LivingEntity qualifies -- and test-behavior.st.json's Damage+50 one-hits it once targeted.
test('test-behavior: aggressive pet attacks nearby monsters on its own', async ({ player, server }) => {
  await player.makeOp();
  await setupArena(server, player);
  await createPet(server, player, 'Cow', { skilltree: 'test-behavior' }); // Aggro+Duel true, Damage+50

  try {
    player.chat('/petbehavior aggressive');
    const victim = await spawnVictim(server, player, 'husk', 'v_beh', { dx: 5 });
    await expectCondition(server, player, `unless entity ${victim}`, { timeout: 25000 });
  } finally {
    killTagged(server, 'v_beh');
    removePet(server, player);
  }
});
