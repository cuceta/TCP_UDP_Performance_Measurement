package TCP;

import java.io.*;
import java.net.*;

public class TCP_Server {
    private static final int PORT = 26896;

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
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            long key = 123456789L; // Shared encryption key

            while (true) {
                byte[] buffer = new byte[512]; // Max message size
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) break; // Client closed connection

                byte[] receivedMessage = new byte[bytesRead];
                System.arraycopy(buffer, 0, receivedMessage, 0, bytesRead);

                byte[] decryptedMessage = encryptDecrypt(receivedMessage, key);
                byte[] encryptedResponse = encryptDecrypt(decryptedMessage, key);

                out.write(encryptedResponse);
                out.flush();
            }

            client.close();
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}
