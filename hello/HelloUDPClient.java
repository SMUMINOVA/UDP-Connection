package info.kgeorgiy.ja.muminova.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient extends AbstractClient {

    public static void main(String[] args) {
        new HelloUDPClient().processArgs(args);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; ++i) {
                executor.submit(getTask(prefix, requests, socketAddress, i));
            }
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(TIMEOUT * threads * requests, TimeUnit.SECONDS)) {
                    System.err.println("Timed out");
                }
            } catch (InterruptedException e) {
                System.err.println("Something get wrong while terminating: " + e.getMessage());
            }
        } catch (UnknownHostException e) {
            System.err.println("Something get wrong: " + e.getMessage());
        }
    }

    private Runnable getTask(String prefix, int requests, InetSocketAddress socketAddress, int i) {
        return () -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(TIMEOUT);
                int receiveBuffer = socket.getReceiveBufferSize();
                DatagramPacket packet = new DatagramPacket(new byte[receiveBuffer], receiveBuffer, socketAddress);
                for (int j = 0; j < requests; j++) {
                    String request = join(prefix, i, j);
                    byte[] requestInBytes = request.getBytes(StandardCharsets.UTF_8);
                    String response = "";
                    while (!response.contains(request)){
                        try {
                            packet.setData(requestInBytes);
                            socket.send(packet);
                            packet.setData(new byte[receiveBuffer]);
                            socket.receive(packet);
                            response = new String(packet.getData(), packet.getOffset(),
                                    packet.getLength(), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            System.err.println("IOException caught for request: " + request + ". " + e.getMessage());
                        }
                    }
                    System.out.println("Sent: " + request + ". Received: " + response);
                }
            } catch (SocketException e) {
                System.err.println("SocketException caught: " + e.getMessage());
            }
        };
    }
}