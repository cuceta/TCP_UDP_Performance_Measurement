package org.example;

import java.io.*;
import java.net.*;

public class EchoClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 26892;
    private static long key = 123456789L; // Shared initial key

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            System.out.println("Connected to server: " + SERVER_IP);

            // Test latency for different message sizes
            int[] messageSizes = {8, 64, 256, 512};
            for (int size : messageSizes) {
                byte[] message = new byte[size];
                for (int i = 0; i < size; i++) {
                    message[i] = (byte) i; // Fill with sample data
                }

                // Encrypt the message
                byte[] encrypted = xorEncrypt(message, key);

                // Measure latency
                long startTime = System.nanoTime();
                out.write(encrypted);
                byte[] response = new byte[size];
                in.read(response);
                long endTime = System.nanoTime();

                // Decrypt and validate the response
                byte[] decrypted = xorEncrypt(response, key);
                if (!validateMessage(decrypted, message)) {
                    System.out.println("Validation failed for message size: " + size);
                }

                // Calculate and print latency
                long latency = (endTime - startTime) / 1_000_000; // Convert to milliseconds
                System.out.println("Latency for " + size + " bytes: " + latency + " ms");

                // Update the key
                key = xorShift(key);
            }

            // Test throughput
            testThroughput(out, in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testThroughput(OutputStream out, InputStream in) throws IOException {
        int[] messageSizes = {1024, 512, 256};
        int[] numMessages = {1024, 2048, 4096};
        long totalDataSize = 1024 * 1024; // 1 MB

        for (int i = 0; i < messageSizes.length; i++) {
            int size = messageSizes[i];
            int count = numMessages[i];
            byte[] message = new byte[size];
            for (int j = 0; j < size; j++) {
                message[j] = (byte) j; // Fill with sample data
            }

            // Measure throughput
            long startTime = System.nanoTime();
            for (int j = 0; j < count; j++) {
                byte[] encrypted = xorEncrypt(message, key);
                out.write(encrypted);
                in.read(new byte[8]); // Read acknowledgment
                key = xorShift(key);
            }
            long endTime = System.nanoTime();

            // Calculate and print throughput
            double timeTaken = (endTime - startTime) / 1_000_000_000.0; // Convert to seconds
            double throughput = (totalDataSize * 8) / timeTaken; // Throughput in bits per second
            System.out.println("Throughput for " + size + " bytes: " + throughput + " bps");
        }
    }

    private static boolean validateMessage(byte[] received, byte[] expected) {
        if (received.length != expected.length) return false;
        for (int i = 0; i < received.length; i++) {
            if (received[i] != expected[i]) return false;
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
            key = xorShift(key);
            for (int j = 0; j < 8 && i + j < data.length; j++) {
                encrypted[i + j] = (byte) (block >> (8 * j));
            }
        }
        return encrypted;
    }
}