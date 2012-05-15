/*
	#######################################
	###                                 ###
	### MyPet Exp-System Level Script  ###
	###         by Keyle                ###
	###                                 ###
	#######################################
	
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
// Minecraft:E = 7 + roundDown (  n    * 3.5)
while (tmpExp >= 7 + Math.floor(tmpLvl * 3.5))
{
    tmpExp -= 7 + Math.floor(tmpLvl * 3.5);
    tmpLvl++;
}

//   |---------------|
//   |  example end  |
//   |---------------|

// set return values
lvl = tmpLvl+1;
requiredExp = 7 + Math.floor(tmpLvl * 3.5);
currentExp = tepExp;