package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class MulticastReceiver extends Thread {
    private final static int clean_up_period = 3000;
    protected int port;
    protected InetAddress group;
    HashMap<String, Long> live_ip = new HashMap<String, Long>();

    public MulticastReceiver(InetAddress group, int port) {
        this.port = port;
        this.group = group;
    }

    private void print() {
        System.out.println("Current IP list :");
        for (HashMap.Entry<String, Long> tmp: live_ip.entrySet()) {
            System.out.println(tmp.getKey());
        }
    }

    public void run() {
        byte[] buf = new byte[1024];
        boolean live_ip_changed;

        while (true) {
            live_ip_changed = false;
            try {
                MulticastSocket receiver = new MulticastSocket(port);
                receiver.joinGroup(group);

                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiver.receive(packet);

                if (!live_ip.containsKey(packet.getAddress().toString())) {
                    live_ip_changed = true;
                }
                live_ip.put(packet.getAddress().toString(), new Date().getTime());

                for (Iterator<HashMap.Entry<String, Long>> iterator = live_ip.entrySet().iterator(); iterator.hasNext();) {
                    HashMap.Entry<String, Long> entry = iterator.next();
                    if (entry.getValue() + clean_up_period < new Date().getTime()) {
                        iterator.remove();
                        live_ip_changed = true;
                    }
                }


                if (live_ip_changed) {
                    print();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
