package de.Keyle.MyPet.migration;

public enum MigrationDomain {
    DATABASE(1),
    CONFIG(2),
    SKILLTREE(3),
    PET_DATA(4),
    PLAYER_DATA(5);

    private final int priority;

    MigrationDomain(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
