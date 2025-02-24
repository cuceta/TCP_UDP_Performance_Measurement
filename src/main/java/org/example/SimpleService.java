package org.example;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class SimpleService {
    static final int PORT = 26892;
    private static long key = 123456789L; // Shared initial key

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("TCP Server is listening on port " + PORT + "...");

            for (;;) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                // Handle client in a new thread
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private static void handleClient(Socket client) {
        try (InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {
            while (true) {
                // Read message length
                byte[] lengthBytes = new byte[4];
                int bytesRead = in.read(lengthBytes);
                if (bytesRead != 4) {
                    System.out.println("Failed to read message length");
                    break;
                }
                int messageLength = ByteBuffer.wrap(lengthBytes).getInt();
                System.out.println("Received message length: " + messageLength + " bytes");

                // Read the exact number of bytes
                byte[] buffer = new byte[messageLength];
                bytesRead = in.read(buffer);
                if (bytesRead != messageLength) {
                    System.out.println("Incomplete message received");
                    break;
                }
                System.out.println("Received encrypted message");

                // Decrypt the message
                byte[] decrypted = xorEncrypt(buffer, key);
                System.out.println("Decrypted message: " + new String(decrypted));

                // Update the key
                key = xorShift(key);

                // Encrypt and echo back the message
                byte[] encrypted = xorEncrypt(decrypted, key);
                out.write(encrypted);
                out.flush();
                System.out.println("Sent echoed message");

                // Send acknowledgment (8 bytes)
                byte[] ack = new byte[8];
                out.write(ack);
                out.flush();
                System.out.println("Sent acknowledgment for message");

                // Update the key
                key = xorShift(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static long xorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    private static byte[] xorEncrypt(byte[] data, long key) {
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i += 8) {
            long block = 0;
            for (int j = 0; j < 8 && i + j < data.length; j++) {
                block |= ((long) data[i + j] & 0xFF) << (8 * j);
            }
            block ^= key; // XOR with the key
            for (int j = 0; j < 8 && i + j < data.length; j++) {
                encrypted[i + j] = (byte) (block >> (8 * j));
            }
        }
        return encrypted;
    }
}