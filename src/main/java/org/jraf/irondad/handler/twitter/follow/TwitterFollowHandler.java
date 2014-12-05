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
package org.jraf.irondad.handler.twitter.follow;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.model.Url;

public class TwitterFollowHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + TwitterFollowHandler.class.getSimpleName();

    public static final Comparator<Status> STATUS_COMPARATOR = new Comparator<Status>() {
        @Override
        public int compare(Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    };

    @Override
    public void init(HandlerContext handlerContext) throws Exception {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new CheckForNewTweetsRunnable(handlerContext), 3, 3, TimeUnit.MINUTES);
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        return false;
    }

    private static Twitter getTwitter(HandlerContext handlerContext) {
        Twitter res = (Twitter) handlerContext.get("twitter");
        if (res == null) {
            TwitterFollowHandlerConfig twitterFollowHandlerConfig = (TwitterFollowHandlerConfig) handlerContext.getHandlerConfig();
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setDebugEnabled(true).setOAuthConsumerKey(twitterFollowHandlerConfig.getOauthConsumerKey());
            configurationBuilder.setOAuthConsumerSecret(twitterFollowHandlerConfig.getOauthConsumerSecret());
            configurationBuilder.setOAuthAccessToken(twitterFollowHandlerConfig.getOauthAccessToken());
            configurationBuilder.setOAuthAccessTokenSecret(twitterFollowHandlerConfig.getOauthAccessTokenSecret());
            TwitterFactory twitterFactory = new TwitterFactory(configurationBuilder.build());
            res = twitterFactory.getInstance();

            handlerContext.put("twitter", res);
        }
        return res;
    }

    public static class CheckForNewTweetsRunnable implements Runnable {
        private final HandlerContext mHandlerContext;
        private Status mLatestStatus;


        public CheckForNewTweetsRunnable(HandlerContext handlerContext) {
            mHandlerContext = handlerContext;
        }

        @Override
        public void run() {
            try {
                if (Config.LOGD) Log.d(TAG, "run Checking for new tweets");
                ResponseList<Status> statusList = getTwitter(mHandlerContext).getHomeTimeline(new Paging(1, 1));
                if (statusList.isEmpty()) return;

                Collections.sort(statusList, STATUS_COMPARATOR);
                Status latestStatus = statusList.get(statusList.size() - 1);

                if (mLatestStatus == null) {
                    // First time
                    mLatestStatus = latestStatus;
                    return;
                }

                long id = latestStatus.getId();
                if (mLatestStatus.getId() == id) {
                    // No change
                    if (Config.LOGD) Log.d(TAG, "run No new tweets");
                    return;
                }

                // New tweet
                mLatestStatus = latestStatus;

                if (Config.LOGD) Log.d(TAG, "run New tweet id=" + id);

                String screenName = latestStatus.getUser().getScreenName();
                //                String tweetUrl = "http://twitter.com/" + screenName + "/status/" + id;
                //                String shortTweetUrl = shortenUrl(tweetUrl);
                String text = "@" + screenName + " " + latestStatus.getText();
                mHandlerContext.getConnection().send(Command.PRIVMSG, mHandlerContext.getChannelName(), text);

            } catch (Exception e) {
                Log.w(TAG, "run", e);
            }
        }
    }

    private static Urlshortener newUrlshortener() throws Exception {
        return new Urlshortener.Builder(GoogleNetHttpTransport.newTrustedTransport(), new JacksonFactory(), null).build();
    }

    private static String shortenUrl(String longUrlStr) throws Exception {
        Url longUrl = new Url();
        longUrl.setLongUrl(longUrlStr);
        Url shortUrl = newUrlshortener().url().insert(longUrl).execute();
        return shortUrl.getId();
    }

    public static void main(String[] av) throws Exception {
        if (Config.LOGD) Log.d(TAG, "main " + shortenUrl("http://JRAF.org"));
    }
}
