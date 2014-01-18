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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jraf.irondad.Constants;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.ClientConfig.HandlerClassAndConfig;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class HandlerManager {
    private static final String TAG = Constants.TAG + HandlerManager.class.getSimpleName();


    private final Map<Handler, HandlerContext> mPrivmsgHandlerContexts = new HashMap<Handler, HandlerContext>();
    private final Map<String, Map<Handler, HandlerContext>> mChannelHandlerContexts = new HashMap<String, Map<Handler, HandlerContext>>();

    public HandlerManager(ClientConfig clientConfig) {
        HashMap<Class<? extends Handler>, Handler> allHandlers = new HashMap<Class<? extends Handler>, Handler>();

        // Privmsg handlers
        for (String configName : clientConfig.getPrivmsgHandlerConfigNames()) {
            try {
                HandlerClassAndConfig handlerClassAndConfig = clientConfig.getHandlerConfig(configName);
                Handler handler = getHandler(allHandlers, handlerClassAndConfig.handlerClass, clientConfig, configName);
                mPrivmsgHandlerContexts.put(handler, new HandlerContext(handlerClassAndConfig.handlerConfig));
            } catch (Exception e) {
                Log.e(TAG, "Could not get handler for config '" + configName + "'", e);
            }
        }

        // Channel handlers
        for (String channel : clientConfig.getChannels()) {
            Map<Handler, HandlerContext> channelHandlerContexts = new HashMap<Handler, HandlerContext>();
            for (String configName : clientConfig.getChannelHandlerConfigNames(channel)) {
                try {
                    HandlerClassAndConfig handlerClassAndConfig = clientConfig.getHandlerConfig(configName);
                    Handler handler = getHandler(allHandlers, handlerClassAndConfig.handlerClass, clientConfig, configName);
                    channelHandlerContexts.put(handler, new HandlerContext(handlerClassAndConfig.handlerConfig));
                } catch (Exception e) {
                    Log.e(TAG, "Could not get handler for config '" + configName + "'", e);
                }
            }
            mChannelHandlerContexts.put(channel, channelHandlerContexts);
        }
    }

    private Handler getHandler(HashMap<Class<? extends Handler>, Handler> allHandlers, Class<? extends Handler> handlerClass, ClientConfig clientConfig,
            String configName) throws Exception {
        Handler res = allHandlers.get(handlerClass);
        if (res == null) {
            res = handlerClass.newInstance();
            res.init(clientConfig);
            allHandlers.put(handlerClass, res);
        }
        return res;
    }

    public void handle(Connection connection, String channel, String fromNickname, String text, Message message) {
        List<String> textAsList = Arrays.asList(text.split("\\s+"));

        if (channel == null) {
            // Handle privmsgs
            for (Handler handler : mPrivmsgHandlerContexts.keySet()) {
                try {
                    if (handler.handleMessage(connection, null, fromNickname, text, textAsList, message, mPrivmsgHandlerContexts.get(handler))) break;
                } catch (Exception e) {
                    Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                }
            }
        } else {
            // Handle channel msgs
            Map<Handler, HandlerContext> channelHandlerContexts = mChannelHandlerContexts.get(channel);
            for (Handler handler : channelHandlerContexts.keySet()) {
                try {
                    if (handler.handleMessage(connection, channel, fromNickname, text, textAsList, message, channelHandlerContexts.get(handler))) break;
                } catch (Exception e) {
                    Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                }
            }
        }
    }
}
