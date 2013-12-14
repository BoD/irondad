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
package org.jraf.irondad.handler.pixgame;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jraf.irondad.Config;
import org.jraf.irondad.Constants;
import org.jraf.irondad.handler.Handler;
import org.jraf.irondad.protocol.ClientConfig;
import org.jraf.irondad.protocol.Command;
import org.jraf.irondad.protocol.Connection;
import org.jraf.irondad.protocol.Message;
import org.jraf.irondad.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

public class PixGameHandler implements Handler {
    private static final String TAG = Constants.TAG + PixGameHandler.class.getSimpleName();

    private static final String APPLICATION_NAME = "BoD-irondad/1.0";
    private static final String COMMAND = "!pix";
    private static final String RANDOM = "random";
    private static final String PREFIX = PixGameHandler.class.getName() + ".";
    public static final String CONFIG_KEY = PREFIX + "KEY";
    public static final String CONFIG_CX = PREFIX + "CX";
    public static final String CONFIG_PATH_DICT = PREFIX + "PATH_DICT";

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final int RESULT_SIZE = 10;
    private static final int GUESSES_FIRST_HINT = 15;
    private static final int GUESSES_SECOND_HINT = 25;
    private static final int GUESSES_THIRD_HINT = 29;
    private static final int GUESSES_MAX = 30;
    private static final String URL_HIDE = "http://lubek.b.free.fr/a.html?a=";

    private String mCx;
    private String mDictPath;
    private Customsearch mCustomSearch;
    private String mSearchTerms;
    private String mGameCreatedBy;
    private int mGuessCount;
    private long mSearchResultCount;
    private List<Result> mSearchResults;

