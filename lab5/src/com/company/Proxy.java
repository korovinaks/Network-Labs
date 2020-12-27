package com.company;



import javafx.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Proxy {

    private static final int BUF_SIZE = 4096;

    enum Stage {FIRST, SECOND, THIRD}

    private int port;
    private Selector selector;
    private DatagramChannel dnsChannel;
    private HashMap<SocketChannel, Stage> connectionStage = new HashMap<>();
    private HashMap<SocketChannel, SocketChannel> proxyConnection = new HashMap<>();
    private DNSUtils dnsUtils = new DNSUtils();


    Proxy(int port) {
        this.port = port;
    }

    private DatagramChannel createReadDatagramSocket(InetSocketAddress bind) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.connect(bind);
        channel.register(selector, SelectionKey.OP_READ);
        return channel;
    }

    private ServerSocketChannel createServerSocket(InetSocketAddress bind) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(bind);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        return channel;
    }

    private SocketChannel createSocket(ServerSocketChannel server) throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
        return channel;
    }

    private void dnsChannelRead(SelectionKey key) throws IOException {
        System.out.println("[Debug] Received message in DNS channel: " + key.channel());
        ByteBuffer dnsBuf = ByteBuffer.allocate(1024);
        int len = dnsChannel.read(dnsBuf);
        if (len <= 0) {
            return;
        }
        Pair<SocketChannel, List<InetSocketAddress>> results = dnsUtils.handleServerResponse(dnsBuf);

        InetSocketAddress firstAddress = results.getValue().get(0);

        if (establishConnection(results.getKey(), firstAddress, key)) {
            connectionStage.replace(results.getKey(), Stage.THIRD);
        } else {
            connectionStage.remove(results.getKey());
            killChannelsOnKey(key);
        }
    }


    public void run() throws IOException {


        selector = Selector.open();

        List<InetSocketAddress> dnsServers = DNSUtils.getDNSServers();

        InetSocketAddress localAddress = new InetSocketAddress("192.168.31.155", port);

        ServerSocketChannel server = createServerSocket(localAddress);
        InetSocketAddress dnsAddress = dnsServers.get(0);
        System.out.println("[Debug] DNS address from system configuration: " + dnsAddress);

        dnsChannel = createReadDatagramSocket(dnsAddress);

        SelectionKey key;
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    System.out.println("[Debug] Finish connect for channel: " + key.channel());
                    ((SocketChannel) key.channel()).finishConnect();
                }

                if (key.isAcceptable()) {
                    System.out.println("[Debug] Accept new channel: " + key.channel());
                    SocketChannel newChannel = createSocket(server);
                    connectionStage.put(newChannel, Stage.FIRST);
                }

                if (dnsChannel.keyFor(selector) == key && key.isReadable()) {
                    dnsChannelRead(key);
                    continue;
                }

                if (key.isReadable()) {
                    System.out.println("[Debug] Read new content from channel" + key.channel());
                    buffer.clear();

                    SocketChannel channelFrom = (SocketChannel) key.channel();

                    if (connectionStage.get(channelFrom) == null) {
                        connectionStage.put(((SocketChannel) key.channel()), Stage.FIRST);
                        System.out.println("[Debug] This shouldn't happened" + key.channel());
                    }

                    switch (connectionStage.get(channelFrom)) {
                        case FIRST:
                            boolean correctFirst = SocksProtocol.getFirstMessage(channelFrom);
                            if (correctFirst) {
                                System.out.println("[Debug] Channel passed stage 1: " + key.channel());
                                SocksProtocol.sendFirstConfirmation(channelFrom);
                                connectionStage.replace(channelFrom, Stage.SECOND);
                            } else {
                                System.out.println("[Debug] Incorrect first message");
                                connectionStage.remove(channelFrom);
                                killChannelsOnKey(key);
                            }
                            buffer.clear();
                            break;
                        case SECOND:
                            SocksProtocol.SecondParseResult secondMessage = SocksProtocol.getSecondMessage(channelFrom);
                            if (secondMessage.isCorrect()) {
                                if (secondMessage.isDns()) {
                                    System.out.println("[Debug] Resolve DNS for channel: " + key.channel());
                                    String domainName = new String(secondMessage.getAddress());
                                    ByteBuffer dnsRequestBuffer = dnsUtils.prepareResolveRequest(domainName, port, channelFrom);
                                    if (dnsRequestBuffer != null) {
                                        dnsChannel.write(dnsRequestBuffer);
                                    }

                                } else {
                                    System.out.println("[Debug] Channel passed stage 2: " + key.channel());
                                    InetAddress address = InetAddress.getByAddress(secondMessage.getAddress());
                                    int port = secondMessage.getPort();
                                    if (establishConnection(channelFrom, new InetSocketAddress(address, port), key))
                                        connectionStage.replace(channelFrom, Stage.THIRD);
                                }
                            }
                            buffer.clear();
                            break;
                        case THIRD:
                            SocketChannel channelTo = proxyConnection.get(channelFrom);
                            System.out.println("[Debug] Connection from " + channelFrom.toString() + " to " + channelTo.toString());
                            if (channelTo.isConnected()) {
                                int amount;
                                try {
                                    amount = channelFrom.read(buffer);
                                    if (amount == -1) {
                                        killChannelsOnKey(key);
                                    } else {
                                        System.out.println(amount);
                                        System.out.println(Arrays.toString(buffer.array()));
                                        channelTo.write(ByteBuffer.wrap(buffer.array(), 0, amount));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("[Debug] Close connection");
                                    killChannelsOnKey(key);
                                }
                            }
                            buffer.clear();
                            break;
                    }
                }
            }
        }
    }

    private boolean establishConnection(SocketChannel channel, InetSocketAddress serverAddress, SelectionKey key) throws IOException {
        SocketChannel serverChannel = SocketChannel.open(serverAddress);
        System.out.println("[Debug] Establish connection: " + key.channel());
        if (!serverChannel.isConnected()) {
            return false;
        }
        try {
            SocksProtocol.sendSecondConfirmationMessage(channel, (short)serverAddress.getPort(), serverChannel.isConnected());
        } catch (IOException e) {
            return false;
        }
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
        proxyConnection.put(channel, serverChannel);
        proxyConnection.put(serverChannel, channel);
        connectionStage.put(serverChannel, Stage.THIRD);
        return serverChannel.isConnected();
    }

    private void killChannelsOnKey(SelectionKey key) throws IOException {
        SocketChannel channel = proxyConnection.get((SocketChannel) key.channel());
        if (channel != null) {
            channel.close();
            proxyConnection.remove(proxyConnection.get((SocketChannel) key.channel()));
            proxyConnection.remove((SocketChannel) key.channel());
        }
        key.channel().close();
    }

}