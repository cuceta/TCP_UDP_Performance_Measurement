package TCP;

import java.io.*;
import java.net.*;

public class TCP_Server {
    private static final int PORT = 26896;
    private static final long KEY = 123456789L;

    private static long xorShift(long r) {
        r ^= (r << 13);
        r ^= (r >>> 7);
        r ^= (r << 17);
        return r;
    }

    private static byte[] encryptDecrypt(byte[] data, long key) {
        byte[] result = new byte[data.length];
        long currentKey = key;
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ (currentKey & 0xFF));
            currentKey = xorShift(currentKey);
        }
        return result;
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept(); //accepts connection
                new Thread(() -> handleClient(client)).start(); //each connection gets its own thread.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            while (true) {
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer); //read incoming
                if (bytesRead == -1) break;

                byte[] receivedData = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedData, 0, bytesRead);
                receivedData = encryptDecrypt(receivedData, KEY);//decrypt received message




                byte[] response = encryptDecrypt(receivedData, KEY); //encrypt received message
                out.write(response); //echo message
                out.flush();
            }

            client.close();
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }
}
