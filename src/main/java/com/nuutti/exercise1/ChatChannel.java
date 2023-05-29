package com.nuutti.exercise1;

import org.json.JSONObject;

public class ChatChannel {
    public int id;
    public String name;

    public ChatChannel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);

        return obj;
    }
}
