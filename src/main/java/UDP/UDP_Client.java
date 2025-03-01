package UDP;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class UDP_Client {
    private static final String HOST = "localhost";
    private static final int PORT = 26896;
    private static final int NUM_MESSAGES = 100;
    private static final String CSV_FILE = "UDP_network_results.csv";

    public static void main(String[] args) throws IOException {
        List<Integer> messageSizes = Arrays.asList(8, 64, 256, 512);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        try (DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress serverAddress = new InetSocketAddress(HOST, PORT);

            for (int size : messageSizes) {
                latencyResults.put(size, measureLatency(channel, serverAddress, size));
                throughputResults.put(size, measureThroughput(channel, serverAddress, size));
            }
        }

        saveResultsToCSV(latencyResults, throughputResults);
        System.out.println("Results saved to " + CSV_FILE);
    }

    private static List<Long> measureLatency(DatagramChannel channel, InetSocketAddress serverAddress, int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        Arrays.fill(buffer.array(), (byte) 1);

        ByteBuffer responseBuffer = ByteBuffer.allocate(messageSize);

        for (int i = 0; i < NUM_MESSAGES; i++) {
            long startTime = System.nanoTime();

            channel.send(buffer, serverAddress);  // Send packet
            buffer.clear();

            channel.receive(responseBuffer);  // Receive response
            long endTime = System.nanoTime();

            latencies.add((endTime - startTime) / 1000); // Convert ns to µs
            responseBuffer.clear();
        }
        return latencies;
    }

    private static double measureThroughput(DatagramChannel channel, InetSocketAddress serverAddress, int messageSize) throws IOException {
        int numMessages = 1048576 / messageSize; // 1MB total data
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        Arrays.fill(buffer.array(), (byte) 1);
        ByteBuffer ackBuffer = ByteBuffer.allocate(8);

        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            channel.send(buffer, serverAddress);
            buffer.clear();
            channel.receive(ackBuffer);  // Receive acknowledgment
            ackBuffer.clear();
        }

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1e9; // Convert ns to seconds
        return (8.0 * 1048576 / duration) / 1e6; // Mbps
    }

    private static void saveResultsToCSV(Map<Integer, List<Long>> latencyResults, Map<Integer, Double> throughputResults) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
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
