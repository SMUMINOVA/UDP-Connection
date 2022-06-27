package info.kgeorgiy.ja.muminova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractClient implements HelloClient {

    protected final int TIMEOUT = 100;

    protected void processArgs(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong number of arguments. Try this: " +
                    "HelloUDPClient <ip-address> <port> <prefix> <threads> <requests>");
            return;
        }
        try {
            HelloUDPClient client = new HelloUDPClient();
            client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Expected number. " + e.getMessage());
        }
    }

    protected String join(String prefix, int index, int request) {
        return prefix + index + "_" + request;
    }
}
