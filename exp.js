/*
	#######################################
	###                                 ###
	### MyWolf EXP-System Level Script  ###
	###         by Keyle                ###
	###                                 ###
	#######################################
	
		required return varibles:
		    lvl 	-> write the return level in this variable
			reqEXP  -> write the require EXP for the nex level in this variable


		Usable variables:
			EXP		-> EXP the wolf has
			name	-> wolf's name
			player	-> name of the owner
			maxhp	-> actual maxHP value the wolf has
			
*/

//declare variables
var lvl;
var EXP;

//example start


// Minecraft:   E = 7 + roundDown( n    * 3.5)
var tmpEXP = EXP;
var tmplvl = 0;

while (tmpEXP >= 7 + Math.floor((tmplvl) * 3.5))
{
    tmpEXP -= 7 + Math.floor((tmplvl) * 3.5);
    tmplvl++;
}

//example end

// set return values
lvl = tmplvl+1;
reqEXP = 7 + Math.floor((tmplvl) * 3.5);