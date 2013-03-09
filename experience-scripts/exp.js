/*
	#####################################
	###                               ###
	### MyPet Exp-System Level Script ###
	###         by Keyle              ###
	###       MC 1.3.1 script         ###
	#####################################
	
		Usable Methods:
		    MyPet.getType()
			MyPet.getOwnerName()

*/

//   |---------------|
//   |  Calculations |
//   |---------------|

//  Level 2-16 cost 17 XP points each
//  Level 17-31 cost 3 more XP points than the previous
//  Level 32-∞ cost 7 more XP points than the previous

var lastExp;
var currentExp;
var level;
var requiredExp;

function calculate(exp) {
    level = 0;
    requiredExp = 0;
    currentExp = 0;

    if(exp !== lastExp) {
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
}

//   |------------------|
//   |  Return Methods  |
//   |------------------|

function getRequiredExp(exp) {
    if(exp !== lastExp) {
        calculate(exp);
        lastExp = exp;
    }
    return requiredExp;
}

function getLevel(exp) {
    if(exp !== lastExp) {
        calculate(exp);
        lastExp = exp;
    }
    return level;
}

function getCurrentExp(exp) {
    if(exp !== lastExp) {
        calculate(exp);
        lastExp = exp;
    }
    return currentExp;
}