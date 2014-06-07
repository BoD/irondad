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
package org.jraf.irondad.handler.macro;

import java.util.List;

import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class MacroHandler extends BaseHandler {
    private String getReply(List<String> textAsList, HandlerContext handlerContext) {
        MacroHandlerConfig handlerConfig = (MacroHandlerConfig) handlerContext.getHandlerConfig();
        String command = textAsList.get(0);
        String reply = handlerConfig.get(command);
        return reply;
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        String reply = getReply(textAsList, handlerContext);
        return reply != null;
    }

    @Override
    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        String reply = getReply(textAsList, handlerContext);
        connection.send(Command.PRIVMSG, channel, reply);
    }
}
