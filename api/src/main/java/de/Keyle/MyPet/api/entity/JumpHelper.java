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

package de.Keyle.MyPet.api.entity;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

public class JumpHelper {
    public static final Map<String, Double> JUMP_MAP = new HashMap<>();
    public static final DecimalFormat JUMP_FORMAT = new DecimalFormat("#.0");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        JUMP_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        JUMP_FORMAT.setGroupingUsed(false);
        JUMP_FORMAT.setDecimalFormatSymbols(symbols);

        JUMP_MAP.put("0.0", 0.13170400000011104);
        JUMP_MAP.put("0.1", 0.16291700000014225);
        JUMP_MAP.put("0.2", 0.19214500000017148);
        JUMP_MAP.put("0.3", 0.21973000000019907);
        JUMP_MAP.put("0.4", 0.24592700000022527);
        JUMP_MAP.put("0.5", 0.27093199999966927);
        JUMP_MAP.put("0.6", 0.29489799999902805);
        JUMP_MAP.put("0.7", 0.31794699999841136);
        JUMP_MAP.put("0.8", 0.3401799999978165);
        JUMP_MAP.put("0.9", 0.36167999999724126);
        JUMP_MAP.put("1.0", 0.38251799999668373);
        JUMP_MAP.put("1.1", 0.40275299999614234);
        JUMP_MAP.put("1.2", 0.4224359999956157);
        JUMP_MAP.put("1.3", 0.44161199999510264);
        JUMP_MAP.put("1.4", 0.46031899999460213);
        JUMP_MAP.put("1.5", 0.47859099999411325);
        JUMP_MAP.put("1.6", 0.4964589999936352);
        JUMP_MAP.put("1.7", 0.5139489999939415);
        JUMP_MAP.put("1.8", 0.5310859999944343);
        JUMP_MAP.put("1.9", 0.5478899999949175);
        JUMP_MAP.put("2.0", 0.5643809999953917);
        JUMP_MAP.put("2.1", 0.5805759999958574);
        JUMP_MAP.put("2.2", 0.5964929999963151);
        JUMP_MAP.put("2.3", 0.6121449999967652);
        JUMP_MAP.put("2.4", 0.6275459999972081);
        JUMP_MAP.put("2.5", 0.642707999997644);
        JUMP_MAP.put("2.6", 0.6576419999980735);
        JUMP_MAP.put("2.7", 0.6723589999984967);
        JUMP_MAP.put("2.8", 0.6868689999989139);
        JUMP_MAP.put("2.9", 0.7011799999993255);
        JUMP_MAP.put("3.0", 0.7153019999997315);
        JUMP_MAP.put("3.1", 0.7292410000001324);
        JUMP_MAP.put("3.2", 0.7430050000005282);
        JUMP_MAP.put("3.3", 0.7566010000009191);
        JUMP_MAP.put("3.4", 0.7700350000013054);
        JUMP_MAP.put("3.5", 0.7833130000016872);
        JUMP_MAP.put("3.6", 0.7964410000020647);
        JUMP_MAP.put("3.7", 0.8094240000024381);
        JUMP_MAP.put("3.8", 0.8222680000028074);
        JUMP_MAP.put("3.9", 0.8349760000031728);
        JUMP_MAP.put("4.0", 0.8475530000035345);
        JUMP_MAP.put("4.1", 0.8600040000038925);
        JUMP_MAP.put("4.2", 0.8723330000042471);
        JUMP_MAP.put("4.3", 0.8845430000045982);
        JUMP_MAP.put("4.4", 0.896637000004946);
        JUMP_MAP.put("4.5", 0.9086200000052905);
        JUMP_MAP.put("4.6", 0.920495000005632);
        JUMP_MAP.put("4.7", 0.9322640000059704);
        JUMP_MAP.put("4.8", 0.9439300000063059);
        JUMP_MAP.put("4.9", 0.9554980000066385);
        JUMP_MAP.put("5.0", 0.9669680000069684);
        JUMP_MAP.put("5.1", 0.9783440000072955);
        JUMP_MAP.put("5.2", 0.98962800000762);
        JUMP_MAP.put("5.3", 1.0008230000078504);
        JUMP_MAP.put("5.4", 1.0119310000069366);
        JUMP_MAP.put("5.5", 1.0229530000060298);
        JUMP_MAP.put("5.6", 1.0338930000051298);
        JUMP_MAP.put("5.7", 1.0447520000042365);
        JUMP_MAP.put("5.8", 1.0555310000033498);
        JUMP_MAP.put("5.9", 1.0662340000024693);
        JUMP_MAP.put("6.0", 1.076861000001595);
        JUMP_MAP.put("6.1", 1.0874140000007269);
        JUMP_MAP.put("6.2", 1.0978959999998645);
        JUMP_MAP.put("6.3", 1.108306999999008);
        JUMP_MAP.put("6.4", 1.1186489999981573);
        JUMP_MAP.put("6.5", 1.128922999997312);
        JUMP_MAP.put("6.6", 1.1391319999964722);
        JUMP_MAP.put("6.7", 1.1492749999956378);
        JUMP_MAP.put("6.8", 1.1593559999948084);
        JUMP_MAP.put("6.9", 1.1693739999939843);
        JUMP_MAP.put("7.0", 1.179331999993165);
        JUMP_MAP.put("7.1", 1.1892289999923509);
        JUMP_MAP.put("7.2", 1.1990689999915414);
        JUMP_MAP.put("7.3", 1.2088509999907366);
        JUMP_MAP.put("7.4", 1.2185769999899365);
        JUMP_MAP.put("7.5", 1.228247999989141);
        JUMP_MAP.put("7.6", 1.2378639999883498);
        JUMP_MAP.put("7.7", 1.2474269999875631);
        JUMP_MAP.put("7.8", 1.2569389999867806);
        JUMP_MAP.put("7.9", 1.2663979999860024);
        JUMP_MAP.put("8.0", 1.2758079999852283);
        JUMP_MAP.put("8.1", 1.2851679999844583);
        JUMP_MAP.put("8.2", 1.2944789999836923);
        JUMP_MAP.put("8.3", 1.3037429999829302);
        JUMP_MAP.put("8.4", 1.312958999982172);
        JUMP_MAP.put("8.5", 1.3221289999814176);
        JUMP_MAP.put("8.6", 1.331253999980667);
        JUMP_MAP.put("8.7", 1.34033399997992);
        JUMP_MAP.put("8.8", 1.3493709999791765);
        JUMP_MAP.put("8.9", 1.3583639999784367);
        JUMP_MAP.put("9.0", 1.3673139999777004);
        JUMP_MAP.put("9.1", 1.3762219999769676);
        JUMP_MAP.put("9.2", 1.3850889999762381);
        JUMP_MAP.put("9.3", 1.393915999975512);
        JUMP_MAP.put("9.4", 1.4027019999747892);
        JUMP_MAP.put("9.5", 1.4114489999740696);
        JUMP_MAP.put("9.6", 1.4201579999733531);
        JUMP_MAP.put("9.7", 1.4288279999726399);
        JUMP_MAP.put("9.8", 1.4374599999719297);
        JUMP_MAP.put("9.9", 1.4460549999712227);
        JUMP_MAP.put("10.0", 1.4546129999705186);
    }

    public static void calcJumpMap() {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.FLOOR);

        int blocks = 0;
        double x = 0;
        while (blocks <= 100) {
            double y = calcJumpHeight(x);
            String format = df.format(y - (blocks / 10.));
            if (format.equals("0")) {
                JUMP_MAP.put("" + (blocks / 10.), x);
                blocks += 1;
            }
            x += 0.000001;
        }
    }

    public static double calcJumpHeight(double x) {
        return -0.1817584952 * (x * x * x) + 3.689713992 * (x * x) + 2.128599134 * x - 0.343930367;
    }
}
