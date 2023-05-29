package com.nuutti.exercise1;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public abstract class MyHandler implements HttpHandler {

    protected Logger LOG;

    public MyHandler() {
        LOG = Logger.getLogger(this.getClass().getName());
        LOG.setLevel(Level.ALL);
    }

    protected String logHttpRequest(HttpExchange exchange) {
        InetSocketAddress address = exchange.getRemoteAddress();
        StringBuilder sb = new StringBuilder("Incoming request from '" + address.getHostName() + ":" + address.getPort() + "': '" +
        exchange.getRequestMethod() + " " + exchange.getRequestURI() + "' headers=");

        String separator = "";
        for (Entry<String, List<String>> e : exchange.getRequestHeaders().entrySet()) {
            sb.append(separator);
            sb.append("'" + e.getKey() + "': '");
            sb.append(String.join(", ", e.getValue()) + "'");
            separator = ", ";
        }

        InputStream stream = exchange.getRequestBody();
        
        String body = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

        try {
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        sb.append(" body=" + body);

        sb.append(" threadID=" + Thread.currentThread().getId());

        LOG.info(sb.toString());

        return body;
    }

    protected void sendResponse(HttpExchange exchange, int statuscode, String text) {
        InetSocketAddress address = exchange.getRemoteAddress();

        StringBuilder sb = new StringBuilder("Sending response to '" + address.getHostName() + ":" + address.getPort() + "': status code=" +
        statuscode);

        LOG.info(sb.toString());

        try {
            if (text == null) {
                exchange.sendResponseHeaders(statuscode, -1);
            }
            else {
                byte[] bytes = text.getBytes("UTF-8");
                exchange.sendResponseHeaders(statuscode, bytes.length);

                OutputStream stream = exchange.getResponseBody();
                stream.write(bytes);
                stream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
