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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jraf.irondad.Constants;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class HandlerManager {
    private static final String TAG = Constants.TAG + HandlerManager.class.getSimpleName();

    private List<Handler> mPrivmsgHandlers;
    private Map<String, List<Handler>> mChannelHandlers;


    public HandlerManager(ClientConfig clientConfig) {
        HashMap<Class<? extends Handler>, Handler> allHandlers = new HashMap<Class<? extends Handler>, Handler>();

        mPrivmsgHandlers = getHandlerList(clientConfig.getPrivmsgHandlers(), allHandlers, clientConfig);
        mChannelHandlers = new HashMap<String, List<Handler>>();
        for (String channel : clientConfig.getChannels()) {
            mChannelHandlers.put(channel, getHandlerList(clientConfig.getHandlers(channel), allHandlers, clientConfig));
        }
    }

    private List<Handler> getHandlerList(List<Class<? extends Handler>> handlerClasses, HashMap<Class<? extends Handler>, Handler> allHandlers,
            ClientConfig clientConfig) {
        List<Handler> res = new ArrayList<Handler>();
        for (Class<? extends Handler> handlerClass : handlerClasses) {
            Handler handler = null;
            try {
                handler = getHandler(handlerClass, allHandlers, clientConfig);
            } catch (Exception e) {
                Log.w(TAG, "HandlerManager Could not get Handler " + handlerClass, e);
            }
            if (handler != null) res.add(handler);
        }
        return res;
    }

    private Handler getHandler(Class<? extends Handler> handlerClass, HashMap<Class<? extends Handler>, Handler> allHandlers, ClientConfig clientConfig)
            throws Exception {
        Handler res = allHandlers.get(handlerClass);
        if (res == null) {
            res = handlerClass.newInstance();
            res.init(clientConfig);
            allHandlers.put(handlerClass, res);
        }
        return res;
    }

    //    private void initRegistry(ClientConfig clientConfig) {
    //        mHandlerRegistry = new ArrayList<Handler>(10);
    //
    //        mHandlerRegistry.add(new ControlHandler(clientConfig));
    //        mHandlerRegistry.add(new YoutubeHandler(clientConfig));
    //        mHandlerRegistry.add(new TwitterHandler(clientConfig));
    //        mHandlerRegistry.add(new HelloWorldHandler(clientConfig));
    //        mHandlerRegistry.add(new MtgoxHandler(clientConfig));
    //        mHandlerRegistry.add(new QuoteHandler(clientConfig));
    //
    //        mHandlerRegistry.add(new PixGameHandler(clientConfig));
    //    }

    public void handle(Connection connection, String channel, String fromNickname, String text, Message message) {
        List<String> textAsList = Arrays.asList(text.split("\\s+"));

        if (channel == null) {
            // Handle privmsgs
            for (Handler handler : mPrivmsgHandlers) {
                try {
                    if (handler.handleMessage(connection, null, fromNickname, text, textAsList, message)) break;
                } catch (Exception e) {
                    Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                }
            }
        } else {
            // Handle channel msgs
            for (Handler handler : mChannelHandlers.get(channel)) {
                try {
                    if (handler.handleMessage(connection, channel, fromNickname, text, textAsList, message)) break;
                } catch (Exception e) {
                    Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                }
            }
        }
    }
}
