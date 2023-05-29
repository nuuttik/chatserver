package com.nuutti.exercise1;

import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;

import java.sql.ResultSet;

public class ChatDatabase {

    private Connection conn;
    private SecureRandom sr = new SecureRandom();
    
    public ChatDatabase(String filename) {
        boolean exists = new File(filename).isFile();

        String url = "jdbc:sqlite:" + filename;

        try {
            conn = DriverManager.getConnection(url);

            if (!exists) {
                initializeDatabase();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            conn.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeDatabase() throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("CREATE TABLE channels (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)");
        stmt.executeUpdate("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, email TEXT NOT NULL, password TEXT NOT NULL, salt TEXT NOT NULL)");
        stmt.executeUpdate("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, channel INTEGER NOT NULL, user TEXT NOT NULL, timestamp INTEGER NOT NULL, text TEXT NOT NULL, location TEXT, edited INTEGER, deleted INTEGER, FOREIGN KEY(channel) REFERENCES channels(id))");

        stmt.close();
    }

    public void addUser(String name, String password, String email) {
        String salt = getSalt();
        String hashedPassword = Crypt.crypt(password, salt);

        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, email, password, salt) VALUES (?,?,?,?)");
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, salt);

            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMessage(ChatMessage msg) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages (user, channel, timestamp, text, location, edited, deleted) VALUES (?,?,?,?,?,NULL,NULL)");
            stmt.setString(1, msg.nick);
            stmt.setInt(2, msg.channel.id);
            stmt.setLong(3, msg.dateAsInt());
            stmt.setString(4, msg.message);
            stmt.setString(5, msg.location);

            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addChannel(ChatChannel channel) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO channels (name) VALUES (?)");
            stmt.setString(1, channel.name);

            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMessage(ChatMessage msg) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE messages SET text = ?, edited = ? WHERE id = ?");
            stmt.setString(1, msg.message);
            stmt.setLong(2, msg.edited.toInstant().toEpochMilli());
            stmt.setInt(3, msg.id);

            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(ChatMessage msg) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE messages SET deleted = ? WHERE id = ?");
            stmt.setLong(1, msg.deleted.toInstant().toEpochMilli());
            stmt.setInt(2, msg.id);

            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser(String name) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT email, password, salt FROM users WHERE name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String password = rs.getString("password");
                String salt = rs.getString("salt");

                return new User(name, email, password, salt);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public ArrayList<ChatMessage> getMessages(Long time, ChatChannel channel) {
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();

        try {
            ResultSet rs;
            PreparedStatement stmt;
            if (time == null) {
                if (channel == null) {
                    stmt = conn.prepareStatement("SELECT * FROM messages");
                }
                else {
                    stmt = conn.prepareStatement("SELECT * FROM messages WHERE channel = ?");
                    stmt.setInt(1, channel.id);
                }
            }
            else
            {
                if (channel == null) {
                    stmt = conn.prepareStatement("SELECT * FROM messages WHERE timestamp > ?");
                }
                else {
                    stmt = conn.prepareStatement("SELECT * FROM messages WHERE timestamp > ? AND channel = ?");
                    stmt.setInt(2, channel.id);
                }
                stmt.setLong(1, time);
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                Integer channelid = rs.getInt("channel");
                String user = rs.getString("user");
                long iTimestamp = rs.getLong("timestamp");
                String text = rs.getString("text");
                String location = rs.getString("location");
                long iEdited = rs.getLong("edited");
                long iDeleted = rs.getLong("deleted");
                
                OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli(iTimestamp), ZoneOffset.UTC);
                OffsetDateTime edited = iEdited == 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochMilli(iEdited), ZoneOffset.UTC);
                OffsetDateTime deleted = iDeleted == 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochMilli(iDeleted), ZoneOffset.UTC);

                channel = getChannel(channelid);

                messages.add(new ChatMessage(id, channel, timestamp, user, text, location, edited, deleted));
            }

            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return messages;
    }

    public ArrayList<ChatChannel> getChannels() {
        ArrayList<ChatChannel> channels = new ArrayList<ChatChannel>();

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM channels");

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                channels.add(new ChatChannel(id, name));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return channels;
    }

    public ChatChannel getChannel(int id) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM channels WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");

                return new ChatChannel(id, name);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public ChatChannel getChannel(String name) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM channels WHERE name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");

                return new ChatChannel(id, name);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private String getSalt() {
        byte bytes[] = new byte[13];
        sr.nextBytes(bytes);

        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;

        return salt;
    }
}
