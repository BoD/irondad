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
package org.jraf.irondad.handler.monitorpage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.kevinsawicki.http.HttpRequest;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class MonitorPageHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + MonitorPageHandler.class.getSimpleName();
    private static final String PAGE_HASHES = "pageHashes";

    @Override
    public void init(HandlerContext handlerContext) throws Exception {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new CheckForPageChangesRunnable(handlerContext), 3, 3, TimeUnit.MINUTES);
//        scheduledExecutorService.scheduleAtFixedRate(new CheckForPageChangesRunnable(handlerContext), 0, 15, TimeUnit.SECONDS);
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        return false;
    }

    public static class CheckForPageChangesRunnable implements Runnable {
        private final HandlerContext mHandlerContext;


        CheckForPageChangesRunnable(HandlerContext handlerContext) {
            mHandlerContext = handlerContext;
        }

        @Override
        public void run() {
            try {
                if (Config.LOGD) Log.d(TAG, "run Checking page changes");

                MonitorPageHandlerConfig config = (MonitorPageHandlerConfig) mHandlerContext.getHandlerConfig();
                for (MonitorPageHandlerConfig.Page page : config.getPages()) {
                    String pageUri = page.uri;
                    if (Config.LOGD) Log.d(TAG, "run pageUri=" + pageUri);

                    // Blocking
                    String pageContents = HttpRequest.get(pageUri).body();

                    // Extract content
                    pageContents = pageContents.substring(pageContents.indexOf(page.contentStart));
                    pageContents = pageContents.substring(0, pageContents.lastIndexOf(page.contentEnd));

                    int pageHash = pageContents.hashCode();
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> pageHashMap = (Map<String, Integer>) mHandlerContext.get(PAGE_HASHES);
                    if (pageHashMap == null) {
                        pageHashMap = new HashMap<>();
                        mHandlerContext.put(PAGE_HASHES, pageHashMap);
                    }
                    if (!pageHashMap.containsKey(pageUri)) {
                        if (Config.LOGD) Log.d(TAG, "run First run");
                        pageHashMap.put(pageUri, pageHash);
                    } else {
                        int previousPageHash = pageHashMap.get(pageUri);
                        if (previousPageHash != pageHash) {
                            if (Config.LOGD) Log.d(TAG, "run Change detected");

                            // Change in the page
                            String text = "This page changed! " + pageUri;
                            mHandlerContext.getConnection().send(Command.PRIVMSG, mHandlerContext.getChannelName(), text);

                            // Save the new hash
                            pageHashMap.put(pageUri, pageHash);
                        } else {
                            if (Config.LOGD) Log.d(TAG, "run No change");
                        }
                    }
                }

            } catch (Exception e) {
                Log.w(TAG, "run", e);
            }
        }
    }

}
