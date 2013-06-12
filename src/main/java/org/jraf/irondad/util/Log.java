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
package org.jraf.irondad.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");

    public static void w(String tag, String msg, Throwable t) {
        System.out.print(DATE_FORMAT.format(new Date()) + " W " + tag + " " + msg + "\n" + getStackTraceString(t));
    }

    public static void w(String tag, String msg) {
        System.out.print(DATE_FORMAT.format(new Date()) + " W " + tag + " " + msg + "\n");
    }

    public static void e(String tag, String msg, Throwable t) {
        System.out.print(DATE_FORMAT.format(new Date()) + " E " + tag + " " + msg + "\n" + getStackTraceString(t));
    }

    public static void e(String tag, String msg) {
        System.out.print(DATE_FORMAT.format(new Date()) + " E " + tag + " " + msg + "\n");
    }

    public static void d(String tag, String msg, Throwable t) {
        System.out.print(DATE_FORMAT.format(new Date()) + " D " + tag + " " + msg + "\n" + getStackTraceString(t));
    }

    public static void d(String tag, String msg) {
        System.out.print(DATE_FORMAT.format(new Date()) + " D " + tag + " " + msg + "\n");
    }

    public static void i(String tag, String msg, Throwable t) {
        System.out.print(DATE_FORMAT.format(new Date()) + " I " + tag + " " + msg + "\n" + getStackTraceString(t));
    }

    public static void i(String tag, String msg) {
        System.out.print(DATE_FORMAT.format(new Date()) + " I " + tag + " " + msg + "\n");
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }
}
