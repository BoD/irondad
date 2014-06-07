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

import org.jraf.irondad.handler.HandlerConfig;

public class TwitterFollowHandlerConfig extends HandlerConfig {
    public static final String OAUTH_CONSUMER_KEY = "OAUTH_CONSUMER_KEY";
    public static final String OAUTH_CONSUMER_SECRET = "OAUTH_CONSUMER_SECRET";
    public static final String OAUTH_ACCESS_TOKEN = "OAUTH_ACCESS_TOKEN";
    public static final String OAUTH_ACCESS_TOKEN_SECRET = "OAUTH_ACCESS_TOKEN_SECRET";

    public String getOauthConsumerKey() {
        return getString(OAUTH_CONSUMER_KEY);
    }

    public void setOauthConsumerKey(String oauthConsumerKey) {
        put(OAUTH_CONSUMER_KEY, oauthConsumerKey);
    }

    public String getOauthConsumerSecret() {
        return getString(OAUTH_CONSUMER_SECRET);
    }

    public void setOauthConsumerSecret(String oauthConsumerSecret) {
        put(OAUTH_CONSUMER_SECRET, oauthConsumerSecret);
    }

    public String getOauthAccessToken() {
        return getString(OAUTH_ACCESS_TOKEN);
    }

    public void setOauthAccessToken(String oauthAccessToken) {
        put(OAUTH_ACCESS_TOKEN, oauthAccessToken);
    }

    public String getOauthAccessTokenSecret() {
        return getString(OAUTH_ACCESS_TOKEN_SECRET);
    }

    public void setOauthAccessTokenSecret(String oauthAccessTokenSecret) {
        put(OAUTH_ACCESS_TOKEN_SECRET, oauthAccessTokenSecret);
    }
}