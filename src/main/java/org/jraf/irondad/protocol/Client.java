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

import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.HandlerManager;
import org.jraf.irondad.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final String TAG = Constants.TAG + Client.class.getSimpleName();

    private static final String ABOUT = Constants.PROJECT_FULL_NAME + " " + Constants.VERSION_NAME + " - " + Constants.PROJECT_URL;

    private static final int READ_TIMEOUT = 5 * 60 * 1000; // ms

    private ClientConfig mClientConfig;
    private Connection mConnection;
    private int mAlternateNickCounter;
    private String mCurrentNickname;
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(4);
    private boolean mRegistered;
    private volatile boolean mStopRequested;
    private HandlerManager mHandlerManager;
    private boolean mJoinChannelsScheduled;
    private boolean mSaslRequested;
    private boolean mCapEndSent;

    public Client(ClientConfig clientConfig) {
        mClientConfig = clientConfig;
        mHandlerManager = new HandlerManager(clientConfig);
    }

    public void startMainLoop() {
        if (Config.LOGD) Log.d(TAG, "startMainLoop");
        while (true) {
            try {
                mRegistered = false;
                connectAndStartReceiveLoop();
            } catch (IOException e) {
                Log.e(TAG, "startMainLoop Exception caught in connectAndStartReceiveLoop", e);
            }

            // Testing end of loop before waiting
            if (mStopRequested) {
                if (Config.LOGD) Log.d(TAG, "startMainLoop Exiting from main loop");
                return;
            }

            if (Config.LOGD) Log.d(TAG, "startMainLoop Waiting a bit before retrying...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {}


            // Testing end of loop after waiting
            if (mStopRequested) {
                if (Config.LOGD) Log.d(TAG, "startMainLoop Exiting from main loop");
                return;
            }
        }
    }

    public synchronized void stop() {
        if (Config.LOGD) Log.d(TAG, "stop");
        mStopRequested = true;
        if (mConnection != null) {
            try {
                send(Command.QUIT, ABOUT);
            } catch (IOException e) {
                Log.w(TAG, "stop Could not send QUIT", e);
            }
        }
    }

    public void connectAndStartReceiveLoop() throws IOException {
        if (Config.LOGD) Log.d(TAG, "connectAndStartReceiveLoop");

        Socket socket;
        try {
            socket = connect();
        } catch (IOException e) {
            Log.w(TAG, "connectAndStartReceiveLoop Could not connect", e);
            throw e;
        }
        try {
            mConnection = new Connection(this, socket);
        } catch (IOException e) {
            Log.w(TAG, "connectAndStartReceiveLoop Could not create connection from socket", e);
            throw e;
        }
        mHandlerManager.setConnection(mConnection);
        try {
            register();
        } catch (IOException e) {
            Log.w(TAG, "connectAndStartReceiveLoop Could not register", e);
            throw e;
        }
        try {
            startReceiveLoop();
        } catch (IOException e) {
            Log.w(TAG, "connectAndStartReceiveLoop Exception caught in receive loop", e);
            throw e;
        }
    }

    private Socket connect() throws IOException {
        if (Config.LOGD) Log.d(TAG, "connect");
        Socket socket = new Socket(mClientConfig.getHost(), mClientConfig.getPort());
        socket.setSoTimeout(READ_TIMEOUT);
        return socket;
    }

    private void register() throws IOException {
        if (Config.LOGD) Log.d(TAG, "register");
        resetCapabilityState();
        if (isSaslConfigured()) {
            mSaslRequested = true;
            send(Command.CAP, "REQ", "sasl");
        }
        nick(mClientConfig.getNickname());
        send(Command.USER, mClientConfig.getNickname(), "0", "*", ABOUT);
    }

    private void resetCapabilityState() {
        mSaslRequested = false;
        mCapEndSent = false;
    }

    private boolean isSaslConfigured() {
        return mClientConfig.getSaslUsername() != null && !mClientConfig.getSaslUsername().isEmpty() && mClientConfig.getSaslPassword() != null && !mClientConfig.getSaslPassword().isEmpty();
    }

    private void nick(String nickname) throws IOException {
        if (Config.LOGD) Log.d(TAG, "nick nickname=" + nickname);
        mCurrentNickname = nickname;
        send(Command.NICK, nickname);
    }

    private void alternativeNick() throws IOException {
        if (!mRegistered) {
            // Not registered: try a new alternative nickname
            nick(mClientConfig.getNickname() + mAlternateNickCounter);
            mAlternateNickCounter++;
        }
        if (Config.LOGD) Log.d(TAG, "alternativeNick Scheduling to try to gain 'own' nick in 5 minutes");
        mScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    nick(mClientConfig.getNickname());
                } catch (IOException e) {
                    Log.w(TAG, "alternativeNick Could not send NICK command", e);
                }
            }
        }, 5 * 60, TimeUnit.SECONDS);
    }

    private void joinChannels() throws IOException {
        if (Config.LOGD) Log.d(TAG, "joinChannels");
        for (String channel : mClientConfig.getChannels()) {
            send(Command.JOIN, channel);
        }
    }

    private void startReceiveLoop() throws IOException {
        if (Config.LOGD) Log.d(TAG, "startReceiveLoop");
        while (true) {
            // Testing end of loop before receiving
            if (mStopRequested) {
                if (Config.LOGD) Log.d(TAG, "startReceiveLoop Exiting from receive loop");
                return;
            }

            Message message = receive();

            // Testing end of loop after receiving
            if (mStopRequested) {
                if (Config.LOGD) Log.d(TAG, "startReceiveLoop Exiting from receive loop");
                return;
            }

            if (message == null) {
                throw new IOException("startReceiveLoop Received null message");
            }
            handleMessage(message);
        }
    }

    protected void handleMessage(Message message) throws IOException {
        switch (message.command) {
            case CAP:
                handleCap(message);
                break;

            case AUTHENTICATE:
                handleAuthenticate(message);
                break;

            case RPL_WELCOME:
                mRegistered = true;
                scheduleJoinChannels();
                break;

            case RPL_NAMREPLY:
                gainOpIfNecessary(message);
                break;

            case RPL_LOGGEDIN:
            case RPL_SASLSUCCESS:
                handleSaslSuccess();
                break;

            case ERR_SASLFAIL:
            case ERR_SASLTOOLONG:
            case ERR_SASLABORTED:
            case ERR_SASLALREADY:
            case RPL_SASLMECHS:
                handleSaslFailure(message);
                break;

            case ERR_ERRONEUSNICKNAME:
            case ERR_NICKCOLLISION:
            case ERR_NICKNAMEINUSE:
                alternativeNick();
                break;

            case PING:
                send(Command.PONG, message.parameters);
                break;

            case PRIVMSG:
                handlePrivmsg(message);
                break;

            case PART:
                send(Command.NAMES, message.parameters.get(0));
                break;
        }
    }


    private void scheduleJoinChannels() {
        if (Config.LOGD) Log.d(TAG, "scheduleJoinChannels Scheduling to join channels every 5 minutes");
        if (mJoinChannelsScheduled) return;
        mJoinChannelsScheduled = true;
        mScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    joinChannels();
                } catch (IOException e) {
                    Log.w(TAG, "scheduleJoinChannels Could not join channels", e);
                }
            }
        }, 0, 5 * 60, TimeUnit.SECONDS);
    }

    private void gainOpIfNecessary(Message message) {
        if (Config.LOGD) Log.d(TAG, "gainOp message=" + message);
        String channel = message.parameters.get(2);
        String namesStr = message.parameters.get(3);
        String[] names = namesStr.split(" ");
        if (names.length == 1 && names[0].equals(mCurrentNickname)) {
            if (Config.LOGD) Log.d(TAG, "gainOp Only user on the chan: gaining op");
            try {
                send(Command.PART, channel);
                send(Command.JOIN, channel);
            } catch (IOException e) {
                Log.w(TAG, "gainOp Could not gain op", e);
            }
        }
    }

    private void handlePrivmsg(Message message) {
        if (Config.LOGD) Log.d(TAG, "handlePrivmsg message=" + message);
        String dest = message.parameters.get(0);
        String channel = dest.startsWith("#") ? dest : null;
        String fromNickname = message.origin.name;
        String text = message.parameters.get(1);
        mHandlerManager.handle(channel, fromNickname, text, message);
    }

    private void handleCap(Message message) throws IOException {
        if (!mSaslRequested) return;
        if (message.parameters.size() < 2) return;
        String subCommand = message.parameters.get(1);
        if ("ACK".equalsIgnoreCase(subCommand)) {
            if (message.parameters.size() >= 3 && message.parameters.get(2).toLowerCase().contains("sasl")) {
                send(Command.AUTHENTICATE, "PLAIN");
            } else {
                finishCapNegotiation();
            }
        } else if ("NAK".equalsIgnoreCase(subCommand)) {
            Log.w(TAG, "handleCap SASL capability not acknowledged");
            finishCapNegotiation();
        }
    }

    private void handleAuthenticate(Message message) throws IOException {
        if (!mSaslRequested) return;
        if (message.parameters.isEmpty()) return;
        if ("+".equals(message.parameters.get(0))) {
            String authPayload = "\0" + mClientConfig.getSaslUsername() + "\0" + mClientConfig.getSaslPassword();
            String encoded = Base64.getEncoder().encodeToString(authPayload.getBytes(StandardCharsets.UTF_8));
            send(Command.AUTHENTICATE, encoded);
        }
    }

    private void handleSaslSuccess() throws IOException {
        if (!mSaslRequested) return;
        finishCapNegotiation();
    }

    private void handleSaslFailure(Message message) throws IOException {
        if (!mSaslRequested) return;
        Log.w(TAG, "handleSaslFailure command=" + message.command);
        finishCapNegotiation();
    }

    private void finishCapNegotiation() throws IOException {
        if (mSaslRequested && !mCapEndSent) {
            send(Command.CAP, "END");
            mCapEndSent = true;
        }
    }


    /*
     * Connection delegates.
     */

    private void send(Command command, List<String> params) throws IOException {
        mConnection.send(command, params);
    }

    private void send(Command command, String... params) throws IOException {
        mConnection.send(command, params);
    }

    private Message receive() throws IOException {
        return mConnection.receive();
    }

    public ClientConfig getClientConfig() {
        return mClientConfig;
    }
}
