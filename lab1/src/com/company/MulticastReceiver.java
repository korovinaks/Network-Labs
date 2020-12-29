package com.company;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class MulticastReceiver {
    private final static int CLEAN_UP_PERIOD = 3000;
    MulticastSocket receiver;
    protected int port;
    protected InetAddress group;
    HashMap<String, Long> live_ip = new HashMap<String, Long>();
    boolean live_ip_changed;

    public MulticastReceiver(InetAddress group, int port) throws IOException {
        this.port = port;
        this.group = group;
        receiver = new MulticastSocket(port);
        receiver.joinGroup(group);
        receiver.setSoTimeout(1);
    }

    private void print() {
        System.out.println("Current IP list :");
        for (HashMap.Entry<String, Long> tmp: live_ip.entrySet()) {
            System.out.println(tmp.getKey());
        }
    }

    public void read() {
        byte[] buf = new byte[1024];

        live_ip_changed = false;

        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            receiver.receive(packet);

            if (!live_ip.containsKey(packet.getAddress().toString())) {
                live_ip_changed = true;
            }
            live_ip.put(packet.getAddress().toString(), new Date().getTime());

            for (Iterator<HashMap.Entry<String, Long>> iterator = live_ip.entrySet().iterator(); iterator.hasNext(); ) {
                HashMap.Entry<String, Long> entry = iterator.next();
                if (entry.getValue() + CLEAN_UP_PERIOD < new Date().getTime()) {
                    iterator.remove();
                    live_ip_changed = true;
                }
            }
            if (live_ip_changed) {
                print();
            }
        } catch (SocketTimeoutException e) {
            return;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
