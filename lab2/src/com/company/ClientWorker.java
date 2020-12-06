package com.company;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;

public class ClientWorker implements Runnable {
    private DataInputStream in;
    private DataOutputStream out;
    private long total_bytes = 0;
    private long last_check_bytes = 0;
    private double start_time;
    private double last_check_time;
    private static final double check_interval = 3000;

    public ClientWorker(Socket client) {
        try {
            in = new DataInputStream(client.getInputStream());
            out = new DataOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void create_folder() {
        File file = new File("./uploads");
        file.mkdirs();
    }

    private void on_data_received(int count_bytes) {
        double current_time = System.currentTimeMillis();
        if (count_bytes == -1) {
            double average_speed = total_bytes / ((current_time - start_time) / 1000);
            System.out.println("Average speed = " + new DecimalFormat("#0.00").format(average_speed) + " B/s");

            return;
        }

        total_bytes += count_bytes;
        if (current_time - last_check_time >= check_interval) {
            double current_speed = (total_bytes - last_check_bytes) / ((current_time - last_check_time) / 1000);
            double average_speed = total_bytes / ((current_time - start_time) / 1000);
            System.out.println("Current speed = " + new DecimalFormat("#0.00").format(current_speed) + " B/s");
            System.out.println("Average speed = " + new DecimalFormat("#0.00").format(average_speed) + " B/s");

            last_check_bytes = total_bytes;
            last_check_time = current_time;
        }
    }

    private void process_file(DataInputStream in, FileOutputStream out_file) {
        byte[] buf = new byte[4096];
        int count_read_bytes = 0;

        start_time = System.currentTimeMillis();
        last_check_time = System.currentTimeMillis();
        try {
            while (true) {
                count_read_bytes = in.read(buf);
                on_data_received(count_read_bytes);

                if (count_read_bytes == -1) {
                    break;
                }
                out_file.write(buf, 0, count_read_bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            CommunicationProtocol protocol = new CommunicationProtocol();

            protocol.receive(in);

            System.out.println("FileName: " + protocol.get_file_name());
            System.out.println("FileSize: " + protocol.get_file_size() + " B");

            create_folder();
            File out_file_name = new File(protocol.get_file_name());

            FileOutputStream out_file = new FileOutputStream("./uploads/" + out_file_name.getName());
            process_file(in, out_file);
            out_file.close();

            if (protocol.get_file_size() == total_bytes) {
                protocol.send_ok_to_client(out);
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
