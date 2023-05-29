package com.nuutti.exercise1;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.KeyStore;
import java.io.FileInputStream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

/**
 * Käyttäjä pystyy luomaan uuden viestikanavan lähettämällä HTTP POST pyynnön /channel
 * polkuun, "action" ja "name" json elementtien kanssa. "action" elementin arvo pitää olla "create"
 * ja "name" elementin arvo pitää olla haluttu kanavan nimi.
 * <p>
 * Käyttäjä saa luettelon kaikista serverin kanavista lähettämällä HTTP GET pyynnön
 * /channel polkuun.
 * <p>
 * Käyttäjä voi pyytää vain tietyn kanavan viestit lähettämällä HTTP GET pyynnön /chat
 * polkuun channel parametrin kanssa. Esimerkiksi /chat?channel=testchannel jossa "testchannel"
 * on kanavan nimi.
 * 
 * @author  Nuutti
 * @version 1.0
 */
public class ChatServer {

    private static Logger LOG = Logger.getLogger(ChatServer.class.getName());

    public static void main(String[] args) {
        LOG.setLevel(Level.ALL);
        HttpsServer server;
        SSLContext ssl;
        InetSocketAddress address;

        if (args.length < 3) {
            System.out.println("Usage: java -jar chat-server-jar-file database.db keystore.jks password123");
            System.out.println("Where first parameter is database file name");
            System.out.println("Second parameter is certificate file name");
            System.out.println("Third parameter is password of the certificate");
            return;
        }

        try {
            address = new InetSocketAddress(InetAddress.getByName(null), 8001);
            server = HttpsServer.create(address, 0);
            ssl = chatServerSSLContext(args[1], args[2]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        server.setHttpsConfigurator(new HttpsConfigurator(ssl) {
            public void configure (HttpsParameters params) {
                InetSocketAddress remote = params.getClientAddress();

                SSLContext c = getSSLContext();

                SSLParameters sslparams = c.getDefaultSSLParameters();
                params.setSSLParameters(sslparams);
            }
        });

        ChatDatabase db = new ChatDatabase(args[0]);

        ChatAuthenticator authenticator = new ChatAuthenticator(db, "chat");

        HttpContext context = server.createContext("/chat", new ChatHandler(db));
        context.setAuthenticator(authenticator);

        context = server.createContext("/channel", new ChannelHandler(db));
        context.setAuthenticator(authenticator);
        
        server.createContext("/registration", new RegistrationHandler(authenticator));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        LOG.info("Server listening on " + address.getHostName() + ":" + address.getPort());

        while (true) {
            String line = System.console().readLine();
            if(line.equals("/quit")) {
                server.stop(3);
                db.close();
                break;
            }
        }
    }

    private static SSLContext chatServerSSLContext(String certname, String certpass) throws Exception {
        char[] passphrase = certpass.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(certname), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }
}
