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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jraf.irondad.handler.Handler;
import org.jraf.irondad.handler.HandlerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientConfig {
    public static class HandlerClassAndConfig {
        public Class<? extends Handler> handlerClass;
        public HandlerConfig handlerConfig;
    }

    private final String mHost;
    private final int mPort;
    private final String mNickname;
    private final String mAdminPassword;
    private final Map<String, HandlerClassAndConfig> mHandlerConfigs = new HashMap<String, HandlerClassAndConfig>();
    private final List<String> mPrivmsgHandlerConfigNames = new ArrayList<String>();
    private final ListMultimap<String, String> mChannelHandlerConfigNames = ArrayListMultimap.create();
    private final String mSaslUsername;
    private final String mSaslPassword;

    public ClientConfig(String host, int port, String nickname, String adminPassword) {
        this(host, port, nickname, adminPassword, null, null);
    }

    public ClientConfig(String host, int port, String nickname, String adminPassword, String saslUsername, String saslPassword) {
        mHost = host;
        mPort = port;
        mNickname = nickname;
        mAdminPassword = adminPassword;
        mSaslUsername = saslUsername;
        mSaslPassword = saslPassword;
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public Set<String> getChannels() {
        return Collections.unmodifiableSet(mChannelHandlerConfigNames.keySet());
    }

    public String getNickname() {
        return mNickname;
    }

    public String getAdminPassword() {
        return mAdminPassword;
    }

    public String getSaslUsername() {
        return mSaslUsername;
    }

    public String getSaslPassword() {
        return mSaslPassword;
    }

    public void addHandlerConfig(String configName, Class<? extends Handler> handlerClass, HandlerConfig handlerConfig) {
        HandlerClassAndConfig handlerClassAndConfig = new HandlerClassAndConfig();
        handlerClassAndConfig.handlerClass = handlerClass;
        handlerClassAndConfig.handlerConfig = handlerConfig;
        mHandlerConfigs.put(configName, handlerClassAndConfig);
    }

    public HandlerClassAndConfig getHandlerConfig(String configName) {
        return mHandlerConfigs.get(configName);
    }

    public List<String> getPrivmsgHandlerConfigNames() {
        return Collections.unmodifiableList(mPrivmsgHandlerConfigNames);
    }

    public void addPrivmsgHandlerConfig(String configName) {
        mPrivmsgHandlerConfigNames.add(configName);
    }

    public List<String> getChannelHandlerConfigNames(String channel) {
        return Collections.unmodifiableList(mChannelHandlerConfigNames.get(channel));
    }

    public void addChannelHandlerConfig(String channel, String configName) {
        mChannelHandlerConfigNames.put(channel, configName);
    }
}
