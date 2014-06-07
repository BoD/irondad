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

import java.util.HashMap;

import org.jraf.irondad.protocol.Connection;

public class HandlerContext extends HashMap<String, Object> {
    private final HandlerConfig mHandlerConfig;
    private final String mChannelName;
    private Connection mConnection;

    public HandlerContext(HandlerConfig handlerConfig, String channelName) {
        mHandlerConfig = handlerConfig;
        mChannelName = channelName;
    }

    public HandlerConfig getHandlerConfig() {
        return mHandlerConfig;
    }

    public void setConnection(Connection connection) {
        mConnection = connection;
    }

    public Connection getConnection() {
        return mConnection;
    }

    /**
     * @return {@code null} if this is a privmsg context.
     */
    public String getChannelName() {
        return mChannelName;
    }
}
