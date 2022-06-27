package info.kgeorgiy.ja.muminova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractServer implements HelloServer {
    protected ExecutorService executorService;

    protected void processArgs(String[] args){
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong number of arguments. Try this: HelloUDPServer <port> <threads>");
            return;
        }
        try {
            HelloUDPServer server = new HelloUDPServer();
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.print("Expected number. " + e.getMessage());
        }
    }

    protected void shutdownExecutor(){
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Time out");
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Shutdown executors were interrupted: " + e.getMessage());
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
