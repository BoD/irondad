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
package org.jraf.irondad.handler.googlegif;

import java.io.IOException;
import java.util.HashMap;
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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

public class GoogleGifHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + GoogleGifHandler.class.getSimpleName();

    private static final String APPLICATION_NAME = "BoD-irondad/" + Constants.VERSION_NAME;


    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final int RESULT_SIZE = 10;

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    private List<Result> mSearchResults;
    private final HashMap<String, Integer> mSearchIdxMap = new HashMap<>();


    @Override
    protected String getCommand() {
        return "!gif";
    }

    @Override
    protected void handleChannelMessage(final Connection connection, final String channel, String fromNickname, final String text,
            final List<String> textAsList, Message message, final HandlerContext handlerContext) throws Exception {
        final String key = ((GoogleGifHandlerConfig) handlerContext.getHandlerConfig()).getKey();
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String result;
                    if (textAsList.size() == 1) {
                        // No parameters: search for Android
                        result = getGoogleQueryResult(connection, handlerContext, "animated android");
                    } else {
                        // "Translate" api
                        String searchTerms = text.substring(getCommand().length() + 1);
                        result = getGoogleQueryResult(connection, handlerContext, "animated " + searchTerms);

                    }
                    connection.send(Command.PRIVMSG, channel, result);
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }


        });
    }

    private String getGoogleQueryResult(final Connection connection, final HandlerContext handlerContext, String searchTerms) throws IOException {
        Integer searchIdx = mSearchIdxMap.get(searchTerms);
        if (searchIdx == null) {
            searchIdx = -1;
        }
        searchIdx++;
        mSearchIdxMap.put(searchTerms, searchIdx);
        String result;
        long searchResultCount = queryGoogle(handlerContext, connection, searchTerms);
        if (searchResultCount == 0) {
            result = "This search has no results!  Try another one.";
        } else {
            result = mSearchResults.get(searchIdx % mSearchResults.size()).getLink();
            result = result.replaceAll("200_s\\.gif", "giphy.gif");
        }
        return result;
    }

    private long queryGoogle(HandlerContext handlerContext, Connection connection, String searchTerms) throws IOException {
        if (Config.LOGD) Log.d(TAG, "queryGoogle searchTerms=" + searchTerms);
        Customsearch customsearch = getCustomsearch(handlerContext);
        Customsearch.Cse.List list = customsearch.cse().list(searchTerms);
        String cx = ((GoogleGifHandlerConfig) handlerContext.getHandlerConfig()).getCx();
        list.setCx(cx);
        list.setSearchType("image");
        list.setFileType("gif");
        list.setFields("items/link,searchInformation/totalResults");
        list.setNum((long) RESULT_SIZE);
        //        list.setStart((long) mGuessCount + 1);

        // Execute the query
        Search search = list.execute();

        mSearchResults = search.getItems();

        return search.getSearchInformation().getTotalResults();
    }

    private Customsearch getCustomsearch(HandlerContext handlerContext) {
        Customsearch res = (Customsearch) handlerContext.get("customsearch");
        if (res == null) {
            Customsearch.Builder customSearchBuilder;
            try {
                customSearchBuilder = new Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, null);
                customSearchBuilder.setApplicationName(APPLICATION_NAME);
                String key = ((GoogleGifHandlerConfig) handlerContext.getHandlerConfig()).getKey();
                customSearchBuilder.setCustomsearchRequestInitializer(new CustomsearchRequestInitializer(key));
                res = customSearchBuilder.build();

                handlerContext.put("customsearch", res);
            } catch (Exception e) {
                Log.e(TAG, "GoogleGifHandler Could not initialize! GoogleGifHandler will not work until this problem is resolved", e);
            }
        }
        return res;
    }
}
