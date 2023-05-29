package com.nuutti.exercise1;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ChatMessage {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public int id;
    public ChatChannel channel;
    public OffsetDateTime sent;
    public String nick;
    public String message;
    public String location;
    public OffsetDateTime edited;
    public OffsetDateTime deleted;

    public ChatMessage(int id, ChatChannel channel, OffsetDateTime sent, String nick, String message, String location, OffsetDateTime edited, OffsetDateTime deleted) {
        this.id = id;
        this.channel = channel;
        this.sent = sent;
        this.nick = nick;
        this.message = message;
        this.location = location;
        this.edited = edited;
        this.deleted = deleted;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("user", nick);
        obj.put("channel", channel.name);
        obj.put("sent", sent.format(formatter));
        if (deleted != null) {
            obj.put("deleted", deleted.format(formatter));
            obj.put("message", "<deleted>");
        }
        else if (edited != null) {
            obj.put("edited", edited.format(formatter));
            obj.put("message", "<edited> " + message);
        }
        else {
            obj.put("message", message);
        }

        if (location != null) {
            obj.put("location", location);
            String temp = getTemperature(location);
            if (temp != null) {
                obj.put("temperature", temp);
            }
        }
        
        return obj;
    }

    public long dateAsInt() {
        return sent.toInstant().toEpochMilli();
    }

    public void setSent(long epoch) {
        sent = OffsetDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    private String getTemperature(String place) {
        try {
            URL url = new URL("http://opendata.fmi.fi/wfs/fin?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::timevaluepair&maxlocations=1&parameters=t2m&place=" + place);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(con.getInputStream());
            Element root = doc.getDocumentElement();

            NodeList map = root.getElementsByTagName("wml2:MeasurementTVP");
            Element e = (Element)map.item(map.getLength() - 1);
            String temperature = e.getElementsByTagName("wml2:value").item(0).getTextContent();
            
            return temperature;
        }
        catch (Exception e) {
            return null;
        }
    }
}
