package info.kgeorgiy.ja.muminova.hello;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

public class HelloUDPNonblockingClient extends AbstractClient {
    private String requestPrefix;

    private static final class Data {
        int thread;
        ByteBuffer byteBuffer;
        int request = 0;

        public Data(int thread, int size) {
            this.thread = thread;
            this.byteBuffer = ByteBuffer.allocate(size);
        }
    }

    public static void main(String[] args) {
        new HelloUDPNonblockingClient().processArgs(args);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        requestPrefix = prefix;
        try {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            Selector selector = Selector.open();
            for (int i = 0; i < threads; ++i) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(inetSocketAddress);
                channel.register(selector, SelectionKey.OP_WRITE, new Data(i, channel.socket().getReceiveBufferSize()));
            }
            while (!selector.keys().isEmpty() && !Thread.interrupted()) {
                try {
                    int selected = selector.select(TIMEOUT);
                    if (selected != 0) {
                        selector.select(key -> {
                            send(key);
                            receive(key, requests);
                        });
                    } else {
                        selector.keys().forEach(this::send);
                    }
                } catch (IOException e) {
                    System.err.println("Caught IOException while sending and receiving. " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Something get wrong while creating selector and channels. " + e.getMessage());
        }
    }

    private void receive(SelectionKey key, int requests) {
        if (!key.isReadable()) {
            return;
        }
        Data data = (Data) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            channel.receive(data.byteBuffer.clear());
        } catch (IOException e) {
            System.err.println("IOException caught while receiving. " + e.getMessage());
        }
        String response = StandardCharsets.UTF_8.decode(data.byteBuffer.flip()).toString();
        if (response.contains(join(requestPrefix, data.thread,data.request))) {
            System.out.println("Received: " + response);
            ++data.request;
        }
        key.interestOps(SelectionKey.OP_WRITE);
        if (data.request >= requests) {
            try {
                key.channel().close();
            } catch (IOException e) {
                System.err.println("IOException caught while closing channel. " + e.getMessage());
            }
        }
    }

    private void send(SelectionKey key) {
        if (!key.isWritable()) {
            return;
        }
        Data data = (Data) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            String request = requestPrefix + data.thread + '_' + data.request;
            System.out.println("Sent: " + request);
            channel.send(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), channel.getRemoteAddress());
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("Something get wrong while sending data. " + e.getMessage());
        }
    }
}
