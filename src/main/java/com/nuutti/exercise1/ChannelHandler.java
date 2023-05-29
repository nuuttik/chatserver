package com.nuutti.exercise1;

import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChannelHandler extends MyHandler {
    private ChatDatabase db;

    public ChannelHandler(ChatDatabase db) {
        super();
        this.db = db;
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        String body = super.logHttpRequest(exchange);

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String type = exchange.getRequestHeaders().getFirst("Content-Type");

            if (type == null || !type.equals("application/json")) {
                sendResponse(exchange, 400, "Only application/json is supported.");
                return;
            }

            handlePost(exchange, body);
        }
        else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGet(exchange);
        }
        else {
            String text = "Request method " + exchange.getRequestMethod() + " is not supported.";
            sendResponse(exchange, 400, text);
        }
    }

    private void handleGet(HttpExchange exchange) {
        ArrayList<ChatChannel> channelsToSend;

        channelsToSend = db.getChannels();

        if (channelsToSend.isEmpty()) {
            sendResponse(exchange, 204, null);
            return;
        }

        JSONArray responseMessage = new JSONArray();

        for (ChatChannel c : channelsToSend) {
            responseMessage.put(c.toJson());
        }

        sendResponse(exchange, 200, responseMessage.toString());
    }

    private void handlePost(HttpExchange exchange, String body) {
        String action, name;

        try {
            JSONObject obj = new JSONObject(body);

            action = obj.getString("action");
            name = obj.getString("name");

            if (action.isEmpty() || name.isEmpty()) {
                sendResponse(exchange, 400, "You must provide values for action and name");
                return;
            }
        }
        catch (JSONException e) {
            sendResponse(exchange, 400, "Invalid request body.");
            return;
        }

        if (action.equals("create")) {
            db.addChannel(new ChatChannel(-1, name));
        }
        else {
            sendResponse(exchange, 400, "Unknown action");
            return;
        }
        sendResponse(exchange, 200, null);
    }
}
