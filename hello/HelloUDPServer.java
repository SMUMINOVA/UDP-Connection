package info.kgeorgiy.ja.muminova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HelloUDPServer extends AbstractServer {
    private DatagramSocket datagramSocket;

    public static void main(String[] args) {
        new HelloUDPServer().processArgs(args);
    }

    @Override
    public void start(int port, int threads) {
        try {
            if (datagramSocket == null) {
                datagramSocket = new DatagramSocket(port);
            }
            executorService = Executors.newFixedThreadPool(threads);
            final int buffSize = datagramSocket.getReceiveBufferSize();
            IntStream.range(0, threads).forEach(i ->
                    executorService.submit(() -> {
                        try {
                            final DatagramPacket packet = new DatagramPacket(new byte[buffSize], buffSize);
                            while (!datagramSocket.isClosed() && !Thread.interrupted()) {
                                datagramSocket.receive(packet);
                                packet.setData(("Hello, " + new String(packet.getData(), packet.getOffset(), packet.getLength()))
                                        .getBytes());
                                datagramSocket.send(packet);
                            }
                        } catch (IOException e) {
                            System.err.println("Something get wrong on port: " + port + ". IOException caught: " + e.getMessage());
                        }
                    })
            );
        } catch (SocketException e) {
            System.err.println("Something get wrong on port: " + port + ". SocketException caught: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        datagramSocket.close();
        shutdownExecutor();
    }
}