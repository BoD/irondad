/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Nicolas Pomepuy
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
package org.jraf.irondad.handler.commitstrip;

import java.io.IOException;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.kevinsawicki.http.HttpRequest;

public class CommitstripHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + CommitstripHandler.class.getSimpleName();

    private static final String URL_HTML = "http://www.commitstrip.com/en/";

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    protected String getCommand() {
        return "!commitstrip";
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
                    connection.send(Command.PRIVMSG, channel, getUri(param));
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage Could not send to connection", e);
                }
            }
        });
    }

    private static String getUri(String param) {
        if (param.equals("help")) {
            return "Options: [random|help]";
                   
        }
        String suffix = "";
        if (param.equals("random")) {
            suffix = "random/";
            
            String html = HttpRequest.get(URL_HTML + suffix).body();
            
            return extractImgFromHtml(html);
        } 

        String html = HttpRequest.get(URL_HTML ).body();
        if (Config.LOGD) Log.d(TAG, html);
        
        
        //Find the first link
        Document doc = Jsoup.parse(html);
        Element excerpt = doc.select(".excerpt").first();
        if (excerpt == null) {
            return null;
        }

        Element a = excerpt.select("a").first();
        
        if(a == null) {
            return null;
        }
        
        String href = a.attr("href");
        if(href == null) {
            return null;
        }
        
        html = HttpRequest.get(href ).body();
        if (Config.LOGD) Log.d(TAG, html);
        
        return extractImgFromHtml(html);
    }
    
    private static String extractImgFromHtml(String html){
        if (Config.LOGD) Log.d(TAG, html);
        Document doc = Jsoup.parse(html);

        Element article = doc.select("article").first();
        if (article == null) {
            return null;
        }
        Element div = article.select("div.entry-content").first();
        if (div == null) {
            return null;
        }
        Element img = div.select("img").first();
        if (img == null) {
            return null;
        }
        
        String src = img.attr("src");
        return src;
        
    }

    public static void main(String[] av) {
        getUri("");
    }
}
