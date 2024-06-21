package models;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ClientThread {
    private static final int NUM_CLIENTS = 1; // Number of clients to simulate
    private static final String HOST = "localhost";
    private static int port = 8080;

        private static final Logger logger = Logger.getLogger(ClientThread.class.getName());


    public static void main(String[] args) {
        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            logger.info("Creating client " + i);
            final short clientId = (short) i;
            final int newPort = port + i;
            Thread clientThread = new Thread(() -> {
                Client client = new Client(HOST, newPort, clientId);
                client.start();
            });

            clientThreads.add(clientThread);
        }

        // Start all client threads
        for (Thread thread : clientThreads) {
            thread.start();
        }

        // Wait for all client threads to finish
        for (Thread thread : clientThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
