/*
 * Copyright 2017 Nicola Fäßler
 *
 * This file is part of StrimBagZ.
 *
 * StrimBagZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.lubot.strimbagzrewrite.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import net.lubot.strimbagzrewrite.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment {

    private WebView chat;

    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chat = (WebView) view.findViewById(R.id.chatView);
        chat.setWebChromeClient(new WebChromeClient());
        chat.setWebViewClient(new WebViewClient());
        chat.getSettings().setJavaScriptEnabled(true);
        chat.loadUrl("file:///android_asset/chat/index.html");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //connect();
        //chat.addJavascriptInterface(new MessageInterface(getContext()), "msgInterface");
    }


    private WebSocket connect() {
        try {
            return new WebSocketFactory()
                    .setConnectionTimeout(5000)
                    .createSocket("wss://irc-ws.chat.twitch.tv/")
                    .addListener(new WebSocketAdapter() {
                        @Override
                        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                            super.onConnected(websocket, headers);
                            websocket.sendText("CAP REQ :twitch.tv/tags twitch.tv/commands");
                            websocket.sendText("NICK justinfan48314");
                            websocket.sendText("JOIN #luigitus");
                        }

                        @Override
                        public void onTextMessage(WebSocket websocket, String text) throws Exception {
                            super.onTextMessage(websocket, text);
                            Log.d("onTextMessage", "Text: " + text);
                            if (text.startsWith("PING")) {
                                websocket.sendText("PONG");
                            }
                            if (text.startsWith("@")) {
                                // PRIVMSG
                                handleMessage(text);
                            }
                        }
                    })
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .connectAsynchronously();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void handleMessage(String rawIRC) {
        String senderName = "";
        String messageType = "UNDEFINED";
        HashMap<String, String> ircTags = new HashMap<String, String>();
        String[] msgContent = {""};
        String[] ircMsgBuffer = rawIRC.split(" ");
        int i = 0;

        while (i < ircMsgBuffer.length) {
            String msg = ircMsgBuffer[i];
            if ((msg.equals("PRIVMSG") || msg.equals("WHISPER") || msg.equals("MODE")
                    || msg.equals("PART") || msg.equals("JOIN") || msg.equals("CLEARCHAT"))
                    && messageType.equals("UNDEFINED")) {
                messageType = msg;
            }
            if (msg.charAt(0) == '@' && i == 0) {
                String[] tagList = msg.split(";");
                for (String tag : tagList) {
                    try {
                        ircTags.put(tag.split("=")[0], tag.split("=")[1]);
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
            }

            if ((messageType.equals("PRIVMSG") || messageType.equals("WHISPER")) && i > 3) {
                if (i == 4) {
                    msgContent = new String[ircMsgBuffer.length - 4];
                    msgContent[i - 4] = msg.substring(1);
                } else {
                    msgContent[i - 4] = msg;
                }
            }
            i++;
        }
        if (messageType.equals("PRIVMSG")) {
            if (senderName.isEmpty()) {
                senderName = rawIRC.substring(rawIRC.indexOf(":") + 1, rawIRC.indexOf("!"));
            }
            String msg = "";
            for (int i1 = 0, msgContentLength = msgContent.length; i1 < msgContentLength; i1++) {
                String content = msgContent[i1];
                if (i1 != 0 && i1 < msgContentLength) {
                    msg += " ";
                }
                msg += content;
            }
            Log.d("handleMessage", "Sender: " + senderName + " Msg: " + msg);
        }
    }

    public class MessageInterface {
        Context context;

        MessageInterface (Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void addMessage(String msg) {
            msg = msg.replace("\"", "'");;
        }
    }
}
