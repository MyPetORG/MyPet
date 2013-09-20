/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util.itemstringinterpreter;

import net.minecraft.server.v1_6_R3.NBTBase;

import java.util.Stack;

/**
 * This class is part of Minecraft 1.7 and will be removed with Minecraft 1.7
 */
public class ItemStringInterpreter {
    public static NBTBase convertString(String data) throws Exception {
        data = data.trim();
        int i = countTags(data);
        if (i != 1) {
            throw new Exception("Encountered multiple top tags, only one expected");
        }

        NBTHolder holder;
        if (data.startsWith("{")) {
            holder = getTag("", data);
        } else {
            holder = getTag(b(data, false), c(data, false));
        }

        return holder.getNBT();
    }

    static int countTags(String paramString) throws Exception {
        int i = 0;
        int j = 0;
        Stack<Character> localStack = new Stack<Character>();

        int k = 0;
        while (k < paramString.length()) {
            char c = paramString.charAt(k);
            if (c == '"') {
                if ((k > 0) && (paramString.charAt(k - 1) == '\\')) {
                    if (j == 0) {
                        throw new Exception("Illegal use of \\\": " + paramString);
                    }
                } else {
                    j = j == 0 ? 1 : 0;
                }
            } else if (j == 0) {
                if ((c == '{') || (c == '[')) {
                    if (localStack.isEmpty()) {
                        i++;
                    }
                    localStack.push(c);
                } else {
                    if ((c == '}') && ((localStack.isEmpty()) || (localStack.pop() != '{'))) {
                        throw new Exception("Unbalanced curly brackets {}: " + paramString);
                    }
                    if ((c == ']') && ((localStack.isEmpty()) || (localStack.pop() != '['))) {
                        throw new Exception("Unbalanced square brackets []: " + paramString);
                    }
                }
            }
            k++;
        }
        if (j != 0) {
            throw new Exception("Unbalanced quotation: " + paramString);
        }
        if (!localStack.isEmpty()) {
            throw new Exception("Unbalanced brackets: " + paramString);
        }

        if ((i == 0) && (!paramString.isEmpty())) {
            return 1;
        }
        return i;
    }

    static NBTHolder getTag(String tagName, String nbtString) throws Exception {
        nbtString = nbtString.trim();
        countTags(nbtString);
        NBTHolder nbtHolder;
        String str1;
        String str2;
        String str3;
        char currentCharacter;
        if (nbtString.startsWith("{")) {
            if (!nbtString.endsWith("}")) {
                throw new Exception("Unable to locate ending bracket for: " + nbtString);
            }

            nbtString = nbtString.substring(1, nbtString.length() - 1);

            nbtHolder = new NBTTagCompoundHolder(tagName);
            while (nbtString.length() > 0) {
                str1 = a(nbtString, false);
                if (str1.length() > 0) {
                    str2 = b(str1, false);
                    str3 = c(str1, false);
                    ((NBTTagCompoundHolder) nbtHolder).holderList.add(getTag(str2, str3));

                    if (nbtString.length() < str1.length() + 1) {
                        break;
                    }
                    currentCharacter = nbtString.charAt(str1.length());
                    if ((currentCharacter != ',') && (currentCharacter != '{') && (currentCharacter != '}') && (currentCharacter != '[') && (currentCharacter != ']')) {
                        throw new Exception("Unexpected token '" + currentCharacter + "' at: " + nbtString.substring(str1.length()));
                    }
                    nbtString = nbtString.substring(str1.length() + 1);
                }

            }

            return nbtHolder;
        }
        if ((nbtString.startsWith("[")) && (!nbtString.matches("\\[[-\\d|,\\s]+\\]"))) {
            if (!nbtString.endsWith("]")) {
                throw new Exception("Unable to locate ending bracket for: " + nbtString);
            }

            nbtString = nbtString.substring(1, nbtString.length() - 1);

            nbtHolder = new NBTTagListHolder(tagName);
            while (nbtString.length() > 0) {
                str1 = a(nbtString, true);
                if (str1.length() > 0) {
                    str2 = b(str1, true);
                    str3 = c(str1, true);
                    ((NBTTagListHolder) nbtHolder).holderList.add(getTag(str2, str3));

                    if (nbtString.length() < str1.length() + 1) {
                        break;
                    }
                    currentCharacter = nbtString.charAt(str1.length());
                    if ((currentCharacter != ',') && (currentCharacter != '{') && (currentCharacter != '}') && (currentCharacter != '[') && (currentCharacter != ']')) {
                        throw new Exception("Unexpected token '" + currentCharacter + "' at: " + nbtString.substring(str1.length()));
                    }
                    nbtString = nbtString.substring(str1.length() + 1);
                } else {
                    System.out.println(nbtString);
                }
            }

            return nbtHolder;
        }
        return new NBTTagHolder(tagName, nbtString);
    }

