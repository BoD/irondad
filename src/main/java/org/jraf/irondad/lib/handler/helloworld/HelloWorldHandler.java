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
package org.jraf.irondad.lib.handler.helloworld;

import java.util.List;
import java.util.Locale;

import org.jraf.irondad.lib.Constants;
import org.jraf.irondad.lib.handler.Handler;
import org.jraf.irondad.lib.protocol.ClientConfig;
import org.jraf.irondad.lib.protocol.Command;
import org.jraf.irondad.lib.protocol.Connection;
import org.jraf.irondad.lib.protocol.Message;

public class HelloWorldHandler implements Handler {
    private static final String TAG = Constants.TAG + HelloWorldHandler.class.getSimpleName();

    private static final String COMMAND = "hello";

    public HelloWorldHandler(ClientConfig clientConfig) {}

    @Override
    public boolean handleMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message)
            throws Exception {
        if (channel != null) {
            // Ignore channel messages
            return false;
        }

        if (!text.trim().toLowerCase(Locale.getDefault()).startsWith(COMMAND)) return false;

        connection.send(Command.PRIVMSG, fromNickname, "Well hello to you too, sir!");
        return true;
    }
}
