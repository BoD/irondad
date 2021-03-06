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
package org.jraf.irondad.handler.opengraph;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class OpengraphHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + OpengraphHandler.class.getSimpleName();

    /**
     * Good characters for Internationalized Resource Identifiers (IRI).
     * This comprises most common used Unicode characters allowed in IRI
     * as detailed in RFC 3987.
     * Specifically, those two byte Unicode characters are not included.
     */
    private static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    /**
     * Regular expression to match all IANA top-level domains for WEB_URL.
     * List accurate as of 2010/02/05.  List taken from:
     * http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     * This pattern is auto-generated by frameworks/base/common/tools/make-iana-tld-pattern.py
     */
    private static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL = "(?:"
            + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
            + "|(?:biz|b[abdefghijmnorstvwyz])"
            + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
            + "|d[ejkmoz]"
            + "|(?:edu|e[cegrstu])"
            + "|f[ijkmor]"
            + "|(?:gov|g[abdefghilmnpqrstuwy])"
            + "|h[kmnrtu]"
            + "|(?:info|int|i[delmnoqrst])"
            + "|(?:jobs|j[emop])"
            + "|k[eghimnprwyz]"
            + "|l[abcikrstuvy]"
            + "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
            + "|(?:name|net|n[acefgilopruz])"
            + "|(?:org|om)"
            + "|(?:pro|p[aefghklmnrstwy])"
            + "|qa"
            + "|r[eosuw]"
            + "|s[abcdeghijklmnortuvyz]"
            + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
            + "|u[agksyz]"
            + "|v[aceginu]"
            + "|w[fs]"
            +
            "|(?:xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-zckzah)"
            + "|y[etu]"
            + "|z[amw]))";

    /**
     * Regular expression pattern to match most part of RFC 3987
     * Internationalized URLs, aka IRIs.  Commonly used Unicode characters are
     * added.
     */
    private static final Pattern WEB_URL = Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)", Pattern.DOTALL); // and finally, a word boundary or end of
    // input.  This is to stop foo.sure from
    // matching as foo.su


    private static final int MAX_URL = 3;

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        return WEB_URL.matcher(text).find();
    }

    @Override
    public void handleChannelMessage(final Connection connection, final String channel, final String fromNickname, String text, List<String> textAsList,
                                     Message message, HandlerContext handlerContext) throws Exception {
        Matcher matcher = WEB_URL.matcher(text);

        int i = 0;
        while (matcher.find()) {
            if (i++ >= MAX_URL) {
                break;
            }
            final String url = text.substring(matcher.start(), matcher.end());
            Log.i(TAG, url);

            mThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String html = HttpRequest.get(url).body();

                        Document doc = Jsoup.parse(html, url);
                        StringBuilder sb = new StringBuilder();
                        Elements metaOgTitle = doc.select("meta[property=og:title]");
                        if (!metaOgTitle.isEmpty()) {
                            sb.append(metaOgTitle.attr("content"));
                        } else {
                            sb.append(doc.title());
                        }

                        Elements metaOgDescription = doc.select("meta[property=og:description]");
                        if (!metaOgDescription.isEmpty()) {
                            sb.append(" - ").append(metaOgDescription.attr("content"));
                        }

                        String imageUrl = null;
                        Elements metaOgImage = doc.select("meta[property=og:image]");
                        if (!metaOgImage.isEmpty()) {
                            imageUrl = metaOgImage.attr("content");
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            sb.append(" - ").append(imageUrl);
                        }

                        connection.send(Command.PRIVMSG, channel, sb.toString().replace("\n", "").replace("\r", ""));
                    } catch (HttpRequestException e) {
                        Log.w(TAG, "handleMessage Could not get " + url, e);
                    } catch (IOException e) {
                        Log.e(TAG, "handleMessage Could not send to connection", e);
                    }
                }
            });
        }
    }
}
