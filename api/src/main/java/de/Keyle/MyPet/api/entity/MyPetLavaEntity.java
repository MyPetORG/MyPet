package de.Keyle.MyPet.api.entity;

/**
 * Marker for pet types whose underlying vanilla mob is immune to lava
 * and navigates through it naturally (Strider, all Nether skeletons, and
 * Magma Cube). The movement layer uses this to allow lava pathfinding,
 * and the survival listener uses it to cancel lava damage events.
 */
public interface MyPetLavaEntity {
}
