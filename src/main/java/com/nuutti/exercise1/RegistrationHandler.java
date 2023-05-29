package com.nuutti.exercise1;

import com.sun.net.httpserver.HttpExchange;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler extends MyHandler {

    private ChatAuthenticator authenticator;
    
    public RegistrationHandler(ChatAuthenticator authenticator) {
        super();
        this.authenticator = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) {
        String body = super.logHttpRequest(exchange);

        String type = exchange.getRequestHeaders().getFirst("Content-Type");

        if (type == null || !type.equals("application/json")) {
            sendResponse(exchange, 400, "Only application/json is supported.");
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePost(exchange, body);
        }
        else {
            String text = "Request method " + exchange.getRequestMethod() + " is not supported.";
            sendResponse(exchange, 400, text);
        }
    }

    private void handlePost(HttpExchange exchange, String body) {
        String name, pass, email;

        try {
            JSONObject obj = new JSONObject(body);
            name = obj.getString("username");
            pass = obj.getString("password");
            email = obj.getString("email");
        }
        catch (JSONException e) {
            sendResponse(exchange, 400, "Invalid request body.");
            return;
        }

        if (name.trim().isEmpty() || pass.trim().isEmpty() || email.trim().isEmpty()) {
            sendResponse(exchange, 400, "All json values must not be empty.");
            return;
        }

        if (authenticator.addUser(name, pass, email)) {
            sendResponse(exchange, 200, null);
        }
        else {
            sendResponse(exchange, 403, "Username already exists.");
        }
    }
}
