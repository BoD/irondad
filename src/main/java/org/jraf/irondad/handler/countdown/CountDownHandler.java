/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.irondad.handler.countdown;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.BaseHandler;
import org.jraf.irondad.handler.HandlerContext;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

public class CountDownHandler extends BaseHandler {
    private static final String TAG = Constants.TAG + CountDownHandler.class.getSimpleName();

    private String getDate(List<String> textAsList, HandlerContext handlerContext) {
        CountDownHandlerConfig handlerConfig = (CountDownHandlerConfig) handlerContext.getHandlerConfig();
        String command = textAsList.get(0);
        String dateStr = handlerConfig.get(command);

        return dateStr;
    }

    @Override
    public boolean isMessageHandled(String channel, String fromNickname, String text, List<String> textAsList, Message message, HandlerContext handlerContext) {
        String dateStr = getDate(textAsList, handlerContext);
        return dateStr != null;
    }

    @Override
    protected void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message,
            HandlerContext handlerContext) throws Exception {
        String eventDateStr = getDate(textAsList, handlerContext);
        String reply = getReply(eventDateStr);
        connection.send(Command.PRIVMSG, new String[] { channel, reply });
    }

    private String getReply(String eventDateStr) {
        DateTime eventDateTime = DateTime.parse(eventDateStr);
        DateTime nowDateTime = DateTime.now();

        String res = null;

        int nbDays = Days.daysBetween(nowDateTime.toLocalDateTime().toDateTime().withTimeAtStartOfDay(),
                eventDateTime.toLocalDateTime().toDateTime().withTimeAtStartOfDay()).getDays();
        if (nbDays > 2) {
            res = "Dans " + nbDays + " jours !";
        } else if (nbDays == 2) {
            res = "APRÉS-DEMAIN !!!";
        } else if (nbDays == 1) {
            res = "DEMAIN !!!!!!";
        } else if (nbDays == 0) {
            int nbHours = Hours.hoursBetween(nowDateTime, eventDateTime).getHours();
            if (nbHours > 0) {
                if (nbHours == 1) res = "Dans " + nbHours + " heure (et quelques) !!";
                else
                    res = "Dans " + nbHours + " heures (et quelques) !";
            } else if (nbHours < 0) {
                res = "C'est en ce moment.";
            } else {
                int nbMinutes = Minutes.minutesBetween(nowDateTime, eventDateTime).getMinutes();
                if (nbMinutes > 0) {
                    if (nbMinutes == 1) res = "Dans 1 minute !!!";
                    else
                        res = "Dans " + nbMinutes + " minutes !!";
                } else if (nbMinutes < 0) {
                    res = "C'est en ce moment même !!!!!";
                } else {
                    int nbSeconds = Seconds.secondsBetween(nowDateTime, eventDateTime).getSeconds();
                    if (nbSeconds > 0) res = "Dans " + nbSeconds + " secondes !!!!";
                    else if (nbSeconds < 0) res = "Ça commence !!!!!";
                    else
                        res = "C'est commencé !!!!!";
                }
            }
        } else if (nbDays == -1) {
            res = "C'est en ce moment...";
        } else {
            res = "C'est fini :(";
        }
        return res;
    }

    public static void main(String[] av) {
        // Just testing...
        Log.d(TAG, new CountDownHandler().getReply("2014-06-25T09:00:00-0700"));
        Log.d(TAG, new CountDownHandler().getReply("2014-06-25T18:00:00+0200"));
        Log.d(TAG, new CountDownHandler().getReply("2014-06-25T09:00:00-0700"));
        Log.d(TAG, new CountDownHandler().getReply("2014-06-25T15:00:00+0200"));
        Log.d(TAG, new CountDownHandler().getReply("2014-06-25T14:00:00+0200"));
    }
}
