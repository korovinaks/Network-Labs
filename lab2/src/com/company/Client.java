package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    public Client() {

    }

    public void run(String file_name, String ip, int port) {
        try {
            Socket client = new Socket(ip, port);
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            File input_file = new File(file_name);
            long file_size = input_file.length();

            CommunicationProtocol protocol = new CommunicationProtocol();
            protocol.send_file_info(out, file_name, file_size);

            byte[] buf = new byte[4096];
            FileInputStream in_file = new FileInputStream(input_file);
            int count_bytes;
            while (true) {
                count_bytes = in_file.read(buf);
                if (count_bytes != -1) {
                    out.write(buf, 0, count_bytes);
                } else {
                    break;
                }
            }
            client.shutdownOutput();

            protocol.get_result(in);

            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
