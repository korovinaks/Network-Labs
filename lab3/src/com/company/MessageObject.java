package com.company;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public class MessageObject implements Serializable {
    private String text_message;
    private InetAddress ip_target;
    private int port_target;
    private int port_sender;
    private MessageType type;
    private String sender_name;
    private InetAddress ip_replacement;
    private int port_replacement;

    public enum MessageType {
        TEXT_MESSAGE,
        CONFIRMATION_MESSAGE
    }

    public MessageObject(String text_message, InetAddress ip_target, int port_target, int port_sender, String sender_name, MessageType type) {
        this.text_message = text_message;
        this.ip_target = ip_target;
        this.port_target = port_target;
        this.port_sender = port_sender;
        this.type = type;
        this.sender_name = sender_name;
    }

    public void set_replacement(InetAddress ip_replacement, int port_replacement) {
        this.ip_replacement = ip_replacement;
        this.port_replacement = port_replacement;
    }

    public InetAddress get_ip_target() {
        return ip_target;
    }

    public int get_port_target() {
        return port_target;
    }

    public MessageType get_type() {
        return type;
    }

    public int get_port_sender() {
        return port_sender;
    }

    public String get_sender_name() {
        return sender_name;
    }

    public String get_text_message() {
        return text_message;
    }

    public InetAddress get_ip_replacement() {
        return ip_replacement;
    }

    public int get_port_replacement() {
        return port_replacement;
    }
}
