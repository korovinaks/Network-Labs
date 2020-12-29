package com.company;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CommunicationProtocol {
    private String file_name;
    private long file_size;

    public String get_file_name() {
        return file_name;
    }

    public long get_file_size() {
        return file_size;
    }

    public void get_result(DataInputStream in) {
        try {
            String result = in.readUTF();
            if (result.equals("OK")) {
                System.out.println("File send OK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send_ok_to_client(DataOutputStream out) {
        try {
            out.writeUTF("OK");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send_file_info(DataOutputStream out, String file_name, long file_size) {
        try {
            out.writeUTF(file_name);
            out.writeLong(file_size);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive(DataInputStream in) {
        try {
            file_name = in.readUTF();
            file_size = in.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
