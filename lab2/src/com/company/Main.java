package com.company;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify arguments!");
            return;
        }

	    if (args[0].equals("client")) {
	        if (args.length == 4) {
	            Client client = new Client();
	            client.run(args[1], args[2], Integer.parseInt(args[3]));
            }
        }

	    if (args[0].equals("server")) {
	        if (args.length == 2) {
	            Server server = new Server(Integer.parseInt(args[1]));
	            server.run();
            }
        }
    }
}
