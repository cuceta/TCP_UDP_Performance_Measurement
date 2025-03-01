package TCP;

import java.io.*;
import java.net.*;
import java.util.*;

public class EchoClient {
    private static final String HOST = "localhost"; // Replace with actual server IP
    private static final int PORT = 26896;
    private static final int NUM_MESSAGES = 100; // Number of latency test messages per size
    private static final String CSV_FILE = "network_results.csv";

    public static void main(String[] args) throws IOException {
        List<Integer> messageSizes = Arrays.asList(8, 64, 256, 512);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        // Measure Latency
        for (int size : messageSizes) {
            latencyResults.put(size, measureLatency(size));
        }

        // Measure Throughput
        for (int size : messageSizes) {
            throughputResults.put(size, measureThroughput(size));
        }

        // Save to CSV
        saveResultsToCSV(latencyResults, throughputResults);
        System.out.println("Results saved to " + CSV_FILE);
    }

    private static List<Long> measureLatency(int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);

        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        for (int i = 0; i < NUM_MESSAGES; i++) {
            long startTime = System.nanoTime();
            out.write(message);
            out.flush();
            in.readFully(new byte[messageSize]); // Read echo back
            long endTime = System.nanoTime();
            latencies.add((endTime - startTime) / 1000); // Convert ns to µs
        }

        socket.close();
        return latencies;
    }

    private static double measureThroughput(int messageSize) throws IOException {
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        int numMessages = 1048576 / messageSize; // 1MB total data
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);

        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            out.write(message);
            out.flush();
            in.readFully(new byte[8]); // Read acknowledgment
        }

        long endTime = System.nanoTime();
        socket.close();

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
