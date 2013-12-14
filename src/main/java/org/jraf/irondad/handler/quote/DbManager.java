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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.util.Log;

public class DbManager {
    private static final String TAG = Constants.TAG + DbManager.class.getSimpleName();

    //@formatter:off
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS quote (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "channel TEXT NOT NULL, " +
            "_date INTEGER NOT NULL, " +
            "added_by TEXT NOT NULL, " +
            "_text TEXT NOT NULL " +
            ")";
    
    private static final String SQL_INSERT = "INSERT INTO quote " +
    		"(channel, _date, added_by, _text)" +
    		" VALUES " +
    		"(?, ?, ?, ?)";

    private static final String SQL_COUNT = "SELECT " +
            "count(_id)" +
            " FROM " +
            "quote" +
            " WHERE " +
            "channel=?";
    
    private static final String SQL_SELECT_ALL = "SELECT " +
            "_id, _date, _text" +
            " FROM " +
            "quote" +
            " WHERE " +
            "channel=?" +
            " ORDER BY " +
            "_id";
    
    private static final String SQL_CHECK_QUOTE_EXISTS = "SELECT " +
            "count(_id)" +
            " FROM " +
            "quote" +
            " WHERE " +
            "channel=? and _text=?";
    
    private static final String SQL_DELETE = "DELETE " +
            " FROM " +
            "quote" +
            " WHERE " +
            "_id=?";
    //@formatter:on

    public static final int ERR_SQL_PROBLEM = -1;
    public static final int ERR_QUOTE_ALREADY_EXISTS = -2;

    private Connection mConnection;

    private ArrayList<Integer> mRandomOrder;
    private int mRandomIdx;

    public DbManager(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "DbManager Could not intialize jdbc driver", e);
        }

        boolean dbExists = new File(dbPath).exists();
        if (Config.LOGD) Log.d(TAG, "DbManager dbExists=" + dbExists);

        try {
            mConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            if (!dbExists) {
                Statement statement = mConnection.createStatement();
                statement.execute(SQL_CREATE_TABLE);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Could not initialize connection", e);
        }
    }

    public long insert(String channel, String addedBy, String text) {
        if (Config.LOGD) Log.d(TAG, "insert channel=" + channel + " addedBy=" + addedBy + " text=" + text);

        if (isExistingQuote(channel, text)) return ERR_QUOTE_ALREADY_EXISTS;

        PreparedStatement statement = null;
        try {
            statement = mConnection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, channel);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, addedBy);
            statement.setString(4, text);

            int rows = statement.executeUpdate();
            if (Config.LOGD) Log.d(TAG, "insert rows=" + rows);
            resetRandom();

            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                long res = resultSet.getLong(1);
                if (Config.LOGD) Log.d(TAG, "insert res=" + res);
                return res;
            }
        } catch (SQLException e) {
            Log.e(TAG, "insert Could not insert", e);
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
                Log.w(TAG, "isExistingQuote", e);
            }
        }
        return ERR_SQL_PROBLEM;
    }

    private int getCount(String channel) {
        if (Config.LOGD) Log.d(TAG, "getCount channel=" + channel);
        PreparedStatement statement = null;
        try {
            statement = mConnection.prepareStatement(SQL_COUNT);
            statement.setString(1, channel);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            int res = resultSet.getInt(1);
            if (Config.LOGD) Log.d(TAG, "getCount res=" + res);
            return res;
        } catch (SQLException e) {
            Log.e(TAG, "Could not get a random quote", e);
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
                Log.w(TAG, "isExistingQuote", e);
            }
        }
        return 0;
    }

    private boolean isExistingQuote(String channel, String text) {
        if (Config.LOGD) Log.d(TAG, "isExistingQuote");
        PreparedStatement statement = null;
        try {
            statement = mConnection.prepareStatement(SQL_CHECK_QUOTE_EXISTS);
            statement.setString(1, channel);
            statement.setString(2, text);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            if (Config.LOGD) Log.d(TAG, "isExistingQuote count=" + count);
            return count > 0;
        } catch (SQLException e) {
            Log.e(TAG, "Could not check if quote exists", e);
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
                Log.w(TAG, "isExistingQuote", e);
            }
        }
        return false;
    }


    public static class Quote {
        public long id;
        public Date date;
        public String text;

        @Override
        public String toString() {
            return "Quote [id=" + id + ", date=" + date + ", text=" + text + "]";
        }
    }

    public Quote getRandom(String channel) {
        if (Config.LOGD) Log.d(TAG, "getRandom channel=" + channel);

        int count = getCount(channel);
        if (count == 0) return null;
        PreparedStatement statement = null;
        try {
            statement = mConnection.prepareStatement(SQL_SELECT_ALL);
            statement.setString(1, channel);
            ResultSet resultSet = statement.executeQuery();
            int random = getRandom(count);
            if (Config.LOGD) Log.d(TAG, "getRandom random=" + random);
            for (int i = 0; i < random + 1; i++) {
                resultSet.next();
            }
            Quote res = new Quote();
            res.id = resultSet.getLong(1);
            res.date = new Date(resultSet.getLong(2));
            res.text = resultSet.getString(3);
            if (Config.LOGD) Log.d(TAG, "getRandom res=" + res);
            return res;
        } catch (SQLException e) {
            Log.e(TAG, "Could not get a random quote", e);
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
                Log.w(TAG, "isExistingQuote", e);
            }
        }
        return null;
    }

    public int delete(long id) {
        if (Config.LOGD) Log.d(TAG, "delete id=" + id);

        PreparedStatement statement = null;
        try {
            statement = mConnection.prepareStatement(SQL_DELETE);
            statement.setLong(1, id);

            int rows = statement.executeUpdate();
            if (Config.LOGD) Log.d(TAG, "delete rows=" + rows);
            resetRandom();
            return rows;
        } catch (SQLException e) {
            Log.e(TAG, "insert Could not insert", e);
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
                Log.w(TAG, "isExistingQuote", e);
            }
        }
        return ERR_SQL_PROBLEM;
    }

    private int getRandom(int count) {
        if (mRandomOrder == null) {
            mRandomOrder = new ArrayList<Integer>(count);
            for (int i = 0; i < count; i++) {
                mRandomOrder.add(i);
            }
            Collections.shuffle(mRandomOrder);
            mRandomIdx = 0;
        }

        int res = mRandomOrder.get(mRandomIdx);
        mRandomIdx++;
        if (mRandomIdx == count) resetRandom();
        return res;
    }

    private void resetRandom() {
        mRandomOrder = null;
    }
}
