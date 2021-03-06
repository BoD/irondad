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
package org.jraf.irondad.handler.androidstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.kevinsawicki.http.HttpRequest;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class AndroidStatsHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + AndroidStatsHandler.class.getSimpleName();

    private static final String URL_HTML = "https://android-dot-google-developers.appspot.com/about/dashboards/index_95cbf8a418417c3d65b22a91dd29d889.frame";

    private static class StatPoint implements Comparable<StatPoint> {
        public int apiLevel;
        public float percentage;
        public float cumulativePercentage;

        public static StatPoint fromJson(JSONObject json) {
            StatPoint res = new StatPoint();
            res.apiLevel = json.getInt("api");
            res.percentage = Float.parseFloat(json.getString("perc"));
            return res;
        }

        @Override
        public int compareTo(StatPoint o) {
            if (apiLevel < o.apiLevel) return 1;
            if (apiLevel == o.apiLevel) return 0;
            return -1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + apiLevel;
            result = prime * result + Float.floatToIntBits(percentage);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            StatPoint other = (StatPoint) obj;
            if (apiLevel != other.apiLevel) return false;
            if (Float.floatToIntBits(percentage) != Float.floatToIntBits(other.percentage)) return false;
            return true;
        }

        @Override
        public String toString() {
            return "StatPoint [apiLevel=" + apiLevel + ", percentage=" + percentage + "]";
        }
    }

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();


    @Override
    protected String getCommand() {
        return "!android stats";
    }

    @Override
    protected void handleChannelMessage(final Connection connection, final String channel, String fromNickname, String text, List<String> textAsList,
            Message message, HandlerContext handlerContext) throws Exception {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.send(Command.PRIVMSG, channel, getStats());
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }

    private static String getStats() {
        String html = HttpRequest.get(URL_HTML).body();
        String start = "var VERSION_DATA =";
        String end = ";";

        html = html.substring(html.indexOf(start) + start.length());
        html = html.substring(0, html.indexOf(end));
        if (Config.LOGD) Log.d(TAG, html);

        JSONArray jsonArray = new JSONArray(html);
        if (Config.LOGD) Log.d(TAG, "jsonArray=" + jsonArray);

        JSONArray data = jsonArray.getJSONObject(0).getJSONArray("data");
        if (Config.LOGD) Log.d(TAG, "data=" + data);

        int len = data.length();
        List<StatPoint> statPoints = new ArrayList<StatPoint>(len);
        for (int i = 0; i < len; i++) {
            StatPoint statPoint = StatPoint.fromJson(data.getJSONObject(i));
            statPoints.add(statPoint);
        }

        // Sort list
        Collections.sort(statPoints);
        if (Config.LOGD) Log.d(TAG, "statPoints=" + statPoints);

        StringBuilder res = new StringBuilder();
        float total = 0;
        for (StatPoint statPoint : statPoints) {
            total += statPoint.percentage;
            statPoint.cumulativePercentage = total;
        }

        // Reverse list
        Collections.reverse(statPoints);
        int i = 0;
        for (StatPoint statPoint : statPoints) {
            res.append(String.format("%1$d=%2$.0f%%", statPoint.apiLevel, statPoint.cumulativePercentage));
            if (i < len - 1) {
                res.append(" ");
            }
            i++;
        }


        if (Config.LOGD) Log.d(TAG, "res=" + res);

        return res.toString();
    }

    public static void main(String[] av) {
        getStats();
    }
}
