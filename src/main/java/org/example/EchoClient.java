package org.example;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class EchoClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 26892;
    private static long key = 123456789L; // Shared initial key

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) {
            socket.setSoTimeout(5000); // 5-second timeout
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            System.out.println("Connected to server: " + SERVER_IP);

            // Test latency for different message sizes
            testLatency(out, in);

            // Test throughput
            testThroughput(out, in);
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timeout: Server did not respond in time");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Program interrupted: " + e.getMessage());
        }
    }

    private static void testLatency(OutputStream out, InputStream in) throws IOException {
        int[] messageSizes = {8, 64, 256, 512};
        int numIterations = 10; // Number of iterations to average latency

        for (int size : messageSizes) {
            long totalLatency = 0;

            for (int i = 0; i < numIterations; i++) {
                byte[] message = new byte[size];
                for (int j = 0; j < size; j++) {
                    message[j] = (byte) j; // Fill with sample data
                }

                // Encrypt the message
                byte[] encrypted = xorEncrypt(message, key);

                // Send message length
                byte[] lengthBytes = ByteBuffer.allocate(4).putInt(encrypted.length).array();
                out.write(lengthBytes);

                // Send encrypted message
                out.write(encrypted);
                out.flush();

                // Update the key
                key = xorShift(key);

                // Measure latency
                long startTime = System.nanoTime();

                // Receive echoed message
                byte[] response = new byte[encrypted.length];
                int bytesRead = 0;
                while (bytesRead < response.length) {
                    int read = in.read(response, bytesRead, response.length - bytesRead);
                    if (read == -1) {
                        System.out.println("Connection closed by server");
                        return;
                    }
                    bytesRead += read;
                }

                long endTime = System.nanoTime();

                // Decrypt the response
                byte[] decrypted = xorEncrypt(response, key);

                // Update the key
                key = xorShift(key);

                // Validate the response
                if (!validateMessage(decrypted, message)) {
                    System.out.println("Validation failed for message size: " + size);
                    return;
                }

                // Accumulate latency
                totalLatency += (endTime - startTime);
            }

            // Calculate and print average latency in nanoseconds
            long averageLatency = totalLatency / numIterations;
            System.out.println("Average latency for " + size + " bytes: " + averageLatency + " ns");
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

            System.out.println("Testing throughput for message size: " + size + " bytes");

            // Measure throughput
            long startTime = System.nanoTime();
            for (int j = 0; j < count; j++) {
                System.out.println("Sending message " + (j + 1) + " of " + count);

                // Encrypt the message
                byte[] encrypted = xorEncrypt(message, key);

                // Send message length
                byte[] lengthBytes = ByteBuffer.allocate(4).putInt(encrypted.length).array();
                out.write(lengthBytes);

                // Send encrypted message
                out.write(encrypted);
                out.flush();

                // Update the key
                key = xorShift(key);

                // Read acknowledgment (8 bytes)
                byte[] ack = new byte[8];
                int bytesRead = 0;
                while (bytesRead < ack.length) {
                    int read = in.read(ack, bytesRead, ack.length - bytesRead);
                    if (read == -1) {
                        System.out.println("Connection closed by server");
                        return;
                    }
                    bytesRead += read;
                }
                System.out.println("Received acknowledgment for message " + (j + 1));

                // Update the key
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
            block ^= key; // XOR with the key
            for (int j = 0; j < 8 && i + j < data.length; j++) {
                encrypted[i + j] = (byte) (block >> (8 * j));
            }
        }
        return encrypted;
    }
}