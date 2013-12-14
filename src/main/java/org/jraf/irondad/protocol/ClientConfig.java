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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jraf.irondad.handler.Handler;

public class ClientConfig {
    private final String mHost;
    private final int mPort;
    private final String mNickname;
    private final String mAdminPassword;
    private List<Class<? extends Handler>> mPrivmsgHandlers;
    private final Map<String, List<Class<? extends Handler>>> mChannelHandlers;
    private final Map<String, String> mExtraConfig;


    @SuppressWarnings({ "unchecked" })
    public ClientConfig(String host, int port, String nickname, String adminPassword, List<Class<? extends Handler>> privmsgHandlers,
            HashMap<String, List<Class<? extends Handler>>> channelHandlers) {
        mHost = host;
        mPort = port;
        mNickname = nickname;
        mAdminPassword = adminPassword;
        mPrivmsgHandlers = new ArrayList<Class<? extends Handler>>();
        mPrivmsgHandlers.addAll(privmsgHandlers);
        mChannelHandlers = (HashMap<String, List<Class<? extends Handler>>>) channelHandlers.clone();

        mExtraConfig = new HashMap<String, String>();
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public Set<String> getChannels() {
        return Collections.unmodifiableSet(mChannelHandlers.keySet());
    }

    public String getNickname() {
        return mNickname;
    }

    public String getAdminPassword() {
        return mAdminPassword;
    }

    public void putExtraConfig(String key, String value) {
        mExtraConfig.put(key, value);
    }

    public String getExtraConfig(String key) {
        return mExtraConfig.get(key);
    }

    public List<Class<? extends Handler>> getPrivmsgHandlers() {
        return Collections.unmodifiableList(mPrivmsgHandlers);
    }

    public List<Class<? extends Handler>> getHandlers(String channel) {
        return Collections.unmodifiableList(mChannelHandlers.get(channel));
    }
}