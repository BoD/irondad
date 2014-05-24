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
package org.jraf.irondad.handler.feed;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class FeedHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + FeedHandler.class.getSimpleName();

    @Override
    public void init(ClientConfig clientConfig) throws Exception {}

    private String getUrl(List<String> textAsList, HandlerContext handlerContext) {
        FeedHandlerConfig handlerConfig = (FeedHandlerConfig) handlerContext.getHandlerConfig();
        String command = textAsList.get(0);
        String url = handlerConfig.get(command);
        return url;
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        String url = getUrl(textAsList, handlerContext);
        return url != null;
    }

    @Override
    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        String url = getUrl(textAsList, handlerContext);
        SyndEntry latestEntry = getLatestEntry(url);
        if (latestEntry == null) return;
        String entryLink = latestEntry.getLink();
        if (entryLink == null) {
            entryLink = latestEntry.getUri();
        }
        if (entryLink == null) {
            if (Config.LOGD) Log.d(TAG, "handleChannelMessage Could not get link for this entry");
            return;
        }
        connection.send(Command.PRIVMSG, channel, entryLink);
    }

    private SyndEntry getLatestEntry(String urlStr) {
        XmlReader reader = null;
        try {
            URL url = new URL(urlStr);
            reader = new XmlReader(url);
            SyndFeed feed = new SyndFeedInput().build(reader);
            return (SyndEntry) feed.getEntries().get(0);
        } catch (Throwable e) {
            Log.e(TAG, "Could not get latest entrey of feed " + urlStr, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Silently ignored
                }
            }
        }
        return null;
    }
}
