package UDP;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class UDP_Client {
    //    private static final String HOST = "localhost";
    private static final String HOST = "pi.cs.oswego.edu";
//    private static final String HOST = "rho.cs.oswego.edu";
//    private static final String HOST = "gee.cs.oswego.edu";

    private static final int PORT = 26896;
    //    private static final String OUTPUT_DIR = "local-local";
    private static final String OUTPUT_DIR = "local-pi";
//    private static final String OUTPUT_DIR = "pi-rho";
//    private static final String OUTPUT_DIR = "rho-gee";

    private static final long KEY = 123456789L;
    private static final int NUM_LATENCY_MESSAGES = 100;

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

    public static void main(String[] args) throws IOException {
        String csvFile = OUTPUT_DIR + "/UDP_network_results.csv";
        List<Integer> latencySizes = Arrays.asList(8, 64, 256, 512);
        List<Integer> throughputSizes = Arrays.asList(1024, 512, 256);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.connect(new InetSocketAddress(HOST, PORT));

            for (int size : latencySizes) {
                System.out.println("Measuring latency for message size: " + size + " bytes");
                latencyResults.put(size, measureLatency(channel, size));
            }

            for (int size : throughputSizes) {
                System.out.println("Measuring throughput for message size: " + size + " bytes");
                throughputResults.put(size, measureThroughput(channel, size));
            }
        }

        saveResultsToCSV(csvFile, latencyResults, throughputResults);
        System.out.println("Results saved to " + csvFile);
    }

    private static List<Long> measureLatency(DatagramChannel channel, int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        byte[] data = new byte[messageSize];
        Arrays.fill(data, (byte) 1);
        data = encryptDecrypt(data, KEY);
        buffer.put(data);
        ByteBuffer responseBuffer = ByteBuffer.allocate(messageSize);

        for (int i = 0; i < NUM_LATENCY_MESSAGES; i++) {
            long startTime = System.nanoTime();
            buffer.rewind();
            channel.write(buffer);
            responseBuffer.clear();
            channel.read(responseBuffer);
            long endTime = System.nanoTime();
            latencies.add((endTime - startTime) / 1000);
            System.out.println("Latency measurement " + (i + 1) + " of " + NUM_LATENCY_MESSAGES);
        }
        return latencies;
    }

    private static double measureThroughput(DatagramChannel channel, int messageSize) throws IOException {
        int numMessages;
        switch (messageSize) {
            case 1024:
                numMessages = 1024;
                break;
            case 512:
                numMessages = 2048;
                break;
            case 256:
                numMessages = 4096;
                break;
            default:
                throw new IllegalArgumentException("Invalid message size for throughput test");
        }

        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        byte[] data = new byte[messageSize];
        for (int i = 0; i < messageSize; i++) {
            data[i] = (byte) (i % 256);
        }
        data = encryptDecrypt(data, KEY);
        buffer.put(data);
        ByteBuffer ackBuffer = ByteBuffer.allocate(8);
        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            buffer.rewind();
            channel.write(buffer);
            ackBuffer.clear();
            channel.read(ackBuffer);
            if ((i + 1) % (numMessages / 10) == 0) {
                System.out.println("Throughput progress: " + ((i + 1) * 100 / numMessages) + "%");
            }
        }

        long endTime = System.nanoTime();
        return (8.0 * 1048576 / ((endTime - startTime) / 1e9));
    }

    private static void saveResultsToCSV(String csvFile, Map<Integer, List<Long>> latencyResults, Map<Integer, Double> throughputResults) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("Message Size,Message Number,Latency (µs)");
            for (int size : latencyResults.keySet()) {
                List<Long> latencies = latencyResults.get(size);
                for (int i = 0; i < latencies.size(); i++) {
                    writer.printf("%d,%d,%d%n", size, i + 1, latencies.get(i));
                }
            }

            writer.println("\nMessage Size,Throughput (Mbps)");
            for (int size : throughputResults.keySet()) {
                writer.printf("%d,%.2f%n", size, throughputResults.get(size));
            }
        }
    }
}
