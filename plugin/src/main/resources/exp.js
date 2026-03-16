// MyPet XP Level costs:
//   Levels 2-17:  17 XP each (flat)
//   Levels 18-31: 20 XP + 3 per level above 17
//   Levels 32+:   62 XP + 7 per level above 31

// Pre-calculated tier boundaries:
var EXP_AT_17 = 272;   // (17-1) * 17
var EXP_AT_32 = 887;   // EXP_AT_17 + sum of costs for levels 18-31, plus level 32's base

function getExpByLevel(level, petType, worldGroup) {
    if (level <= 1) {
        return 0;
    }
    if (level <= 17) {
        // Flat 17 XP per level
        return (level - 1) * 17;
    }
    if (level <= 31) {
        // Arithmetic series: costs are 20, 23, 26, ... (increment of 3)
        var n = level - 17;
        return EXP_AT_17 + n * (3 * level - 14) / 2;
    }
    // Level 32+: costs are 69, 76, 83, ... (increment of 7)
    var n = level - 32;
    return EXP_AT_32 + n * (7 * level - 93) / 2;
}