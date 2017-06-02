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
package org.jraf.irondad.handler.wikipedia;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class WikipediaHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + WikipediaHandler.class.getSimpleName();

    private static final String COMMAND = "!wiki";
    private static final int MAX_LINE_LEN = 420;

    // Wikipedia url parts
    private static final String URL_HTML = ".wikipedia.org/w/api.php?action=opensearch&search=";
    private static final String DEFAULT_LOCALE = "en";
    private static final String END_URL_HTML = "&limit=10&namespace=0&format=json";

    // Google search constants
    private static final String APPLICATION_NAME = "BoD-irondad/" + Constants.VERSION_NAME;
    private static final String SEARCH_PREFIX = "wikipedia ";
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final int RESULT_SIZE = 1;

    // Possible replies
    private static final String REPLY_USAGE = "Usage: " + COMMAND + " keywords | " + COMMAND + " [fr/it/...] keywords | " + COMMAND + " help";
    private static final String REPLY_NO_MATCH = "No match";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    protected String getCommand() {
        return COMMAND;
    }

    @Override
    protected void handleChannelMessage(final Connection connection, final String channel, String fromNickname, String text, List<String> textAsList,
                                        Message message, final HandlerContext handlerContext) throws Exception {
        if (Config.LOGD) Log.d(TAG, "handleChannelMessage");
        String param = "";

        final String locale;

        final boolean isUsage;
        if (textAsList.size() == 1 || textAsList.get(1).equals("help")) {
            // Usage
            isUsage = true;
            locale = DEFAULT_LOCALE;
        } else {
            String firstParam = textAsList.get(1);
            int startIndex = 1;
            if (firstParam.startsWith("[") && firstParam.endsWith("]")) {
                if (textAsList.size() == 2) {
                    // Locale argument, but no search terms
                    isUsage = true;
                    locale = DEFAULT_LOCALE;
                } else {
                    // Search with locale argument
                    isUsage = false;
                    locale = firstParam.substring(1, firstParam.length() - 1);
                    startIndex = 2;
                }
            } else {
                // Search with default locale
                isUsage = false;
                locale = DEFAULT_LOCALE;
            }

            for (int i = startIndex; i < textAsList.size(); i++) {
                if (i != startIndex) {
                    param += " ";
                }
                param += textAsList.get(i);
            }
        }

        param = URLEncoder.encode(param, "UTF-8");

        final String finalParam = param;
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isUsage) {
                        connection.send(Command.PRIVMSG, channel, REPLY_USAGE);
                        return;
                    }

                    // First we query Google to get the right keywords
                    String keywords = queryGoogle(handlerContext, finalParam);

                    // No keywords => no match
                    if (keywords == null) {
                        connection.send(Command.PRIVMSG, channel, REPLY_NO_MATCH);
                        return;
                    }

                    keywords = URLEncoder.encode(keywords, "UTF-8");
                    connection.send(Command.PRIVMSG, channel, getResult(keywords, locale));
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }

    private String queryGoogle(HandlerContext handlerContext, String searchTerms) throws IOException {
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

    private static String getResult(String param, String locale) {
        // Reconstruct wikipedia API url with the locale and keywords
        String wikiUrl = "https://" + locale + URL_HTML + param + END_URL_HTML;
        if (Config.LOGD) Log.d(TAG, wikiUrl);
        String json = HttpRequest.get(wikiUrl).body();
        if (Config.LOGD) Log.d(TAG, json);

        JSONArray result = new JSONArray(json);

        String resultStr = result.getJSONArray(2).getString(0);
        String url = result.getJSONArray(3).getString(0);

        // No result no url => no match
        if ((resultStr == null || resultStr.equals("")) && (url == null || url.equals(""))) {
            return REPLY_NO_MATCH;
        }

        if ((resultStr == null || resultStr.equals(""))) {
            resultStr = result.getJSONArray(1).getString(0);
        }

        // Let's truncate (if needed) to fill a whole IRC line (420 chars) with the text and the URL
        // +1 is for the space between the text and the url
        boolean tooLong = false;
        if (resultStr.getBytes().length + (url.getBytes().length + 1) > MAX_LINE_LEN) {
            tooLong = true;
        }

        // +4 is for the space and the … char
        if (tooLong) {
            while (resultStr.getBytes().length + (url.getBytes().length + 4) > MAX_LINE_LEN) {
                resultStr = resultStr.substring(0, resultStr.length() - 1);
            }
        }

        if (tooLong) {
            resultStr += "… ";
        } else {
            resultStr += " ";
        }

        return resultStr + url;
    }

    private static String getResourceNameFromUrl(String url) {
        int slashIdx = url.lastIndexOf('/');
        url = url.substring(slashIdx + 1);
        try {
            url = URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException ignored) {}
        url = url.replace('_', ' ');
        url = url.toLowerCase(Locale.US);
        return url;
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
}
