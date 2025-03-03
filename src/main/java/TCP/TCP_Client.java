package TCP;

import java.io.*;
import java.net.*;
import java.util.*;

public class TCP_Client {
//    private static final String HOST = "localhost";
    private static final String HOST = "pi.oswego.edu";
//    private static final String HOST = "rho.oswego.edu";
//    private static final String HOST = "gee.oswego.edu";


    private static final int PORT = 26896;
    private static final int NUM_MESSAGES = 100;
//    private static String OUTPUT_DIR = "local-local";
    private static String OUTPUT_DIR = "local-pi";
//    private static String OUTPUT_DIR = "pi-rho";
//    private static String OUTPUT_DIR = "rho-gee";

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

    public static void main(String[] args) throws IOException {
        String csvFile = OUTPUT_DIR + "/TCP_network_results.csv";
        List<Integer> messageSizes = Arrays.asList(8, 64, 256, 512);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        System.out.println("Starting TCP Client... Connecting to " + HOST + ":" + PORT);

        for (int size : messageSizes) {
            latencyResults.put(size, measureLatency(size));
        }

        for (int size : messageSizes) {
            throughputResults.put(size, measureThroughput(size));
        }

        saveResultsToCSV(csvFile, latencyResults, throughputResults);
        System.out.println("Results saved to " + csvFile);
    }

    private static List<Long> measureLatency(int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);
        message = encryptDecrypt(message, KEY); // Encrypt the message

        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        for (int i = 0; i < NUM_MESSAGES; i++) {
            long startTime = System.nanoTime();
            out.write(message);
            out.flush();
            byte[] response = new byte[messageSize];
            in.readFully(response);
            response = encryptDecrypt(response, KEY); // Decrypt response
            long endTime = System.nanoTime();
            latencies.add((endTime - startTime) / 1000);
        }

        socket.close();
        return latencies;
    }

    private static double measureThroughput(int messageSize) throws IOException {
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        int numMessages = 1048576 / messageSize;
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);
        message = encryptDecrypt(message, KEY); // Encrypt the message

        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            out.write(message);
            out.flush();
            byte[] response = new byte[8];
            in.readFully(response);
            response = encryptDecrypt(response, KEY); // Decrypt response
        }

        long endTime = System.nanoTime();
        socket.close();

        double duration = (endTime - startTime) / 1e9;
        return (8.0 * 1048576 / duration) / 1e6;
    }

    private static void saveResultsToCSV(String csvFile, Map<Integer, List<Long>> latencyResults, Map<Integer, Double> throughputResults) throws IOException {
        System.out.println("Saving results to CSV...");
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
        System.out.println("CSV file saved successfully.");
    }
}




