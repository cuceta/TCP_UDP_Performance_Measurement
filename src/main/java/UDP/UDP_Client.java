package UDP;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class UDP_Client {


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
        File directory = new File(OUTPUT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String csvFile = OUTPUT_DIR + "/UDP_network_results.csv";

        List<Integer> messageSizes = Arrays.asList(8, 64, 256, 512);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.connect(new InetSocketAddress(HOST, PORT));

            for (int size : messageSizes) {
                latencyResults.put(size, measureLatency(channel, size));
                throughputResults.put(size, measureThroughput(channel, size));
            }
        }

        saveResultsToCSV(latencyResults, throughputResults, csvFile);
        System.out.println("Results saved to " + csvFile);
    }

    private static List<Long> measureLatency(DatagramChannel channel, int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        Arrays.fill(buffer.array(), (byte) 1);
        byte[] encryptedData = encryptDecrypt(buffer.array(), KEY);
        buffer.put(encryptedData);
        ByteBuffer ackBuffer = ByteBuffer.allocate(1);

        for (int i = 0; i < NUM_MESSAGES; i++) {
            long startTime = System.nanoTime();
            buffer.rewind();
            channel.write(buffer);

            if ((i + 1) % 10 == 0) {
                ackBuffer.clear();
                channel.read(ackBuffer);
            }

            long endTime = System.nanoTime();
            latencies.add((endTime - startTime) / 1000);
        }
        return latencies;
    }

    private static double measureThroughput(DatagramChannel channel, int messageSize) throws IOException {
        int numMessages = 1048576 / messageSize;
        ByteBuffer buffer = ByteBuffer.allocate(messageSize);
        Arrays.fill(buffer.array(), (byte) 1);
        byte[] encryptedData = encryptDecrypt(buffer.array(), KEY);
        buffer.put(encryptedData);
        ByteBuffer ackBuffer = ByteBuffer.allocate(1);
        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            buffer.rewind();
            channel.write(buffer);

            if ((i + 1) % 10 == 0) {
                ackBuffer.clear();
                channel.read(ackBuffer);
            }
        }

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1e9;
        return (8.0 * 1048576 / duration) / 1e6;
    }

    private static void saveResultsToCSV(Map<Integer, List<Long>> latencyResults, Map<Integer, Double> throughputResults, String csvFile) throws IOException {
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
