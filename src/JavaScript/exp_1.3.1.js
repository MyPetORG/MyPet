/*
	#####################################
	###                               ###
	### MyPet Exp-System Level Script ###
	###         by Keyle              ###
	###       MC 1.3.1 script         ###
	###  rename this file to exp.js   ###
	###        befor using it         ###
	###                               ###
	#####################################
	
		required return varibles:
		    lvl         -> write the return level in this variable
			requiredExp -> write the require Exp for the next level in this variable
			currentExp  -> write the current Exp for this level in this variable


		Usable variables:
		    type    -> Mobtype of the pet
			exp		-> Exp the pet has
			name	-> The name of the pet
			player	-> The name of the owner
*/

//   |---------------|
//   | example start |
//   |---------------|

    var tmpExp = exp;
    var tmpLvl = 0;
//  Level 1-15 cost 17 XP points each
//  Level 16-30 cost 3 more XP points than the previous (cost = 17 + (level - 15) * 3)
//  Level 31-∞ cost 7 more XP points than the previous (cost = 62 + (level - 30) * 7)
    while (tmpExp > 0)
    {
        if(tmpLvl < 16)
        {
            tmpExp -= 17;
        }
        else if(tmpLvl < 31)
        {
            tmpExp -= 17 + (tmpLvl - 15) * 3;
        }
        else
        {
            tmpExp -= 62 + (tmpLvl - 30) * 7;
        }
        if(tmpExp < 0)
        {
            if(tmpLvl < 16)
            {
                tmpExp += 17;
            }
            else if(tmpLvl < 31)
            {
                tmpExp += 17 + (tmpLvl - 15) * 3;
            }
            else
            {
                tmpExp += 62 + (tmpLvl - 30) * 7;
            }
            break;
        }
        tmpLvl++;
    }

//  set return values
    lvl = tmpLvl;
    if(tmpLvl < 16)
    {
        requiredExp = 17;
    }
    else if(tmpLvl < 31)
    {
        requiredExp = 17 + (tmpLvl - 14) * 3;
    }
    else
    {
        requiredExp = 62 + (tmpLvl - 29) * 7;
    }

    currentExp = tmpExp;

//   |---------------|
//   |  example end  |
//   |---------------|