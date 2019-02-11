/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

//  #####################################
//  ###                               ###
//  ### MyPet Exp-System Level Script ###
//  ###         by Keyle              ###
//  ###       MC 1.3.1 script         ###
//  #####################################
//
//      Usable Fields for the "info" Object:
//          info.type
//          info.worldGroup
//

//  Level 2-16 cost 17 XP points each
//  Level 17-31 cost 3 more XP points than the previous
//  Level 32-∞ cost 7 more XP points than the previous

//   |------------------|
//   |  Return Methods  |
//   |------------------|

function getExpByLevel(level, petType, worldGroup) {
    if (level <= 1) {
        return 0;
    }
    var exp = 0, i;
    if (level > 31) {
        exp = 887;
        level -= 31;
        for (i = 1; i < level; i++) {
            exp += 62 + (i * 7);
        }
        return exp;
    }
    if (level > 17) {
        exp = 272;
        level -= 17;
        for (i = 1; i <= level; i++) {
            exp += 17 + (i * 3);
        }
        return exp;
    }
    return (level - 1) * 17;
}
