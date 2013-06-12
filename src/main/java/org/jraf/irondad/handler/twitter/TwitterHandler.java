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
package org.jraf.irondad.handler.twitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.Handler;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

public class TwitterHandler implements Handler {
    private static final String TAG = Constants.TAG + TwitterHandler.class.getSimpleName();

    private static final Pattern PATTERN_TWEET_ID = Pattern.compile("(.*twitter\\.com.*status/)([0-9]+)[^0-9]*.*", Pattern.CASE_INSENSITIVE);
    private static final int PATTERN_TWEET_ID_GROUP = 2;
    private static final String URL_API_TWEET = "https://api.twitter.com/1/statuses/show/%s.json";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    public TwitterHandler(ClientConfig clientConfig) {}

    @Override
    public boolean handleMessage(final Connection connection, final String channel, final String fromNickname, String text, List<String> textAsList,
            Message message) throws Exception {
        if (channel == null) {
            // Ignore private messages
            return false;
        }
        final String tweetId = getTweetId(text);
        if (tweetId == null) {
            // Text doesn't contain a twitter link: ignore
            return false;
        }
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                String uri = String.format(URL_API_TWEET, tweetId);
                try {
                    String jsonStr = HttpRequest.get(uri).body();
                    JSONObject mainObject = new JSONObject(jsonStr);
                    String tweetText = mainObject.getString("text");
                    connection.send(Command.PRIVMSG, channel, tweetText);
                } catch (HttpRequestException e) {
                    Log.w(TAG, "handleMessage Could not get " + uri, e);
                } catch (JSONException e) {
                    Log.w(TAG, "handleMessage Could not parse json", e);
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
        return true;
    }

    private static String getTweetId(String text) {
        Matcher matcher = PATTERN_TWEET_ID.matcher(text);
        if (!matcher.matches()) return null;
        return matcher.group(PATTERN_TWEET_ID_GROUP);
    }
}
