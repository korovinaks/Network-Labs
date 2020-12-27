package com.company;

import javafx.util.Pair;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DNSUtils {
    private HashMap<Integer, SocketChannel> channelsMap = new HashMap<>();
    private HashMap<Integer, Integer> portsMap = new HashMap<>();

    public static List<InetSocketAddress> getDNSServers() {
        return ResolverConfig.getCurrentConfig().servers();
    }

    public Pair<SocketChannel, List<InetSocketAddress>> handleServerResponse(ByteBuffer dnsBuf) throws IOException {
        Message msg = new Message(dnsBuf.array());

        List<InetSocketAddress> inetSocketAddresses = new ArrayList<>();

        int id = msg.getHeader().getID();

        int port = portsMap.get(id);
        SocketChannel channel = channelsMap.get(id);

        List<Record> recs = msg.getSection(1);
        for (Record rec : recs) {
            if (rec instanceof ARecord) {
                ARecord aRecord = (ARecord) rec;
                InetAddress address = aRecord.getAddress();

                inetSocketAddresses.add(new InetSocketAddress(address, port));
            }
        }

        portsMap.remove(id);
        channelsMap.remove(id);

        return new Pair<>(channel, inetSocketAddresses);
    }

    public ByteBuffer prepareResolveRequest(String domainName, int port, SocketChannel channel) {
        try {
            Name name = org.xbill.DNS.Name.fromString(domainName, Name.root);
            Record rec = Record.newRecord(name, Type.A, DClass.IN);
            Message message = Message.newQuery(rec);

            portsMap.put(message.getHeader().getID(), port);
            channelsMap.put(message.getHeader().getID(), channel);

            return ByteBuffer.wrap(message.toWire());

        } catch (org.xbill.DNS.TextParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}