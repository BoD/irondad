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
package org.jraf.irondad.handler.bitcoin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

public class BitcoinHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + BitcoinHandler.class.getSimpleName();

    private static final String URL_API = "http://api.bitcoincharts.com/v1/markets.json";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    protected String getCommand() {
        return "!btc";
    }

    @Override
    public void init(ClientConfig clientConfig) {}

    @Override
    public void handleChannelMessage(final Connection connection, final String channel, final String fromNickname, String text, List<String> textAsList,
            Message message, HandlerContext handlerContext) throws Exception {
        // Special case for djis
        if ("djis".equalsIgnoreCase(fromNickname)) {
            connection.send(Command.PRIVMSG, channel, String.format("$%1$1.2f", Math.random() * 299 + 200));
            return;
        }

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonStr = HttpRequest.get(URL_API).body();
                    if (jsonStr == null || jsonStr.length() == 0) {
                        // Try again once, sometimes we get an empty string
                        jsonStr = HttpRequest.get(URL_API).body();
                    }
                    JSONArray mainObject = new JSONArray(jsonStr);
                    int len = mainObject.length();
                    for (int i = 0; i < len; i++) {
                        JSONObject dataObject = mainObject.getJSONObject(i);
                        if ("bitstampUSD".equals(dataObject.getString("symbol"))) {
                            double avg = dataObject.getDouble("avg");
                            connection.send(Command.PRIVMSG, channel, String.format("$%1$1.2f", avg));
                            break;
                        }
                    }
                } catch (HttpRequestException e) {
                    Log.w(TAG, "handleMessage Could not get " + URL_API, e);
                } catch (JSONException e) {
                    Log.w(TAG, "handleMessage Could not parse json", e);
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }
}
