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
			factor	-> EXP factor from config
			name	-> wolf's name
			player	-> name of the owner
			maxhp	-> actual maxHP value the wolf has
			
*/

//declare variables
var lvl;
var EXP;
var factor;

//example start
var tmplvl = 1;
var tmpreqEXP = 1;

for (i = factor * factor; i <= EXP; i = i * factor)
{
	tmplvl++;
	tmpreqEXP = i*factor;
}
//example end

lvl = tmplvl; // set return value
reqEXP = tmpreqEXP;