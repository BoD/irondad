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
package org.jraf.irondad.handler.quote;

import java.util.ArrayList;
import java.util.List;

import org.jraf.irondad.handler.HandlerConfig;
import org.json.JSONArray;

public class QuoteHandlerConfig extends HandlerConfig {
    private static final String DB_PATH = "DB_PATH";
    private static final String BLACK_LIST = "BLACK_LIST";

    public String getDbPath() {
        return getString(DB_PATH);
    }

    public void setDbPath(String dbPath) {
        put(DB_PATH, dbPath);
    }

    public void setBlackList(JSONArray blackList) {
        put(BLACK_LIST, blackList);
    }

    public List<String> getBlackList() {
        JSONArray blackList = getJSONArray(BLACK_LIST);
        ArrayList<String> res = new ArrayList<String>();
        int len = blackList.length();
        for (int i = 0; i < len; i++) {
            res.add(blackList.getString(i));
        }
        return res;
    }
}