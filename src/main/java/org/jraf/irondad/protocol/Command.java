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

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public enum Command {
    //@formatter:off
    
    /*
     * Messages.
     */
    
    PING,
    PONG,
    NICK,
    USER,
    JOIN,
    PART,
    PRIVMSG,
    NAMES,
    QUIT,
    CAP,
    AUTHENTICATE,
    
    UNKNOWN, 
    
    
    /*
     * Replies.
     */
    
    RPL_WELCOME(1),
    
    RPL_NAMREPLY(353),
    RPL_LOGGEDIN(900),
    RPL_SASLSUCCESS(903),
    ERR_SASLFAIL(904),
    ERR_SASLTOOLONG(905),
    ERR_SASLABORTED(906),
    ERR_SASLALREADY(907),
    RPL_SASLMECHS(908),
    
    ERR_ERRONEUSNICKNAME(432),
    ERR_NICKNAMEINUSE(433),
    ERR_NICKCOLLISION(436),
    
    ;

    //@formatter:on

    private static final HashMap<String, Command> sCommandByName = new HashMap<String, Command>(40);
    private static final HashMap<Integer, Command> sCommandByCode = new HashMap<Integer, Command>(40);

    static {
        for (Command command : values()) {
            sCommandByName.put(command.name(), command);
            if (command.mCode != -1) sCommandByCode.put(command.mCode, command);
        }
    }

    private int mCode;

    private Command() {
        mCode = -1;
    }

    private Command(int code) {
        mCode = code;
    }

    public static Command from(String commandStr) {
        if (StringUtils.isNumeric(commandStr)) {
            // Reply (numeric command)
            return from(Integer.valueOf(commandStr));
        }
        // Message (string command)
        Command res = sCommandByName.get(commandStr);
        if (res == null) return UNKNOWN;
        return res;
    }

    public static Command from(int code) {
        Command res = sCommandByCode.get(code);
        if (res == null) return UNKNOWN;
        return res;
    }
}
