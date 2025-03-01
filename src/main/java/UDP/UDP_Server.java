package UDP;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDP_Server {
    private static final int PORT = 26896;

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

    public static void main(String[] args) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(PORT));
            System.out.println("UDP Server listening on port " + PORT);

            ByteBuffer buffer = ByteBuffer.allocate(512);
            InetSocketAddress clientAddress;

            while (true) {
                buffer.clear();
                clientAddress = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();

                byte[] receivedData = new byte[buffer.remaining()];
                buffer.get(receivedData);

                long key = 123456789L; // Shared encryption key
                byte[] decryptedMessage = encryptDecrypt(receivedData, key);
                byte[] encryptedResponse = encryptDecrypt(decryptedMessage, key);

                // Send response
                buffer.clear();
                buffer.put(encryptedResponse);
                buffer.flip();
                channel.send(buffer, clientAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
