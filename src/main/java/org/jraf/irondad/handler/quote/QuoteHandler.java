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

import org.apache.commons.lang3.StringUtils;
import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.CommandHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.handler.quote.DbManager.Quote;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class QuoteHandler extends CommandHandler {
    private static final String TAG = Constants.TAG + QuoteHandler.class.getSimpleName();

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final long MIN_DELAY_BETWEEN_QUOTES = 2 * 60 * 1000;

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
    protected void handlePrivmsgMessage(Connection connection, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        if (textAsList.size() != 5) return;
        if (!textAsList.get(1).equals("rm")) return;
        String channel = textAsList.get(2);
        long id;
        try {
            id = Long.valueOf(textAsList.get(3));
        } catch (Exception e) {
            connection.send(Command.PRIVMSG, fromNickname, "0");
            return;
        }
        String password = textAsList.get(4);
        if (!mClientConfig.getAdminPassword().equals(password)) return;
        DbManager dbManager = mDbManagers.get(channel);
        if (dbManager == null) {
            connection.send(Command.PRIVMSG, fromNickname, "0");
            return;
        }
        int res = dbManager.delete(id);
        connection.send(Command.PRIVMSG, fromNickname, String.valueOf(res));
    }

    @Override
    public void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
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
        } else if (textAsList.size() == 2) {
            if (textAsList.get(1).startsWith("#")) {
                // Find quote by id
                String id = textAsList.get(1);
                if (id.length() < 2) {
                    displayText = "Could not find this quote.";
                } else {
                    id = id.substring(1);
                    if (!StringUtils.isNumeric(id)) {
                        displayText = "Could not find this quote.";
                    } else {
                        long idLong = Long.valueOf(id);
                        Quote quote = dbManager.getQuote(idLong);
                        if (quote == null) {
                            displayText = "Could not find this quote.";
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                            displayText = "\"" + quote.text + "\" - " + sdf.format(quote.date) + " (#" + quote.id + ")";
                        }
                    }
                }
            } else {
                // Find quote by text search
                String query = textAsList.get(1);
                Quote quote = dbManager.getQuote(query);
                if (quote == null) {
                    displayText = "Could not find this quote.";
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                    displayText = "\"" + quote.text + "\" - " + sdf.format(quote.date) + " (#" + quote.id + ")";
                }
            }
        } else {
            // New quote
            long latestQuoteDate = dbManager.getLatestQuoteDate(channel);
            if (System.currentTimeMillis() - latestQuoteDate < MIN_DELAY_BETWEEN_QUOTES) {
                // Throttled
                displayText = "Try again in a bit.";
            } else {
                // Check for black list
                String nameUserHost = message.origin.toFormattedString();
                boolean blackListed = false;
                QuoteHandlerConfig quoteHandlerConfig = (QuoteHandlerConfig) handlerContext.getHandlerConfig();
                for (String item : quoteHandlerConfig.getBlackList()) {
                    if (nameUserHost.matches(item)) {
                        if (Config.LOGD) Log.d(TAG, "handleChannelMessage " + nameUserHost + " matches " + item + " - blacklisted");
                        blackListed = true;
                        break;
                    }
                }
                if (blackListed) {
                    displayText = "Did not add quote.";
                } else {
                    // Add the new quote
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
            }
        }
        connection.send(Command.PRIVMSG, channel, displayText);
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
