/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.irondad.handler.srm;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class SrmHandler extends BaseHandler {
    private static final String NICKNAME = "srm";
    private static final Pattern PATTERN_OSEF =
            Pattern.compile("(.*note\\s*4.*)|(.*n\\s*4.*)|(.*nexus.*)|(.*n\\s*6.*)|(.*essential.*)|(.*pixel.*)", Pattern.CASE_INSENSITIVE);


    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        if (!fromNickname.toLowerCase(Locale.US).contains(NICKNAME.toLowerCase(Locale.US))) return false;
        return osef(text);
    }

    @Override
    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        if (!fromNickname.toLowerCase(Locale.US).contains(NICKNAME.toLowerCase(Locale.US))) return;
        if (osef(text)) {
            connection.send(Command.PRIVMSG,
                    new String[] {channel, fromNickname + ": osef de ton Note 4 et/ou de ton Nexus 6[P] et/ou de ton Essential et/ou de ton Pixel [1|2|XL]"});
        }
    }

    private boolean osef(String text) {
        Matcher matcher = PATTERN_OSEF.matcher(text);
        return matcher.matches();
    }
}