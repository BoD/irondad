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

import java.util.ArrayList;

public class Message {
    /**
     * Origin of the message (can be {@code null}).
     */
    public final Origin origin;
    public final Command command;
    public final ArrayList<String> parameters;

    public Message(Origin origin, Command command, ArrayList<String> parameters) {
        this.origin = origin;
        this.command = command;
        this.parameters = parameters;
    }

    public static Message parse(String line) {
        ArrayList<String> split = split(line);
        Origin origin = null;
        if (split.get(0).startsWith(":")) {
            // Prefix is an optional origin
            String originStr = split.remove(0);
            // Remove colon
            originStr = originStr.substring(1);
            origin = new Origin(originStr);
        }
        String commandStr = split.remove(0);
        Command command = Command.from(commandStr);
        return new Message(origin, command, split);
    }



    private static ArrayList<String> split(String line) {
        ArrayList<String> split = new ArrayList<String>(10);
        int len = line.length();
        StringBuilder currentToken = new StringBuilder(10);
        boolean previousCharIsSpace = false;
        boolean lastParam = false;
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ' && !lastParam) {
                // Space
                split.add(currentToken.toString());
                currentToken = new StringBuilder(10);
                previousCharIsSpace = true;
            } else if (c == ':' && i != 0 && previousCharIsSpace) {
                // Colon: if at start of token, the remainder is the last param (can have spaces)
                lastParam = true;
                //                currentToken.append(c);
            } else {
                // Other characters
                currentToken.append(c);
                previousCharIsSpace = false;
            }
        }
        split.add(currentToken.toString());
        return split;
    }

    @Override
    public String toString() {
        return "Message [origin=" + origin + ", command=" + command + ", parameters=" + parameters + "]";
    }
}
