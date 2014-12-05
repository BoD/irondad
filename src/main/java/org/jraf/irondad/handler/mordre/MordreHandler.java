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
package org.jraf.irondad.handler.mordre;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class MordreHandler extends BaseHandler {
    private static final List<String> VERBS = new ArrayList<>();

    static {
        VERBS.add("ai");
        VERBS.add("as");
        VERBS.add("a");
        VERBS.add("avons");
        VERBS.add("avez");
        VERBS.add("ont");
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        if (!fromNickname.equals("neoakira")) return false;
        return !isGrammaticallyCorrect(text);
    }

    @Override
    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        if (!fromNickname.equals("neoakira")) return;
        if (!isGrammaticallyCorrect(text)) {
            connection.send(Command.PRIVMSG, new String[] { channel, "neoakira: mordre" });
        }
    }

    private boolean isGrammaticallyCorrect(String text) {
        String[] split = text.split("[\\p{Punct}\\s]+");
        for (int i = 1; i < split.length; i++) {
            if (split[i].toLowerCase(Locale.US).endsWith("er")) {
                String prevWord = split[i - 1];
                if (VERBS.contains(prevWord.toLowerCase(Locale.US))) {
                    return false;
                }
            }
        }
        return true;
    }
}