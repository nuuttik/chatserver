package com.nuutti.exercise1;

import com.sun.net.httpserver.BasicAuthenticator;

import org.apache.commons.codec.digest.Crypt;

public class ChatAuthenticator extends BasicAuthenticator {

    private ChatDatabase db;

    public ChatAuthenticator(ChatDatabase db, String realm) {
        super(realm);

        this.db = db;
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        User user = db.getUser(username);
        if (user != null && user.getPassword().equals(Crypt.crypt(password, user.getSalt()))) {
            return true;
        }
        return false;
    }
    
    public boolean addUser(String username, String password, String email) {
        if (db.getUser(username) != null) {
            return false;
        }

        db.addUser(username, password, email);
        return true;
    }
}
