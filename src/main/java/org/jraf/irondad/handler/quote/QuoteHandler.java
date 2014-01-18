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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.handler.quote.DbManager.Quote;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class QuoteHandler extends BaseHandler {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private Map<String, DbManager> mDbManagers = new HashMap<String, DbManager>();
    private ClientConfig mClientConfig;

    @Override
    protected String getCommand() {
        return "!quote";
    }

    @Override
    public void init(ClientConfig clientConfig) {
        mClientConfig = clientConfig;
    }

    @Override
    protected boolean handlePrivmsgMessage(Connection connection, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        if (textAsList.size() != 5) return true;
        if (!textAsList.get(1).equals("rm")) return true;
        String channel = textAsList.get(2);
        long id;
        try {
            id = Long.valueOf(textAsList.get(3));
        } catch (Exception e) {
            connection.send(Command.PRIVMSG, fromNickname, "0");
            return true;
        }
        String password = textAsList.get(4);
        if (!mClientConfig.getAdminPassword().equals(password)) return true;
        DbManager dbManager = mDbManagers.get(channel);
        if (dbManager == null) {
            connection.send(Command.PRIVMSG, fromNickname, "0");
            return true;
        }
        int res = dbManager.delete(id);
        connection.send(Command.PRIVMSG, fromNickname, String.valueOf(res));
        return true;
    }

    @Override
    public boolean handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        DbManager dbManager = getDbManager(channel, handlerContext);

        String displayText;
        if (textAsList.size() == 1) {
            // Random
            Quote randomQuote = dbManager.getRandom(channel);
            if (randomQuote == null) {
                displayText = "No quotes currently in db.";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                displayText = "\"" + randomQuote.text + "\" - " + sdf.format(randomQuote.date) + " (#" + randomQuote.id + ")";
            }
        } else {
            long id = dbManager.insert(channel, message.origin.toFormattedString(), text.substring(text.indexOf(' ') + 1));
            switch ((int) id) {
                case DbManager.ERR_SQL_PROBLEM:
                    displayText = "Could not add quote.";
                    break;

                case DbManager.ERR_QUOTE_ALREADY_EXISTS:
                    displayText = "This quote already exists!";
                    break;

                default:
                    displayText = "Quote #" + id + " added.";
                    break;
            }
        }
        connection.send(Command.PRIVMSG, channel, displayText);
        return true;
    }

    private DbManager getDbManager(String channel, HandlerContext handlerContext) {
        DbManager res = mDbManagers.get(channel);
        if (res == null) {
            QuoteHandlerConfig quoteHandlerConfig = (QuoteHandlerConfig) handlerContext.getHandlerConfig();
            res = new DbManager(quoteHandlerConfig.getDbPath());
            mDbManagers.put(channel, res);
        }
        return res;
    }
}
