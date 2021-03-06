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
package org.jraf.irondad.handler;

import java.util.List;

import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public abstract class BaseHandler implements Handler {
    @Override
    public void init(ClientConfig clientConfig) throws Exception {}

    @Override
    public void init(HandlerContext handlerContext) throws Exception {}

    @Override
    public void handleMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        if (channel != null) {
            handleChannelMessage(connection, channel, fromNickname, text, textAsList, message, handlerContext);
            return;
        }

        handlePrivmsgMessage(connection, fromNickname, text, textAsList, message, handlerContext);
    }

    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {}

    protected void handlePrivmsgMessage(Connection connection, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {}
}
