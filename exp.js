/*
	#######################################
	###				###
	### MyWolf EXP-System Level Script 	###
	### 		by Keyle 		###
	###				###
	#######################################
	
		Usable variables:
			lvl 	-> write the return level in this variable
			EXP		-> EXP the wolf has
			factor	-> EXP factor from config
			name	-> wolf's name
			player	-> name of the owner
			maxhp	-> actual maxHP value the wolf has
			lives	-> lives the wolf ha
			
*/

//declare variables
var lvl;
var EXP;
var factor;

//example start
var tmplvl = 1;

for (i = factor * factor; i <= EXP; i = i * factor)
{
	tmplvl++;
}
//example end

lvl = tmplvl; // set return value