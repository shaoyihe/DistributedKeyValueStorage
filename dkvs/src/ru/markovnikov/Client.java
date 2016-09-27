package ru.markovnikov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by nikita on 29.05.16.
 */
public class Client {
    public static void main(String[] args) {
        int id = 0;
        if (args.length != 0)
            id = Integer.parseInt(args[0]);
        int[] ports = new int[]{5454, 5455, 5456};
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            int port = ports[id];
            Socket socket = new Socket();
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            socket.connect(address);
            System.out.println("connected: " + port);
            OutputStreamWriter socketWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            InputStreamReader socketReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(socketReader);
            while (true) {
                String request = reader.readLine();
                System.out.println("request: " + request);
                if (request == null) {
                    socketWriter.close();
                    return;
                }
                if (request.equals("set h 123"))
                    System.out.println("response: VALUE 123");
                socketWriter.write(request + "\n");
                socketWriter.flush();

                String response = bufferedReader.readLine();
                System.out.println("response: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
