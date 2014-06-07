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
package org.jraf.irondad.handler.control;

import java.util.List;

import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class ControlHandler extends CommandHandler {
    @Override
    protected String getCommand() {
        return "!control";
    }

    @Override
    public void handlePrivmsgMessage(Connection connection, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        String adminPassword = connection.getClient().getClientConfig().getAdminPassword();
        if (!adminPassword.equals(textAsList.get(1))) return;
        connection.send(text.substring(getCommand().length() + adminPassword.length() + 2));
    }
}
