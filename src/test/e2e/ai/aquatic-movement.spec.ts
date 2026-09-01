import { test } from '@drownek/plugwright';
import { expectCondition, expectConditionHolds } from '../lib/oracle.js';
import { createPet, removePet } from '../lib/pets.js';
import { setupArena, spawnVictim, killTagged, ARENA } from '../lib/world.js';

// Depth coverage for the strict water-breathers (PetAquaticEntity).
//
// PetFloatGoal added +0.05 Y every tick to every non-flying pet in water,
// including the ones that breathe it, so a submerged pet was shoved back to
// the waterline no matter what depth its navigation asked for.
// PetType#specialFloat(), the escape hatch built for exactly this, is
// hardcoded false with no overrides anywhere, so nothing ever took it.
//
// Run per species because each mob's vanilla move control composes differently
// with MyPet's goals -- Guardian's puffer-drive, the Dolphin's surface bias and
// AbstractFish's FishMoveControl are all distinct.

/**
 * The complete PetAquaticEntity roster -- every strict water-breather, not just
 * the seven named in the report. The other four share the marker and therefore
 * took exactly the same fix, so leaving them untested would leave the blast
 * radius of that fix unmeasured. Nautilus is included deliberately: its own
 * open tracker entry ("doesn't follow the owner on land; floats to the water
 * surface when underwater") describes both defects fixed here.
 *
 * The species with their own vanilla propulsion (Squid, GlowSquid, Nautilus)
 * are the interesting ones -- Dolphin already proved that a mob driving itself
 * can ignore MyPet's navigation entirely.
 */
const SPECIES = [
  // reported
  'Cod',
  'Salmon',
  'Pufferfish',
  'TropicalFish',
  'Guardian',
  'ElderGuardian',
  'Dolphin',
  // same marker, same fix, not in the report
  'Squid',
  'GlowSquid',
  'Tadpole',
  'Nautilus',
] as const;

// Pool half-width; spans ARENA +/- 4 horizontally and ARENA.y..+4 vertically,
// so the surface sits at ARENA.y+5 over the sea-lantern floor at ARENA.y-1.
const POOL = 4;

/**
 * How far east of the owner the husk spawns. Inside PetAggressiveTargetGoal's
 * ~9.5-block acquisition radius, but far enough out that reaching it is real
 * travel rather than melee reach from the spawn point.
 */
const VICTIM_DX = 8;

/**
 * Chase-pool bounds for the in-water test. Longer in +X than the depth pool so
 * the pet has VICTIM_DX blocks of open water to actually swim through, and 4
 * deep so both pet and victim sit well below the surface.
 */
const CHASE_POOL = { back: 4, forward: VICTIM_DX + 6, side: 5, height: 4 };

/**
 * Species that run the depth test. Nautilus is excluded: it rises under its own
 * propulsion fast enough that the ~5s window catches it inconsistently -- one
 * run held the box for all 6 checks (in 9.9s, against 6.2s for every other
 * species), the next never entered it at all. That is Cluster Q drift, not the
 * PetFloatGoal nudge this test exists to guard, and the box must NOT be widened
 * to accommodate it: a looser box would stop detecting the nudge, which is the
 * one thing this assertion is for. The other ten species prove the nudge is
 * gone; Nautilus's own drift is tracked with the rest of Cluster Q.
 */
const DEPTH_SPECIES = SPECIES.filter((s) => s !== 'Nautilus');

/**
 * Species EXCLUDED from the in-water chase below because they are still broken
 * there -- not because the case does not apply to them. See the "self-propelled
 * water pets" cluster in docs/pet-type-issue-tracker.md.
 *
 * The split is clean and mechanical: every water-breather that moves via
 * standard pathfinding navigation passes the chase (Cod, Salmon, Pufferfish,
 * TropicalFish and Tadpole are all AbstractFish-shaped; Guardian and
 * ElderGuardian likewise). Every water-breather with its OWN vanilla propulsion
 * fails it, all four at an identical ~41.8s timeout.
 *
 * Mechanism measured directly for Dolphin (melee goal instrumented, 2026-08-28):
 * target held and navigation accepting the path (navOk=true) for the full 40s,
 * while the pet advanced ~2.3 blocks horizontally and ROSE 3 blocks. The other
 * three share the failure signature and the self-propulsion trait; their
 * mechanism is inferred from Dolphin, not separately instrumented.
 *
 * None of these declare a walkSpeed override -- they run the same 0.30 default
 * as Cod and Guardian, which pass -- so this is not a speed-attribute problem
 * and not something this file can fix. Remove a species from this set the
 * moment its follow controller lands; the test is already correct.
 */
const SELF_PROPELLED = ['Dolphin', 'Squid', 'GlowSquid', 'Nautilus'];
const CHASE_SPECIES = SPECIES.filter((s) => !SELF_PROPELLED.includes(s));

