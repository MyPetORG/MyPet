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
import java.util.Map;

import static java.util.Map.entry;

public class JumpHelper {
    public static final Map<String, Double> JUMP_MAP = Map.<String, Double>ofEntries(
            entry("0.0", 0.13170400000011104),
            entry("0.1", 0.16291700000014225),
            entry("0.2", 0.19214500000017148),
            entry("0.3", 0.21973000000019907),
            entry("0.4", 0.24592700000022527),
            entry("0.5", 0.27093199999966927),
            entry("0.6", 0.29489799999902805),
            entry("0.7", 0.31794699999841136),
            entry("0.8", 0.3401799999978165),
            entry("0.9", 0.36167999999724126),
            entry("1.0", 0.38251799999668373),
            entry("1.1", 0.40275299999614234),
            entry("1.2", 0.4224359999956157),
            entry("1.3", 0.44161199999510264),
            entry("1.4", 0.46031899999460213),
            entry("1.5", 0.47859099999411325),
            entry("1.6", 0.4964589999936352),
            entry("1.7", 0.5139489999939415),
            entry("1.8", 0.5310859999944343),
            entry("1.9", 0.5478899999949175),
            entry("2.0", 0.5643809999953917),
            entry("2.1", 0.5805759999958574),
            entry("2.2", 0.5964929999963151),
            entry("2.3", 0.6121449999967652),
            entry("2.4", 0.6275459999972081),
            entry("2.5", 0.642707999997644),
            entry("2.6", 0.6576419999980735),
            entry("2.7", 0.6723589999984967),
            entry("2.8", 0.6868689999989139),
            entry("2.9", 0.7011799999993255),
            entry("3.0", 0.7153019999997315),
            entry("3.1", 0.7292410000001324),
            entry("3.2", 0.7430050000005282),
            entry("3.3", 0.7566010000009191),
            entry("3.4", 0.7700350000013054),
            entry("3.5", 0.7833130000016872),
            entry("3.6", 0.7964410000020647),
            entry("3.7", 0.8094240000024381),
            entry("3.8", 0.8222680000028074),
            entry("3.9", 0.8349760000031728),
            entry("4.0", 0.8475530000035345),
            entry("4.1", 0.8600040000038925),
            entry("4.2", 0.8723330000042471),
            entry("4.3", 0.8845430000045982),
            entry("4.4", 0.896637000004946),
            entry("4.5", 0.9086200000052905),
            entry("4.6", 0.920495000005632),
            entry("4.7", 0.9322640000059704),
            entry("4.8", 0.9439300000063059),
            entry("4.9", 0.9554980000066385),
            entry("5.0", 0.9669680000069684),
            entry("5.1", 0.9783440000072955),
            entry("5.2", 0.98962800000762),
            entry("5.3", 1.0008230000078504),
            entry("5.4", 1.0119310000069366),
            entry("5.5", 1.0229530000060298),
            entry("5.6", 1.0338930000051298),
            entry("5.7", 1.0447520000042365),
            entry("5.8", 1.0555310000033498),
            entry("5.9", 1.0662340000024693),
            entry("6.0", 1.076861000001595),
            entry("6.1", 1.0874140000007269),
            entry("6.2", 1.0978959999998645),
            entry("6.3", 1.108306999999008),
            entry("6.4", 1.1186489999981573),
            entry("6.5", 1.128922999997312),
            entry("6.6", 1.1391319999964722),
            entry("6.7", 1.1492749999956378),
            entry("6.8", 1.1593559999948084),
            entry("6.9", 1.1693739999939843),
            entry("7.0", 1.179331999993165),
            entry("7.1", 1.1892289999923509),
            entry("7.2", 1.1990689999915414),
            entry("7.3", 1.2088509999907366),
            entry("7.4", 1.2185769999899365),
            entry("7.5", 1.228247999989141),
            entry("7.6", 1.2378639999883498),
            entry("7.7", 1.2474269999875631),
            entry("7.8", 1.2569389999867806),
            entry("7.9", 1.2663979999860024),
            entry("8.0", 1.2758079999852283),
            entry("8.1", 1.2851679999844583),
            entry("8.2", 1.2944789999836923),
            entry("8.3", 1.3037429999829302),
            entry("8.4", 1.312958999982172),
            entry("8.5", 1.3221289999814176),
            entry("8.6", 1.331253999980667),
            entry("8.7", 1.34033399997992),
            entry("8.8", 1.3493709999791765),
            entry("8.9", 1.3583639999784367),
            entry("9.0", 1.3673139999777004),
            entry("9.1", 1.3762219999769676),
            entry("9.2", 1.3850889999762381),
            entry("9.3", 1.393915999975512),
            entry("9.4", 1.4027019999747892),
            entry("9.5", 1.4114489999740696),
            entry("9.6", 1.4201579999733531),
            entry("9.7", 1.4288279999726399),
            entry("9.8", 1.4374599999719297),
            entry("9.9", 1.4460549999712227),
            entry("10.0", 1.4546129999705186)
    );

    public static final DecimalFormat JUMP_FORMAT = new DecimalFormat("#.0");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        JUMP_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        JUMP_FORMAT.setGroupingUsed(false);
        JUMP_FORMAT.setDecimalFormatSymbols(symbols);
    }
}
