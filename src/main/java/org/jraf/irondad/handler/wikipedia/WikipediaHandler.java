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
package org.jraf.irondad.handler.wikipedia;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jraf.dbpedia2sqlite.db.DatabaseManager;
import org.jraf.dbpedia2sqlite.db.Resource;
import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.ClientConfig;
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

public class WikipediaHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + WikipediaHandler.class.getSimpleName();

    private static final String APPLICATION_NAME = "BoD-irondad/" + Constants.VERSION_NAME;
    private static final String SEARCH_PREFIX = "wikipedia ";
    private static final String REPLY_NO_MATCH = "No match";

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final int RESULT_SIZE = 1;
    private static final int MAX_LINE_LEN = 450;

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    public String getCommand() {
        return "!wikipedia ";
    }

    @Override
    public void init(ClientConfig clientConfig) {}

    @Override
    public void handleMessage(final Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            final HandlerContext handlerContext) throws Exception {
        if (!text.trim().toLowerCase(Locale.getDefault()).startsWith(getCommand())) return;

        final String chanOrNick = channel == null ? fromNickname : channel;
        final String searchTerms = text.substring(getCommand().length());

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String resourceName = queryGoogle(handlerContext, connection, searchTerms);
                    if (Config.LOGD) Log.d(TAG, "resourceName=" + resourceName);
                    if (resourceName == null) {
                        connection.send(Command.PRIVMSG, chanOrNick, REPLY_NO_MATCH);
                        return;
                    }

                    DatabaseManager databaseManager = getDatabaseManager(handlerContext);

                    Resource resource = databaseManager.getFromName(resourceName);
                    if (resource == null) {
                        connection.send(Command.PRIVMSG, chanOrNick, REPLY_NO_MATCH);
                        return;
                    }

                    String line = resource._abstract;
                    if (line.length() >= MAX_LINE_LEN) {
                        line = line.substring(0, MAX_LINE_LEN - 5) + "(...)";
                    }
                    connection.send(Command.PRIVMSG, chanOrNick, line);
                } catch (Exception e) {
                    Log.w(TAG, "handleMessage", e);
                }
            }
        });
    }

    private String queryGoogle(HandlerContext handlerContext, Connection connection, String searchTerms) throws IOException {
        if (Config.LOGD) Log.d(TAG, "queryGoogle searchTerms=" + searchTerms);
        Customsearch customsearch = getCustomsearch(handlerContext);
        Customsearch.Cse.List list = customsearch.cse().list(SEARCH_PREFIX + searchTerms);
        String cx = ((WikipediaHandlerConfig) handlerContext.getHandlerConfig()).getCx();
        list.setCx(cx);
        list.setFields("items/link,searchInformation/totalResults");
        list.setNum((long) RESULT_SIZE);

        // Execute the query
        Search search = list.execute();

        Long totalResults = search.getSearchInformation().getTotalResults();
        if (totalResults == null || totalResults == 0) return null;

        List<Result> searchResults = search.getItems();
        String link = searchResults.get(0).getLink();
        link = getResourceNameFromUrl(link);
        return link;
    }


    protected DatabaseManager getDatabaseManager(HandlerContext handlerContext) {
        DatabaseManager res = (DatabaseManager) handlerContext.get("databaseManager");
        if (res == null) {
            res = new DatabaseManager(((WikipediaHandlerConfig) handlerContext.getHandlerConfig()).getDbPath());
            handlerContext.put("databaseManager", res);
        }
        return res;
    }


    private Customsearch getCustomsearch(HandlerContext handlerContext) {
        Customsearch res = (Customsearch) handlerContext.get("customsearch");
        if (res == null) {
            Customsearch.Builder customSearchBuilder;
            try {
                customSearchBuilder = new Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, null);
                customSearchBuilder.setApplicationName(APPLICATION_NAME);
                String key = ((WikipediaHandlerConfig) handlerContext.getHandlerConfig()).getKey();
                customSearchBuilder.setCustomsearchRequestInitializer(new CustomsearchRequestInitializer(key));
                res = customSearchBuilder.build();

                handlerContext.put("customsearch", res);
            } catch (Exception e) {
                Log.e(TAG, "WikipediaHandler Could not initialize! WikipediaHandler will not work until this problem is resolved", e);
            }
        }
        return res;
    }

    private static String getResourceNameFromUrl(String url) {
        int slashIdx = url.lastIndexOf('/');
        url = url.substring(slashIdx + 1);
        try {
            url = URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "getResourceNameFromUrl Could not urldecode " + url, e);
            return null;
        }
        url = url.replace('_', ' ');
        url = url.toLowerCase(Locale.US);
        return url;
    }
}
