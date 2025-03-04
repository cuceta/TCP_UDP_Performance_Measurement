package TCP;

import java.io.*;
import java.net.*;
import java.util.*;

public class TCP_Client {
//    private static final String HOST = "localhost";
//    private static final String HOST = "pi.cs.oswego.edu";
//    private static final String HOST = "rho.cs.oswego.edu";
    private static final String HOST = "moxie.cs.oswego.edu";

    private static final int PORT = 26896;


//        private static final String OUTPUT_DIR = System.getProperty("user.home") + "/Documents/GitHub/TCP_UDP_Perfomance_Measurement/local-local";
//    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/Documents/GitHub/TCP_UDP_Perfomance_Measurement/local-pi";
//    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/Desktop/TCP_UDP_Perfomance_Measurement/pi-rho";
    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/Desktop/TCP_UDP_Perfomance_Measurement/rho-moxie";


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
        List<Integer> latencySizes = Arrays.asList(8, 64, 256, 512);
        List<Integer> throughputSizes = Arrays.asList(1024, 512, 256);
        Map<Integer, List<Long>> latencyResults = new HashMap<>();
        Map<Integer, Double> throughputResults = new HashMap<>();

        System.out.println("Starting TCP Client... Connecting to " + HOST + ":" + PORT);

        for (int size : latencySizes) {
            latencyResults.put(size, measureLatency(size));
        }

        for (int size : throughputSizes) {
            throughputResults.put(size, measureThroughput(size));
        }

        System.out.println("Saving results to: " + csvFile);
        saveResultsToCSV(csvFile, latencyResults, throughputResults);
    }

    private static List<Long> measureLatency(int messageSize) throws IOException {
        List<Long> latencies = new ArrayList<>();

        //prepare message to be sent
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);
        message = encryptDecrypt(message, KEY);

        //open connection
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        for (int i = 0; i < 100; i++) {
            long startTime = System.nanoTime();//start time

            //send message
            out.write(message);
            out.flush();
            byte[] response = new byte[messageSize];
            in.readFully(response);
            response = encryptDecrypt(response, KEY);

            long endTime = System.nanoTime(); //stop time
            latencies.add((endTime - startTime) / 1000); //latency in microseconds
        }

        socket.close(); //stop time
        return latencies;
    }

    private static double measureThroughput(int messageSize) throws IOException {
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        int numMessages = 1048576 / messageSize; //figure out how many messages

        //prepare message to be sent
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);
        message = encryptDecrypt(message, KEY);

        long startTime = System.nanoTime();//start time

        //Send message
        for (int i = 0; i < numMessages; i++) {
            out.write(message);
            out.flush();
            byte[] response = new byte[messageSize];
            in.readFully(response);
            response = encryptDecrypt(response, KEY);
        }

        long endTime = System.nanoTime();//stop time
        socket.close(); //stop connection
        return (8.0 * 1048576 / ((endTime - startTime) / 1e9)) / 1_000_000; //Throughput in Mbps
    }

    private static void saveResultsToCSV(String csvFile, Map<Integer, List<Long>> latencyResults, Map<Integer, Double> throughputResults) throws IOException {
        File file = new File(csvFile);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Failed to create directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

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