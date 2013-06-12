/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.jraf.irondad.lib.util;

public class StringUtil {
    /**
     * Returns whether the given string contains only digits.
     */
    public static boolean isDigitsOnly(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string containing given the tokens joined by the given delimiters.
     */
    public static String join(String delimiter, Object[] tokens) {
        StringBuilder res = new StringBuilder();
        boolean first = true;
        for (Object token : tokens) {
            if (!first) {
                res.append(delimiter);
            } else {
                first = false;
            }
            res.append(token);
        }
        return res.toString();
    }
}
