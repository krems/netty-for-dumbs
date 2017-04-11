package ru.mipt.dpqe;

import ru.mipt.dpqe.server.ServerCommunicator;
import java.util.Date;

public class Server {

    public static void main(String[] args) throws InterruptedException {
        ServerCommunicator communicator = new ServerCommunicator(System.out::println, 8080);
        communicator.start();
        try {
            for (int i = 0; i < 2000; i++) {
                Thread.sleep(1000);
                communicator.broadcastMessage(new Date().toString());
            }
        } finally {
            communicator.stop();
        }
    }
}