for (const species of SPECIES) {
  // Owner stands on the pool floor beside the pet, inside the follow goal's
  // stop distance, so the follow goal contributes no navigation of its own and
  // the buoyancy nudge is the only thing under test. Water breathing keeps the
  // bot alive for the duration.
  if (DEPTH_SPECIES.includes(species as never))
  test(`aquatic-movement: a submerged ${species} holds its depth`, async ({ player, server }) => {
    await player.makeOp();
    await setupArena(server, player);

    server.execute(`effect give ${player.username} minecraft:water_breathing 120 1 true`);
    server.execute(
      `fill ${ARENA.x - POOL} ${ARENA.y} ${ARENA.z - POOL} ` +
      `${ARENA.x + POOL} ${ARENA.y + 4} ${ARENA.z + POOL} minecraft:water`);
    server.execute(`tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);

    const pet = await createPet(server, player, species);

    try {
      // Box spans ARENA.y-1 .. ARENA.y+2 -- the bottom of a column whose
      // surface is at ARENA.y+5. A force-floated pet sits at the top and fails
      // the first check, usually within a second of spawning.
      //
      // Caveat worth knowing: this window is ~5s, which catches the old +0.05
      // per-tick nudge instantly but NOT a slow drift. A submerged Dolphin
      // rises ~0.083 blocks/sec from its own vanilla move control, i.e. ~0.4
      // blocks here -- inside the box. Dolphin therefore passes this test and
      // still fails the chase test below. This assertion proves the MyPet
      // buoyancy nudge is gone; it does not prove a pet holds depth forever.
      await expectConditionHolds(server, player,
        `positioned ${ARENA.x - POOL - 2} ${ARENA.y - 1} ${ARENA.z - POOL - 2} ` +
        `if entity @e[tag=${pet.tag},dx=${POOL * 2 + 4},dy=3,dz=${POOL * 2 + 4}]`,
        { checks: 6 });
    } finally {
      removePet(server, player);
      server.execute(`kill @e[tag=${pet.tag}]`);
      server.execute(
        `fill ${ARENA.x - POOL} ${ARENA.y} ${ARENA.z - POOL} ` +
        `${ARENA.x + POOL} ${ARENA.y + 4} ${ARENA.z + POOL} minecraft:air`);
      server.execute(`effect clear ${player.username} minecraft:water_breathing`);
    }
  });

  if (!CHASE_SPECIES.includes(species as never)) continue;

  // The in-water half of defect 1, and the case PetGoalInstaller's change was
  // most aimed at. Underwater, PetAquaticMovementGoal deliberately does nothing
  // (it returns early on isInWaterOrBubble) -- so the ONLY thing that can close
  // the distance here is PetMeleeAttackGoal steering the pet's own
  // WaterBoundPathNavigation, which is exactly the goal the old `waterBound`
  // gate withheld from these species. A dry-land pass proves nothing about it.
  //
  // The victim is a Drowned, not a husk: a husk left underwater converts (husk
  // -> zombie -> drowned) on a ~30s timer, which would swap the entity out from
  // under the tag mid-test. A Drowned is already terminal.
  //
  // This also exercises the depth fix under load -- a pet still pinned to the
  // surface by the old buoyancy nudge could not reach a target on the floor.
  if (!CHASE_SPECIES.includes(species as never)) continue;

  test(`aquatic-movement: a submerged ${species} swims to its target and kills it`, async ({ player, server }) => {
    await player.makeOp();
    await setupArena(server, player);

    const victimTag = `w_${species.toLowerCase()}`;
    server.execute(`effect give ${player.username} minecraft:water_breathing 300 1 true`);
    server.execute(
      `fill ${ARENA.x - CHASE_POOL.back} ${ARENA.y} ${ARENA.z - CHASE_POOL.side} ` +
      `${ARENA.x + CHASE_POOL.forward} ${ARENA.y + CHASE_POOL.height} ${ARENA.z + CHASE_POOL.side} ` +
      `minecraft:water`);
    server.execute(`tp ${player.username} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);

    const pet = await createPet(server, player, species, { skilltree: 'test-behavior-modes' });

    try {
      player.chat('/petbehavior aggressive');
      const victim = await spawnVictim(server, player, 'drowned', victimTag, { dx: VICTIM_DX });

      await expectCondition(server, player, `unless entity ${victim}`, { timeout: 40000 });
    } finally {
      killTagged(server, victimTag);
      removePet(server, player);
      server.execute(
        `fill ${ARENA.x - CHASE_POOL.back} ${ARENA.y} ${ARENA.z - CHASE_POOL.side} ` +
        `${ARENA.x + CHASE_POOL.forward} ${ARENA.y + CHASE_POOL.height} ${ARENA.z + CHASE_POOL.side} ` +
        `minecraft:air`);
      server.execute(`effect clear ${player.username} minecraft:water_breathing`);
    }
  });
}
