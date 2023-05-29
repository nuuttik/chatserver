package com.nuutti.exercise1;

import com.sun.net.httpserver.HttpExchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatHandler extends MyHandler {

    private ChatDatabase db;
    private static final DateTimeFormatter headerformat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS z", Locale.US);

    public ChatHandler(ChatDatabase db) {
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
        ArrayList<ChatMessage> messagesToSend;
        String dateStr = exchange.getRequestHeaders().getFirst("If-Modified-Since");
        String channelname = getParamMap(exchange.getRequestURI().getQuery()).get("channel");
        ChatChannel channel = null;
        if (channelname != null) {
            channel = db.getChannel(channelname);
            if (channel == null) {
                sendResponse(exchange, 400, "Unknown channel name");
                return;
            }
        }
        
        if (dateStr == null) {
            messagesToSend = db.getMessages(null, channel);
        }
        else {
            Long time = ZonedDateTime.parse(dateStr, headerformat).toInstant().toEpochMilli();
            messagesToSend = db.getMessages(time, channel);
        }

        if (messagesToSend.isEmpty()) {
            sendResponse(exchange, 204, null);
            return;
        }

        JSONArray responseMessage = new JSONArray();
        OffsetDateTime newest = null;

        for (ChatMessage m : messagesToSend) {
            if (newest == null || newest.isBefore(m.sent)) {
                newest = m.sent;
            }
            responseMessage.put(m.toJson());
        }

        String modified = newest.toInstant().atZone(ZoneId.of("GMT")).format(headerformat);
        exchange.getResponseHeaders().add("Last-Modified", modified);

        sendResponse(exchange, 200, responseMessage.toString());
    }

    private void handlePost(HttpExchange exchange, String body) {
        String user, dateStr, message = null, action = null, location = null;
        ChatChannel channel = null;
        int messageid = -1;
        OffsetDateTime odt;

        try {
            JSONObject obj = new JSONObject(body);

            user = obj.getString("user");
            dateStr = obj.getString("sent");

            if (obj.has("action")) {
                action = obj.getString("action");
                messageid = obj.getInt("messageid");
                if (action.equals("editmessage")) {
                    message = obj.getString("message");
                }
            }
            else {
                message = obj.getString("message");
                if (obj.has("location")) {
                    location = obj.getString("location");
                    if (location.isEmpty()) {
                        location = null;
                    }
                }

                String channelname = obj.getString("channel");
                channel = db.getChannel(channelname);
                if (channel == null) {
                    sendResponse(exchange, 400, "Invalid channel name.");
                    return;
                }
            }
        }
        catch (JSONException e) {
            sendResponse(exchange, 400, "Invalid request body.");
            return;
        }

        try {
            odt = OffsetDateTime.parse(dateStr);
        }
        catch (DateTimeParseException e) {
            sendResponse(exchange, 400, "Invalid datetime.");
            return;
        }

        if (user.isEmpty()) {
            sendResponse(exchange, 400, "You must provide value for user");
            return;
        }
        if (message != null && message.isEmpty()) {
            sendResponse(exchange, 400, "You must provide value for message");
            return;
        }

        if (action == null) {
            db.addMessage(new ChatMessage(-1, channel, odt, user, message, location, null, null));
        }
        else if (action.equals("editmessage")) {
            db.editMessage(new ChatMessage(messageid, channel, null, user, message, location, odt, null));
        }
        else if (action.equals("deletemessage")) {
            db.deleteMessage(new ChatMessage(messageid, channel, null, user, null, location, null, odt));
        }
        else {
            sendResponse(exchange, 400, "Unknown action");
            return;
        }
        sendResponse(exchange, 200, null);
    }

    /* Source: https://stackoverflow.com/a/63976481 */
    private Map<String, String> getParamMap(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }

        return Stream.of(query.split("&"))
        .filter(s -> !s.isEmpty())
        .map(kv -> kv.split("=", 2))
        .collect(Collectors.toMap(x -> x[0], x-> x[1]));
    }
}