    private static String a(String paramString, boolean paramBoolean) throws Exception {
        int i = a(paramString, ':');
        if ((i < 0) && (!paramBoolean)) {
            throw new Exception("Unable to locate name/value separator for string: " + paramString);
        }
        int j = a(paramString, ',');
        if ((j >= 0) && (j < i) && (!paramBoolean)) {
            throw new Exception("Name error at: " + paramString);
        }
        if ((paramBoolean) && ((i < 0) || (i > j))) {
            i = -1;
        }

        Stack<Character> localStack = new Stack<Character>();
        int k = i + 1;
        int m = 0;
        int n = 0;
        int i1 = 0;
        int i2 = 0;

        while (k < paramString.length()) {
            char c = paramString.charAt(k);

            if (c == '"') {
                if ((k > 0) && (paramString.charAt(k - 1) == '\\')) {
                    if (m == 0) {
                        throw new Exception("Illegal use of \\\": " + paramString);
                    }
                } else {
                    m = m == 0 ? 1 : 0;
                    if ((m != 0) && (i1 == 0)) {
                        n = 1;
                    }
                    if (m == 0) {
                        i2 = k;
                    }
                }
            } else if (m == 0) {
                if ((c == '{') || (c == '[')) {
                    localStack.push(c);
                } else {
                    if ((c == '}') && ((localStack.isEmpty()) || (localStack.pop() != '{'))) {
                        throw new Exception("Unbalanced curly brackets {}: " + paramString);
                    }
                    if ((c == ']') && ((localStack.isEmpty()) || (localStack.pop() != '['))) {
                        throw new Exception("Unbalanced square brackets []: " + paramString);
                    }
                    if ((c == ',') &&
                            (localStack.isEmpty())) {
                        return paramString.substring(0, k);
                    }
                }
            }
            if (!Character.isWhitespace(c)) {
                if ((m == 0) && (n != 0) && (i2 != k)) {
                    return paramString.substring(0, i2 + 1);
                }
                i1 = 1;
            }

            k++;
        }
        return paramString.substring(0, k);
    }

    private static String b(String paramString, boolean paramBoolean) throws Exception {
        if (paramBoolean) {
            paramString = paramString.trim();
            if ((paramString.startsWith("{")) || (paramString.startsWith("["))) {
                return "";
            }
        }

        int i = paramString.indexOf(':');
        if (i < 0) {
            if (paramBoolean) {
                return "";
            }
            throw new Exception("Unable to locate name/value separator for string: " + paramString);
        }
        return paramString.substring(0, i).trim();
    }

    private static String c(String paramString, boolean paramBoolean) throws Exception {
        if (paramBoolean) {
            paramString = paramString.trim();
            if ((paramString.startsWith("{")) || (paramString.startsWith("["))) {
                return paramString;
            }
        }

        int i = paramString.indexOf(':');
        if (i < 0) {
            if (paramBoolean) {
                return paramString;
            }
            throw new Exception("Unable to locate name/value separator for string: " + paramString);
        }
        return paramString.substring(i + 1).trim();
    }

    private static int a(String paramString, char paramChar) {
        int i = 0;
        int j = 0;
        while (i < paramString.length()) {
            char c = paramString.charAt(i);
            if (c == '"') {
                if ((i <= 0) || (paramString.charAt(i - 1) != '\\')) {
                    j = j == 0 ? 1 : 0;
                }
            } else if (j == 0) {
                if (c == paramChar) {
                    return i;
                }
                if ((c == '{') || (c == '[')) {
                    return -1;
                }
            }
            i++;
        }
        return -1;
    }
}