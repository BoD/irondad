/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016 Nicolas Pomepuy
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
package org.jraf.irondad.handler.cyanide;

import com.github.kevinsawicki.http.HttpRequest;
import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CyanideHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + CyanideHandler.class.getSimpleName();

    private static final String URL_HTML = "https://explosm.net/comics/latest";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    protected String getCommand() {
        return "!cyanide";
    }

    @Override
    protected void handleChannelMessage(final Connection connection, final String channel, String fromNickname, String text, List<String> textAsList,
            Message message, HandlerContext handlerContext) throws Exception {
        if (Config.LOGD) Log.d(TAG, "handleChannelMessage");
        final String param;
        if (textAsList.size() > 1) {
            param = textAsList.get(1);
        } else {
            param = "";
        }
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.send(Command.PRIVMSG, channel, getResult(param));
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }

    private static String getResult(String param) {
        String html = HttpRequest.get(URL_HTML).followRedirects(true).body();
        if (Config.LOGD) Log.d(TAG, html);
        String start = "<img src=\"";
        String end = "\"";

        html = html.substring(html.indexOf(start) + start.length());
        html = html.substring(0, html.indexOf(end));
        if (Config.LOGD) Log.d(TAG, html);
        return html;
    }

    public static void main(String[] av) {
        getResult("");
    }
}
