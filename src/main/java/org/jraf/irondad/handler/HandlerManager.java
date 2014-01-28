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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.ClientConfig.HandlerClassAndConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class HandlerManager {
    private static final String TAG = Constants.TAG + HandlerManager.class.getSimpleName();

    // No more than 5 messages in 20 seconds
    private static final int FLOOD_LOG_SIZE_MAX = 5;
    private static final int FLOOD_TIME_DIFF = 20000;
    private static final int FLOOD_PAUSE_DURATION = 2 * 60 * 1000;

    private final Map<Handler, HandlerContext> mPrivmsgHandlerContexts = new HashMap<Handler, HandlerContext>();
    private final Map<String, Map<Handler, HandlerContext>> mChannelHandlerContexts = new HashMap<String, Map<Handler, HandlerContext>>();

    private static class FloodControl {
        private final Deque<Long> mFloodLog = new ArrayDeque<Long>(FLOOD_LOG_SIZE_MAX);
        private long mFloodPreventStart;
        private boolean mFloodShowWarning;
    }

    private final Map<String, FloodControl> mFloodControl = new HashMap<String, FloodControl>();


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
        String chanOrNick = channel == null ? fromNickname : channel;
        List<String> textAsList = Arrays.asList(text.split("\\s+"));
        if (channel == null) {
            // Handle privmsgs
            for (Handler handler : mPrivmsgHandlerContexts.keySet()) {
                if (handler.isMessageHandled(null, fromNickname, text, textAsList, message, mPrivmsgHandlerContexts.get(handler))) {
                    if (checkForFloodLocked(connection, chanOrNick)) {
                        return;
                    }
                    accountForFlood(connection, chanOrNick);
                    try {
                        handler.handleMessage(connection, null, fromNickname, text, textAsList, message, mPrivmsgHandlerContexts.get(handler));
                    } catch (Exception e) {
                        Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                    }
                    break;
                }
            }
        } else {
            // Handle channel msgs
            Map<Handler, HandlerContext> channelHandlerContexts = mChannelHandlerContexts.get(channel);
            for (Handler handler : channelHandlerContexts.keySet()) {
                if (handler.isMessageHandled(channel, fromNickname, text, textAsList, message, channelHandlerContexts.get(handler))) {
                    if (checkForFloodLocked(connection, chanOrNick)) {
                        return;
                    }
                    accountForFlood(connection, chanOrNick);
                    try {
                        handler.handleMessage(connection, channel, fromNickname, text, textAsList, message, channelHandlerContexts.get(handler));
                    } catch (Exception e) {
                        Log.w(TAG, "handle Handler " + handler + " threw an exception while calling handleMessage", e);
                    }
                    break;
                }
            }
        }
    }

    private FloodControl getFloodControl(String chanOrNick) {
        FloodControl res = mFloodControl.get(chanOrNick);
        if (res == null) {
            res = new FloodControl();
            mFloodControl.put(chanOrNick, res);
        }
        return res;
    }

    private boolean checkForFloodLocked(Connection connection, String chanOrNick) {
        long now = System.currentTimeMillis();
        FloodControl floodControl = getFloodControl(chanOrNick);
        if (floodControl.mFloodPreventStart != 0) {
            // Currently pausing
            if (now - floodControl.mFloodPreventStart < FLOOD_PAUSE_DURATION) {
                if (Config.LOGD) Log.d(TAG, "handleFlood Flood prevention: ignoring message");

                if (floodControl.mFloodShowWarning) {
                    try {
                        connection.send(Command.PRIVMSG, chanOrNick, "Throttled");
                    } catch (IOException e) {
                        Log.w(TAG, "handle Could not send flood warning", e);
                    }
                    floodControl.mFloodShowWarning = false;
                }

                return true;
            }
            if (Config.LOGD) Log.d(TAG, "handleFlood Flood prevention: end of period");
            floodControl.mFloodPreventStart = 0;
            return false;
        }
        return false;
    }

    private void accountForFlood(Connection connection, String chanOrNick) {
        long now = System.currentTimeMillis();
        FloodControl floodControl = getFloodControl(chanOrNick);
        floodControl.mFloodLog.addLast(now);
        if (Config.LOGD) Log.d(TAG, "handleFlood mFloodLog=" + floodControl.mFloodLog);
        if (floodControl.mFloodLog.size() < FLOOD_LOG_SIZE_MAX) {
            if (Config.LOGD) Log.d(TAG, "handleFlood Not enough samples");
            return;
        }
        // Make room for the new value
        floodControl.mFloodLog.removeFirst();
        long timeDiff = now - floodControl.mFloodLog.getFirst();

        if (Config.LOGD) Log.d(TAG, "handleFlood timeDiff=" + timeDiff);

        if (timeDiff < FLOOD_TIME_DIFF) {
            // Too many messages in not enough time
            floodControl.mFloodPreventStart = now;
            floodControl.mFloodShowWarning = true;
        }
    }
}
