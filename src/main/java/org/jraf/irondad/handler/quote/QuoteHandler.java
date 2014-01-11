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
import java.util.List;
import java.util.Locale;

import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.Handler;
import org.jraf.irondad.handler.quote.DbManager.Quote;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;

public class QuoteHandler implements Handler {
    private static final String TAG = Constants.TAG + QuoteHandler.class.getSimpleName();

    private static final String COMMAND = "!quote";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final String PREFIX = QuoteHandler.class.getName() + ".";
    public static final String CONFIG_PATH_DB = PREFIX + "CONFIG_PATH_DB";

    private DbManager mDbManager;

    @Override
    public void init(ClientConfig clientConfig) {
        mDbManager = new DbManager(clientConfig.getExtraConfig(CONFIG_PATH_DB));
    }

    @Override
    public boolean handleMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message)
            throws Exception {
        if (!text.startsWith(COMMAND)) return false;

        if (channel == null) {
            // Private messages
            if (textAsList.size() != 4) return false;
            if (!textAsList.get(1).equals("rm")) return false;
            long id;
            try {
                id = Long.valueOf(textAsList.get(2));
            } catch (Exception e) {
                return false;
            }
            if (!connection.getClient().getClientConfig().getAdminPassword().equals(textAsList.get(3))) return false;
            int res = mDbManager.delete(id);
            connection.send(Command.PRIVMSG, fromNickname, String.valueOf(res));
            return true;
        }

        String displayText;
        if (text.trim().equals(COMMAND)) {
            // Random
            Quote randomQuote = mDbManager.getRandom(channel);
            if (randomQuote == null) {
                displayText = "No quotes currently in db.";
            } else {
                displayText = "\"" + randomQuote.text + "\" - " + DATE_FORMAT.format(randomQuote.date) + " (#" + randomQuote.id + ")";
            }
        } else {
            long id = mDbManager.insert(channel, message.origin.toFormattedString(), text.substring(text.indexOf(' ') + 1));
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
}
