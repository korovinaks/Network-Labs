package com.company;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeServer {

    private String node_name;
    private int port;
    private int loss_percent;
    private InetAddress parent_ip;
    private int parent_port;
    private Map<UUID, MessageObject> messages_to_deliver = new ConcurrentHashMap<>();
    private Map<UUID, MessageObject> wait_confirmation = new ConcurrentHashMap<>();
    private Map<UUID, Long> wait_confirmation_time = new ConcurrentHashMap<>();
    private Map<InetAddress, Integer> children = new ConcurrentHashMap<>();
    private Map<UUID, Long> live_time = new ConcurrentHashMap<>();
    private InetAddress replacement_ip;
    private int replacement_port;
    private InetAddress parent_replacement_ip;
    private int parent_replacement_port;

    public NodeServer(String node_name, int port, int loss_percent) {
        this.node_name = node_name;
        this.port = port;
        this.loss_percent = loss_percent;
    }

    public void set_parent(InetAddress parent_ip, int parent_port) {
        this.parent_ip = this.replacement_ip = parent_ip;
        this.parent_port = this.replacement_port = parent_port;
    }

    public int get_port() {
        return port;
    }

    public int get_loss_percent() {
        return loss_percent;
    }

    public Map.Entry<UUID, MessageObject> get_message_to_deliver() {
        if (messages_to_deliver.isEmpty()) {
            return null;
        }

        Iterator<Map.Entry<UUID, MessageObject>> iterator = messages_to_deliver.entrySet().iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        Map.Entry<UUID, MessageObject> entry = iterator.next();

        iterator.remove();

        return entry;
    }

    public void new_message(String message) {
        System.out.println(node_name + ": " + message);
        if (parent_ip != null) {
            MessageObject message_object = new MessageObject(message, parent_ip, parent_port, port, node_name, MessageObject.MessageType.TEXT_MESSAGE);
            message_object.set_replacement(replacement_ip, replacement_port);
            messages_to_deliver.put(UUID.randomUUID(), message_object);
        }

        for (Map.Entry<InetAddress, Integer> child: children.entrySet()) {
            MessageObject message_object = new MessageObject(message, child.getKey(), child.getValue(), port, node_name, MessageObject.MessageType.TEXT_MESSAGE);
            message_object.set_replacement(replacement_ip, replacement_port);
            messages_to_deliver.put(UUID.randomUUID(), message_object);
        }
    }

    public void forward_message(InetAddress ip_sender, MessageObject message) {
        if (parent_ip != null && !ip_sender.equals(parent_ip)) {
            MessageObject message_object = new MessageObject(message.get_text_message(), parent_ip, parent_port, port, message.get_sender_name(), MessageObject.MessageType.TEXT_MESSAGE);
            message_object.set_replacement(replacement_ip, replacement_port);
            messages_to_deliver.put(UUID.randomUUID(), message_object);
        }

        for (Map.Entry<InetAddress, Integer> child: children.entrySet()) {
            if (ip_sender.equals(child.getKey())) {
                continue;
            }
            MessageObject message_object = new MessageObject(message.get_text_message(), child.getKey(), child.getValue(), port, message.get_sender_name(), MessageObject.MessageType.TEXT_MESSAGE);
            message_object.set_replacement(replacement_ip, replacement_port);
            messages_to_deliver.put(UUID.randomUUID(), message_object);
        }
    }


    public void add_to_confirmation_map(UUID id, MessageObject message) {
        if (message.get_type() == MessageObject.MessageType.CONFIRMATION_MESSAGE) {
            return;
        }

        Long current_time = System.currentTimeMillis();

        wait_confirmation.put(id, message);
        wait_confirmation_time.put(id, current_time);

        if (!live_time.containsKey(id)) {
            live_time.put(id, current_time);
        }
    }

    public void new_message_received(UUID id, MessageObject message, InetAddress ip_sender, int port_sender) {
        //System.out.println("[DEBUG] Message received: " + id + ", ip_sender = " + ip_sender);
        if (!ip_sender.equals(parent_ip)) {
            children.put(ip_sender, port_sender);
            //System.out.println("[DEBUG] Ip added to child list: " + ip_sender + ", port " + port_sender);
        }

        if (parent_ip == null && replacement_ip == null) {
            //System.out.println("[DEBUG] Replacement ip set to " + ip_sender + " " + port_sender);
            replacement_ip = ip_sender;
            replacement_port = port_sender;
        }

        if (parent_ip != null && ip_sender.equals(parent_ip)) {
            //System.out.println("[DEBUG] Parent replacement ip set to " + message.get_ip_replacement());
            parent_replacement_ip = message.get_ip_replacement();
            parent_replacement_port = message.get_port_replacement();
        }

        if(message.get_type() == MessageObject.MessageType.TEXT_MESSAGE) {
            System.out.println(message.get_sender_name() + ": " + message.get_text_message());

            int port_target = message.get_port_sender();
            InetAddress ip_target = ip_sender;
            MessageObject confirmation_message = new MessageObject("OK", ip_target, port_target, port, node_name, MessageObject.MessageType.CONFIRMATION_MESSAGE);
            confirmation_message.set_replacement(replacement_ip, replacement_port);
            messages_to_deliver.put(id, confirmation_message);

            forward_message(ip_sender, message);
        }

        if(message.get_type() == MessageObject.MessageType.CONFIRMATION_MESSAGE) {
            wait_confirmation.remove(id);
            wait_confirmation_time.remove(id);
            live_time.remove(id);
        }
    }

    public void check_old_confirmations(long wait_period) {
        for (Iterator<HashMap.Entry<UUID, Long>> iterator = wait_confirmation_time.entrySet().iterator(); iterator.hasNext();) {
            HashMap.Entry<UUID, Long> entry = iterator.next();
            UUID id = entry.getKey();
            if (entry.getValue() + wait_period < System.currentTimeMillis()) {
                messages_to_deliver.put(id, wait_confirmation.get(id));

                iterator.remove();
                wait_confirmation.remove(id);
            }
        }
    }

    public void check_live(long live_period) {
        for (Iterator<HashMap.Entry<UUID, Long>> iterator = live_time.entrySet().iterator(); iterator.hasNext();) {
            HashMap.Entry<UUID, Long> entry = iterator.next();
            UUID id = entry.getKey();
            if (entry.getValue() + live_period < System.currentTimeMillis()) {
                //System.out.println("[DEBUG] Found dead UUID " + id);
                MessageObject message = wait_confirmation.get(id);
                if (message != null) {
                    InetAddress dead_ip = message.get_ip_target();
                    if (dead_ip.equals(parent_ip)) {
                        //System.out.println("[DEBUG] Replacing parent to " + parent_replacement_ip + " : " + parent_replacement_port);
                        set_parent(parent_replacement_ip, parent_replacement_port);
                    } else {
                        //System.out.println("[DEBUG] Removing child: " + dead_ip);
                        children.remove(dead_ip);
                    }
                    iterator.remove();
                }
            }
        }
    }

    public void run() {
        new ReadMessagesThread(this).start();
        new SendMessagesThread(this).start();
        new ReceiveMessagesThread(this).start();
        new MonitorConfirmationsThread(this).start();
    }
}
