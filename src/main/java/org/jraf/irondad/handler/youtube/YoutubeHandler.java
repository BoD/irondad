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
package org.jraf.irondad.handler.youtube;

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

public class YoutubeHandler implements Handler {
    private static final String TAG = Constants.TAG + YoutubeHandler.class.getSimpleName();

    private static final Pattern PATTERN_VIDEO_ID = Pattern.compile("((.*youtube\\.com.*v=)|(.*youtu\\.be/))([a-zA-Z0-9_\\-]+)[^a-zA-Z0-9_\\-]*.*",
            Pattern.CASE_INSENSITIVE);
    private static final int PATTERN_VIDEO_ID_GROUP = 4;
    private static final String URL_API_VIDEO = "http://gdata.youtube.com/feeds/api/videos/%s?alt=json&prettyprint=true";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    public YoutubeHandler(ClientConfig clientConfig) {}

    @Override
    public boolean handleMessage(final Connection connection, final String channel, final String fromNickname, String text, List<String> textAsList,
            Message message) throws Exception {
        if (channel == null) {
            // Ignore private messages
            return false;
        }
        final String videoId = getVideoId(text);
        if (videoId == null) {
            // Text doesn't contain a youtube link: ignore
            return false;
        }
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                String uri = String.format(URL_API_VIDEO, videoId);
                try {
                    String jsonStr = HttpRequest.get(uri).body();
                    JSONObject mainObject = new JSONObject(jsonStr);
                    JSONObject entryObject = mainObject.getJSONObject("entry");
                    JSONObject titleObject = entryObject.getJSONObject("title");
                    String title = titleObject.getString("$t");
                    connection.send(Command.PRIVMSG, channel, title);
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

    private static String getVideoId(String text) {
        Matcher matcher = PATTERN_VIDEO_ID.matcher(text);
        if (!matcher.matches()) return null;
        return matcher.group(PATTERN_VIDEO_ID_GROUP);
    }
}
