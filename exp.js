/*
	#######################################
	###                                 ###
	### MyWolf EXP-System Level Script  ###
	###         by Keyle                ###
	###                                 ###
	#######################################
	
		required return varibles:
		    lvl         -> write the return level in this variable
			requiredExp -> write the require EXP for the next level in this variable
			currentExp  -> write the current EXP for this level in this variable


		Usable variables:
			EXP		-> EXP the wolf has
			name	-> wolf's name
			player	-> name of the owner
*/

//   |---------------|
//   | example start |
//   |---------------|

var tmpExp = Exp;
var tmplvl = 0;
// Minecraft:   E = 7 + roundDown( n    * 3.5)
while (tmpExp >= 7 + Math.floor((tmplvl) * 3.5))
{
    tmpExp -= 7 + Math.floor((tmplvl) * 3.5);
    tmplvl++;
}

//   |---------------|
//   |  example end  |
//   |---------------|

// set return values
lvl = tmplvl+1;
requiredExp = 7 + Math.floor((tmplvl) * 3.5);
currentExp = tepExp;