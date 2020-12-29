package com.company;


public class Main {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            Proxy proxy = new Proxy(port);
            proxy.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
