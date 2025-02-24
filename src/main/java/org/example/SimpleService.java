package org.example;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class SimpleService {
    static final int PORT = 26893;
    private static long key = 123456789L; // Shared initial key

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("TCP Server is listening on port " + PORT + "...");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());
                handleClient(client);
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
                byte[] lengthBytes = new byte[4];
                if (!readFully(in, lengthBytes, 4)) {
                    System.out.println("Failed to read message length. Connection may be closed.");
                    break;
                }
                int messageLength = ByteBuffer.wrap(lengthBytes).getInt();
                System.out.println("Received message length: " + messageLength + " bytes");

                byte[] buffer = new byte[messageLength];
                if (!readFully(in, buffer, messageLength)) {
                    System.out.println("Incomplete message received.");
                    break;
                }
                System.out.println("Received encrypted message. Key before decryption: " + key);

                byte[] decrypted = xorEncrypt(buffer, key);
                key = xorShift(key);
                byte[] encrypted = xorEncrypt(decrypted, key);
                out.write(encrypted);
                out.flush();
                System.out.println("Sent echoed message. Key before sending: " + key);

                byte[] ack = new byte[8];
                out.write(ack);
                out.flush();
                System.out.println("Sent acknowledgment.");
                key = xorShift(key);
            }
        } catch (IOException e) {
            System.out.println("Error in client communication: " + e.getMessage());
        }
    }

    private static boolean readFully(InputStream in, byte[] buffer, int length) throws IOException {
        int bytesRead = 0;
        while (bytesRead < length) {
            int read = in.read(buffer, bytesRead, length - bytesRead);
            if (read == -1) {
                return false; // Connection closed
            }
            bytesRead += read;
        }
        return true;
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
            block ^= key;
            for (int j = 0; j < 8 && i + j < data.length; j++) {
                encrypted[i + j] = (byte) (block >> (8 * j));
            }
        }
        return encrypted;
    }
}
