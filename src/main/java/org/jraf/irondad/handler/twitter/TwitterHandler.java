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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + TwitterHandler.class.getSimpleName();

    private static final Pattern PATTERN_TWEET_ID = Pattern.compile("(.*twitter\\.com.*status/)([0-9]+)[^0-9]*.*", Pattern.CASE_INSENSITIVE);
    private static final int PATTERN_TWEET_ID_GROUP = 2;

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    @Override
    public void init(ClientConfig clientConfig) {}

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        String tweetId = getTweetId(text);
        return tweetId != null;
    }

    @Override
    public void handleChannelMessage(final Connection connection, final String channel, final String fromNickname, String text, List<String> textAsList,
            Message message, final HandlerContext handlerContext) throws Exception {
        final String tweetId = getTweetId(text);
        if (tweetId == null) {
            // Text doesn't contain a twitter link: ignore
            return;
        }
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Status status = getTwitter(handlerContext).showStatus(Long.valueOf(tweetId));
                    String tweetText = status.getText();
                    connection.send(Command.PRIVMSG, channel, tweetText);
                } catch (Exception e) {
                    Log.w(TAG, "handleMessage" + tweetId, e);
                }
            }
        });
    }

    private static String getTweetId(String text) {
        Matcher matcher = PATTERN_TWEET_ID.matcher(text);
        if (!matcher.matches()) return null;
        return matcher.group(PATTERN_TWEET_ID_GROUP);
    }


    private Twitter getTwitter(HandlerContext handlerContext) {
        Twitter res = (Twitter) handlerContext.get("twitter");
        if (res == null) {
            TwitterHandlerConfig twitterHandlerConfig = (TwitterHandlerConfig) handlerContext.getHandlerConfig();
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setDebugEnabled(true).setOAuthConsumerKey(twitterHandlerConfig.getOauthConsumerKey());
            configurationBuilder.setOAuthConsumerSecret(twitterHandlerConfig.getOauthConsumerSecret());
            configurationBuilder.setOAuthAccessToken(twitterHandlerConfig.getOauthAccessToken());
            configurationBuilder.setOAuthAccessTokenSecret(twitterHandlerConfig.getOauthAccessTokenSecret());
            TwitterFactory twitterFactory = new TwitterFactory(configurationBuilder.build());
            res = twitterFactory.getInstance();

            handlerContext.put("twitter", res);
        }
        return res;
    }

}
