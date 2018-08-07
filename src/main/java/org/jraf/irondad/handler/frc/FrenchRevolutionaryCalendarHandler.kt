/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2017 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.irondad.handler.frc

import ca.rmen.lfrc.FrenchRevolutionaryCalendar
import org.jraf.irondad.Config
import org.jraf.irondad.Constants
import org.jraf.irondad.handler.CommandHandler
import org.jraf.irondad.handler.HandlerContext
import org.jraf.irondad.protocol.Command
import org.jraf.irondad.protocol.Connection
import org.jraf.irondad.protocol.Message
import org.jraf.irondad.util.Log
import java.io.IOException
import java.util.GregorianCalendar
import java.util.Locale

class FrenchRevolutionaryCalendarHandler : CommandHandler() {

    override fun getCommand() = "!frc"

    @Throws(Exception::class)
    override fun handleChannelMessage(
        connection: Connection, channel: String, fromNickname: String, text: String, textAsList: List<String>,
        message: Message, handlerContext: HandlerContext
    ) {
        if (Config.LOGD) Log.d(TAG, "handleChannelMessage")
        val frcCalendar = FrenchRevolutionaryCalendar(Locale.FRENCH, FrenchRevolutionaryCalendar.CalculationMethod.ROMME)
        val frcDate = frcCalendar.getDate(GregorianCalendar.getInstance() as GregorianCalendar)
        val frenchDate =
            "Le ${frcDate.weekdayName} ${frcDate.dayOfMonth} ${frcDate.monthName} de l'an ${frcDate.year}. (${frcDate.objectTypeName} du jour : ${frcDate.objectOfTheDay})"

        try {
            connection.send(Command.PRIVMSG, channel, frenchDate)
        } catch (e: IOException) {
            Log.e(TAG, "handleMessage Could not send to connection", e)
        }

    }

    companion object {
        private val TAG = Constants.TAG + FrenchRevolutionaryCalendarHandler::class.java.simpleName
    }
}