    @Override
    public void init(ClientConfig clientConfig) {
        mDictPath = clientConfig.getExtraConfig(CONFIG_PATH_DICT);
        Customsearch.Builder customSearchBuilder;
        try {
            customSearchBuilder = new Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, null);
            customSearchBuilder.setApplicationName(APPLICATION_NAME);
            String key = clientConfig.getExtraConfig(CONFIG_KEY);
            mCx = clientConfig.getExtraConfig(CONFIG_CX);
            customSearchBuilder.setCustomsearchRequestInitializer(new CustomsearchRequestInitializer(key));
            mCustomSearch = customSearchBuilder.build();
        } catch (Exception e) {
            Log.e(TAG, "PixGameHandler Could not initialize! PixGameHandler will not work until this problem is resolved", e);
        }
    }

    @Override
    public boolean handleMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message)
            throws Exception {
        if (!text.startsWith(COMMAND)) return false;

        if (channel == null) {
            // Private message
            handlePrivateMessage(connection, fromNickname, text, textAsList);
        } else {
            // Channel message
            handleChannelMessage(connection, channel, fromNickname, text, textAsList, message);
        }
        return true;
    }

    private void handlePrivateMessage(Connection connection, String fromNickname, String text, List<String> textAsList) throws IOException {
        if (textAsList.size() < 2) {
            connection.send(Command.PRIVMSG, fromNickname, "Syntax: \"!pix <search terms>\" or \"!pix random\" to use a random word.");
            return;
        }
        if (isGameOngoing()) {
            connection.send(Command.PRIVMSG, fromNickname, "A game is already ongoing (started by " + mGameCreatedBy + ").");
            return;
        }
        String searchTerms = text.trim().substring(COMMAND.length() + 1).trim();
        newGame(connection, fromNickname, searchTerms);
        return;
    }

    private void handleChannelMessage(Connection connection, String channel, String fromNickname, String text, List<String> textAsList, Message message)
            throws IOException {
        if (!isGameOngoing()) {
            connection.send(Command.PRIVMSG, channel, fromNickname
                    + ": No game is currently ongoing.  Privmsg me \"!pix <search terms>\" or \"!pix random\" to start one.");
            return;
        }
        if (text.trim().equals(COMMAND)) {
            // Give current status
            connection.send(Command.PRIVMSG, channel, "Ongoing game started by " + mGameCreatedBy + " (" + mGuessCount
                    + " guesses).  Guess the search terms with \"!pix <your guess>\".");
            // Show the current link
            connection.send(Command.PRIVMSG, channel, hideUrl(mSearchResults.get(mGuessCount % RESULT_SIZE).getLink()));
        } else {
            // Guess the word
            String guess = text.trim().substring(COMMAND.length() + 1).trim();
            guess(connection, channel, fromNickname, guess);
        }
    }

    private void newGame(Connection connection, String fromNickname, String searchTerms) throws IOException {
        boolean isRandom = RANDOM.equalsIgnoreCase(searchTerms);
        if (isRandom) {
            searchTerms = getRandomWord();
        } else if (!StringUtils.isAlphanumericSpace(searchTerms)) {
            connection.send(Command.PRIVMSG, fromNickname, "No punctuation allowed in the search terms.  Try another one.");
            return;
        }
        try {
            mSearchResultCount = queryGoogle(connection, searchTerms);
        } catch (IOException e) {
            Log.w(TAG, "newGame Could not query Google", e);
            connection.send(Command.PRIVMSG, fromNickname, "Oops something went wrong...");
            return;
        }
        if (mSearchResultCount == 0) {
            connection.send(Command.PRIVMSG, fromNickname, "This search has no results!  Try another one.");
            resetGame();
            return;
        }
        mSearchTerms = searchTerms;
        mGameCreatedBy = fromNickname;

        if (isRandom) {
            connection.send(Command.PRIVMSG, fromNickname, "New game started with a random word search.");
        } else {
            connection.send(Command.PRIVMSG, fromNickname, "New game started with the search \"" + searchTerms + "\".");
        }
    }

    private long queryGoogle(Connection connection, String searchTerms) throws IOException {
        if (Config.LOGD) Log.d(TAG, "queryGoogle searchTerms=" + searchTerms);
        com.google.api.services.customsearch.Customsearch.Cse.List list = mCustomSearch.cse().list("\"" + searchTerms + "\"");
        list.setCx(mCx);
        list.setSearchType("image");
        list.setFields("items/link,searchInformation/totalResults");
        list.setNum((long) RESULT_SIZE);
        list.setStart((long) mGuessCount + 1);

        // Execute the query
        Search search = list.execute();

        mSearchResults = search.getItems();

        return search.getSearchInformation().getTotalResults();
    }

    private void guess(Connection connection, String channel, String fromNickname, String guess) throws IOException {
        mGuessCount++;
        if (!StringUtils.stripAccents(mSearchTerms.toLowerCase(Locale.FRANCE)).equals(StringUtils.stripAccents(guess.toLowerCase(Locale.FRANCE)))) {
            // Lost
            connection.send(Command.PRIVMSG, channel, fromNickname + ": WRONG.");

            if (mGuessCount >= mSearchResultCount) {
                connection.send(Command.PRIVMSG, channel, "Well there are no more results.  You lose, after " + mGuessCount
                        + " guesses!  The secret search was \"" + mSearchTerms + "\"...  FAIL.");
                resetGame();
                return;
            }

            switch (mGuessCount) {
                case GUESSES_FIRST_HINT:
                    int nbWords = mSearchTerms.split("\\s+").length;
                    connection.send(Command.PRIVMSG, channel, "Ok since you guys suck, here's an hint: the search has " + nbWords + " word"
                            + (nbWords == 1 ? "." : "s."));
                    break;

                case GUESSES_SECOND_HINT:
                    connection.send(Command.PRIVMSG, channel, "Ok since you guys suck, here's another hint: the search looks like this: \"" + getSecondHint()
                            + "\".");
                    break;

                case GUESSES_THIRD_HINT:
                    connection.send(Command.PRIVMSG, channel, "Ok I'll give you one last hint: the search looks like this: \"" + getThirdHint() + "\".");
                    break;

                case GUESSES_MAX:
                    connection.send(Command.PRIVMSG, channel, "Ok you guys suck too much.  You lose, after " + mGuessCount
                            + " guesses!  The secret search was \"" + mSearchTerms + "\"...  FAIL.");
                    resetGame();
                    return;

            }

            if (mGuessCount % RESULT_SIZE == 0) {
                // Fetch a new results page
                queryGoogle(connection, mSearchTerms);
            }

            connection.send(Command.PRIVMSG, channel, hideUrl(mSearchResults.get(mGuessCount % RESULT_SIZE).getLink()));
        } else {
            // Won
            connection.send(Command.PRIVMSG, channel, fromNickname + ": YES!  The secret search was \"" + mSearchTerms + "\".  It was found in " + mGuessCount
                    + " guesses.  Congrats!");
            resetGame();
        }
    }

    private String getSecondHint() {
        int len = mSearchTerms.length();
        StringBuilder res = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            if (mSearchTerms.charAt(i) == ' ') {
                res.append(' ');
            } else {
                res.append('*');
            }
        }
        return res.toString();
    }

    private String getThirdHint() {
        int len = mSearchTerms.length();
        StringBuilder res = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = mSearchTerms.charAt(i);
            if (i == 0 || mSearchTerms.charAt(i - 1) == ' ' || i == len - 1 || mSearchTerms.charAt(i + 1) == ' ') {
                res.append(c);
                continue;
            }
            if (c == ' ') {
                res.append(' ');
            } else {
                res.append('*');
            }
        }
        return res.toString();
    }

    private boolean isGameOngoing() {
        return mSearchTerms != null;
    }

    private void resetGame() {
        mSearchTerms = null;
        mGameCreatedBy = null;
        mGuessCount = 0;
        mSearchResults = null;
    }

    private static String hideUrl(String url) {
        return URL_HIDE + rot13(url.substring(url.indexOf("//") + 2));
    }

    private static String rot13(String s) {
        int len = s.length();
        StringBuilder res = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (!Character.isLetter(ch)) {
                res.append(ch);
            } else if (Character.isUpperCase(ch)) {
                res.append((char) ((ch - 'A' + 13) % 26 + 'A'));
            } else {
                res.append((char) ((ch - 'a' + 13) % 26 + 'a'));
            }
        }
        return res.toString();
    }

    public String getRandomWord() throws IOException {
        RandomAccessFile file = new RandomAccessFile(mDictPath, "r");
        file.seek((long) (Math.random() * file.length()));
        // Eat the characters of the current line to go to beginning of the next line
        file.readLine();
        // Now read and return the line
        return file.readLine();
    }
}
