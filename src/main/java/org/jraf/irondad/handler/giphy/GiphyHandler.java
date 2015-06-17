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
package org.jraf.irondad.handler.giphy;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;
import org.json.JSONObject;

import com.github.kevinsawicki.http.HttpRequest;

public class GiphyHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + GiphyHandler.class.getSimpleName();

    private static final String URL_API_TRANSLATE = "http://api.giphy.com/v1/gifs/translate?s=%1s&api_key=%2s";
    private static final String URL_API_RANDOM = "http://api.giphy.com/v1/gifs/random?api_key=%1s";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    public String getCommand() {
        return "!gif";
    }

    @Override
    protected void handleChannelMessage(final Connection connection, final String channel, String fromNickname, final String text,
            final List<String> textAsList, Message message, HandlerContext handlerContext) throws Exception {
        final String key = ((GiphyHandlerConfig) handlerContext.getHandlerConfig()).getKey();
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                String result;
                if (textAsList.size() == 1) {
                    // No parameters: random gif
                    result = callRandom(key);
                } else {
                    // "Translate" api
                    String searchTerms = text.substring(getCommand().length() + 1);
                    result = callTranslate(key, searchTerms);
                }
                try {
                    connection.send(Command.PRIVMSG, channel, result);
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }

    protected String callTranslate(String apiKey, String searchTerms) {
        try {
            searchTerms = URLEncoder.encode(searchTerms, "utf-8");
            String url = String.format(URL_API_TRANSLATE, searchTerms, apiKey);
            String jsonStr = HttpRequest.get(url).userAgent(USER_AGENT).body();
            if (Config.LOGD) Log.d(TAG, "callTranslate jsonStr=" + jsonStr);
            JSONObject result = new JSONObject(jsonStr);
            JSONObject data = result.getJSONObject("data");
            JSONObject images = data.getJSONObject("images");
            JSONObject original = images.getJSONObject("original");
            String gifUrl = original.getString("url");
            return gifUrl;
        } catch (Exception e) {
            if (Config.LOGD) Log.d(TAG, "queryGiphy", e);
            return "No result";
        }
    }

    protected String callRandom(String apiKey) {
        try {
            String url = String.format(URL_API_RANDOM, apiKey);
            String jsonStr = HttpRequest.get(url).userAgent(USER_AGENT).body();
            if (Config.LOGD) Log.d(TAG, "callTranslate jsonStr=" + jsonStr);
            JSONObject result = new JSONObject(jsonStr);
            if (Config.LOGD) Log.d(TAG, "callTranslate result=" + result);
            JSONObject data = result.getJSONObject("data");
            String gifUrl = data.getString("image_original_url");
            return gifUrl;
        } catch (Exception e) {
            if (Config.LOGD) Log.d(TAG, "queryGiphy", e);
            return "No result";
        }
    }
}
