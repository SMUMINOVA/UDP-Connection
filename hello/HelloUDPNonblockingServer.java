package info.kgeorgiy.ja.muminova.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HelloUDPNonblockingServer extends AbstractServer {
    private Selector selector;
    private DatagramChannel channel;
    private final Queue<ByteBuffer> byteBuffers = new ConcurrentLinkedQueue<>();
    private final Queue<Data> packets = new ConcurrentLinkedQueue<>();

    static final class Data {
        ByteBuffer buffer;
        SocketAddress address;

        public Data(ByteBuffer buffer, SocketAddress address) {
            this.buffer = buffer;
            this.address = address;
        }
    }

    public static void main(String[] args) {
        new HelloUDPServer().processArgs(args);
    }

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);
            executorService = Executors.newFixedThreadPool(threads);
            int bufferSize = channel.socket().getReceiveBufferSize();
            IntStream.range(0, threads).forEach(i -> byteBuffers.add(ByteBuffer.allocate(bufferSize)));
            Executors.newSingleThreadExecutor().submit(this::startWork);
        } catch (IOException e) {
            System.err.println("Something get wrong while initializing sources. " + e.getMessage());
            close();
        }
    }

    private void startWork() {
        while (!selector.keys().isEmpty() && !Thread.interrupted()) {
            try {
                selector.select(key -> {
                    if (key.isReadable()) {
                        receive(key);
                    } else {
                        send(key);
                    }
                });
            } catch (IOException e) {
                close();
                break;
            }
        }
    }

    private void send(SelectionKey key) {
        if (packets.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        Data data = packets.poll();
        SocketAddress address = data.address;
        try {
            channel.send(data.buffer, address);
            key.interestOps(SelectionKey.OP_READ);
            key.interestOpsOr(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            System.err.println("Something get wrong while sending data. " + e.getMessage());
        }
    }

    private void receive(SelectionKey key) {
        if (byteBuffers.isEmpty()) {
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }
        ByteBuffer buffer = byteBuffers.poll();
        try {
            SocketAddress address = channel.receive(buffer.clear());
            executorService.submit(() -> {
                byte[] response = ("Hello, " + StandardCharsets.UTF_8.decode(buffer.flip())).getBytes(StandardCharsets.UTF_8);
                packets.add(new Data(ByteBuffer.wrap(response), address));
                byteBuffers.add(buffer);
                key.interestOpsOr(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        } catch (IOException e) {
            System.err.println("Something get wrong while receiving data. " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (selector != null) {
                selector.close();
            }
            if (channel != null) {
                channel.close();
            }
            shutdownExecutor();
        } catch (IOException e) {
            System.err.println("Something get wrong while closing resources. " + e.getMessage());
        }
    }
}
