package ru.mipt.dpqe;

import ru.mipt.dpqe.client.ClientCommunicator;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        String host = "localhost";
        int port = 8080;

        ClientCommunicator communicator = new ClientCommunicator(host, port, System.out::println);
        communicator.start();
        try {
            for (int i = 0; i < 20; i++) {
                Thread.sleep(1000);
                communicator.send(String.valueOf(System.currentTimeMillis()));
            }
        } finally {
            communicator.stop();
        }
    }
}
