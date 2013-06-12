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
package org.jraf.irondad.protocol;

public class Origin {
    public final String name;
    public final String user;
    public final String host;

    public Origin(String originStr) {
        if (originStr.contains("!")) {
            String[] originSplit = originStr.split("\\!");
            name = originSplit[0];
            String[] userAtHost = originSplit[1].split("@");
            user = userAtHost[0];
            host = userAtHost[1];
        } else {
            name = originStr;
            user = null;
            host = null;
        }
    }

    @Override
    public String toString() {
        return "Origin [name=" + name + ", user=" + user + ", host=" + host + "]";
    }

    public String toFormattedString() {
        if (user != null) {
            return name + "!" + user + "@" + host;
        }
        return name;
    }
}
