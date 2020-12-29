package com.company;

public class MonitorConfirmationsThread extends Thread {

    private NodeServer node_server;
    private static final long wait_period = 5000;
    private static final long sleep_period = 500;
    private static final long live_period = 15000;

    public MonitorConfirmationsThread(NodeServer node_server) {
        this.node_server = node_server;
    }

    @Override
    public void run() {
        while (true)  {
            try {
                node_server.check_old_confirmations(wait_period);
                node_server.check_live(live_period);
                sleep(sleep_period);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
