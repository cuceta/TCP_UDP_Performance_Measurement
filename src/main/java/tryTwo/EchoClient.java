package tryTwo;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class EchoClient {
    private static final String HOST = "localhost";
    private static final int PORT = 26895;
    private static final int NUM_SAMPLES = 100; // Number of messages per size for averaging

    public static void main(String[] args) {
        long key = 123456789L; // Initial encryption key (should be shared with server)

        try {
            System.out.println("Measuring Latency...");
            System.out.println("Latency for 8 bytes: " + measureLatency(8, NUM_SAMPLES, key) + " µs");
            System.out.println("Latency for 64 bytes: " + measureLatency(64, NUM_SAMPLES, key) + " µs");
            System.out.println("Latency for 256 bytes: " + measureLatency(256, NUM_SAMPLES, key) + " µs");
            System.out.println("Latency for 512 bytes: " + measureLatency(512, NUM_SAMPLES, key) + " µs");

            System.out.println("\nMeasuring Throughput...");
            measureThroughput(1024, key);
            measureThroughput(512, key);
            measureThroughput(256, key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Measures the average round-trip latency for messages of a given size.
     */
    private static double measureLatency(int messageSize, int numSamples, long key) throws IOException {
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1); // Fill message with dummy data
        long totalLatency = 0;

        for (int i = 0; i < numSamples; i++) {
            long startTime = System.nanoTime(); // Start timer

            out.write(encryptDecrypt(message, key));
            out.flush(); // Ensure immediate sending

            byte[] response = new byte[messageSize];
            in.readFully(response); // Read echoed response

            long endTime = System.nanoTime(); // Stop timer
            totalLatency += (endTime - startTime); // Accumulate latency
        }

        socket.close();
        return (totalLatency / numSamples) / 1000.0; // Convert to microseconds
    }

    /**
     * Measures throughput by sending 1MB of data in chunks of the given message size.
     */
    private static void measureThroughput(int messageSize, long key) throws IOException {
        Socket socket = new Socket(HOST, PORT);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        int numMessages = 1048576 / messageSize; // 1MB total data
        byte[] message = new byte[messageSize];
        Arrays.fill(message, (byte) 1);

        long startTime = System.nanoTime();

        for (int i = 0; i < numMessages; i++) {
            out.write(encryptDecrypt(message, key));
            out.flush();
            in.readFully(new byte[8]); // Read small 8-byte acknowledgment
        }

        long endTime = System.nanoTime();
        socket.close();

        double duration = (endTime - startTime) / 1e9; // Convert ns to seconds
        double throughputMbps = (8.0 * 1048576 / duration) / 1e6; // Convert bits to Mbps

        System.out.printf("Throughput for %d bytes: %.2f Mbps%n", messageSize, throughputMbps);
    }

    /**
     * Simple XOR-based encryption/decryption with xorshift key update.
     */
    private static byte[] encryptDecrypt(byte[] data, long key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ (key & 0xFF)); // XOR with lowest byte of key
            key = xorShift(key); // Update key
        }
        return result;
    }

    private static long xorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }
}
