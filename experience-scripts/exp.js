/*
	#####################################
	###                               ###
	### MyPet Exp-System Level Script ###
	###         by Keyle              ###
	###       MC 1.3.1 script         ###
	#####################################
	
		Usable Methods for the "mypet" Object:
		    mypet.getType()
			mypet.getOwnerName()
			mypet.getSkilltree()
			mypet.getUUID()
			mypet.getWorldGroup()

*/

//   |---------------|
//   |  Calculations |
//   |---------------|

//  Level 2-16 cost 17 XP points each
//  Level 17-31 cost 3 more XP points than the previous
//  Level 32-∞ cost 7 more XP points than the previous

function calculate(exp) {
    var level = 0;
    var requiredExp = 0;
    var currentExp = 0;

    while (exp > 0) {
        if (level < 16) {
            exp -= 17;
        }
        else if (level < 31) {
            exp -= 17 + ((level - 15) * 3);
        }
        else {
            exp -= 17 + ((level - 30) * 7);
        }
        if(exp >= 0) {
            level++;
        }
        else {
            if(level < 16) {
                currentExp = exp + 17;
            }
            else if(level < 31) {
                currentExp = exp + 17 + (level - 15) * 3;
            }
            else {
                currentExp = exp + 62 + (level - 30) * 7;
            }
        }
    }
    level++;
    if(level < 16) {
        requiredExp = 17;
    }
    else if(level < 31) {
        requiredExp = 17 + (level - 14) * 3;
    }
    else {
        requiredExp = 62 + (level - 29) * 7;
    }
    return new Array(level, requiredExp, currentExp);
}

//   |------------------|
//   |  Return Methods  |
//   |------------------|

function getRequiredExp(exp, mypet) {
    return calculate(exp)[1];
}

function getLevel(exp, mypet) {
    return calculate(exp)[0];
}

function getCurrentExp(exp, mypet) {
    return calculate(exp)[2];
}

function getExpByLevel(level, mypet) {
    if(level <= 1) {
        return 0;
    }
    if(level > 31) {
        var exp = 887;
        level -= 31;
        for(var i=1;i<level;i++) {
            exp += 62 + (i*7);
        }
        return exp;
    }
    if(level > 17) {
        var exp = 272;
        level -= 17;
        for(var i=1;i<=level;i++) {
            exp += 17 + (i*3);
        }
        return exp;
    }
    return (level-1) * 17;
}

/**
    Level   Exp     Exp from last
    2       17      17
    3       34      17
    4       51      17
    5       68      17
    6       85      17
    7       102     17
    8       119     17
    9       136     17
    10      153     17
    11      170     17
    12      187     17
    13      204     17
    14      221     17
    15      238     17
    16      255     17
    17      272     17
    18      292     20
    19      315     23
    20      341     26
    21      370     29
    22      402     32
    23      437     35
    24      475     38
    25      516     41
    26      560     44
    27      607     47
    28      657     50
    29      710     53
    30      766     56
    31      825     59
    32      887     62
    33      956     69
    34      1032    76
    35      1115    83
    36      1205    90
    37      1302    97
    38      1406    104
    39      1517    111
    40      1635    118
    41      1760    125
*/