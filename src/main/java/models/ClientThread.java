package models;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ClientThread {
    private static int NUM_CLIENTS; // Number of clients to simulate
    private static int port = 8080;

        private static final Logger logger = Logger.getLogger(ClientThread.class.getName());


    public static void main(String[] args) {
        List<Thread> clientThreads = new ArrayList<>();
        NUM_CLIENTS = Integer.parseInt(args[0]);
        for (int i = 0; i < NUM_CLIENTS; i++) {
            logger.info("Creating client " + i);
            final short clientId = (short) i;
            Thread clientThread = new Thread(() -> {
                Client client = new Client(args[1], port, clientId);
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
