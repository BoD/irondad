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
package org.jraf.irondad.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jraf.irondad.Constants;
import org.jraf.irondad.util.Log;

public class Connection {
    private static final String TAG = Constants.TAG + Connection.class.getSimpleName();
    private static final String CR_LF = "\r\n";

    private Client mClient;
    private final BufferedReader mBufferedReader;
    private final OutputStream mOutputStream;

    public Connection(Client client, Socket socket) throws IOException {
        mClient = client;
        mBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
        mOutputStream = socket.getOutputStream();
    }

    public void send(String line) throws IOException {
        Log.d(TAG, "SND " + line);
        mOutputStream.write((line + CR_LF).getBytes("utf-8"));
    }

    public void send(Command command, String... params) throws IOException {
        String[] paramsCopy = params.clone();
        if (paramsCopy.length > 0) {
            // Add a colon to the last param if it contains spaces
            if (paramsCopy[paramsCopy.length - 1].contains(" ")) {
                paramsCopy[paramsCopy.length - 1] = ":" + paramsCopy[paramsCopy.length - 1];
            }
            send(command.name() + " " + StringUtils.join(paramsCopy, " "));
        } else {
            send(command.name());
        }
    }

    public void send(Command command, List<String> params) throws IOException {
        String[] paramArray = params.toArray(new String[params.size()]);
        send(command, paramArray);
    }

    public String receiveLine() throws IOException {
        String line = mBufferedReader.readLine();
        Log.i(TAG, "RCV " + line);
        return line;
    }

    public Message receive() throws IOException {
        String line = receiveLine();
        if (line == null) return null;
        Message res = Message.parse(line);
        //        if (Config.LOGD) Log.d(TAG, "receive res=" + res);
        return res;
    }

    public Client getClient() {
        return mClient;
    }
}
