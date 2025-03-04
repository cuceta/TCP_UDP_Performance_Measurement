package UDP;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDP_Server {
    private static final int PORT = 26896;
    private static final int BUFFER_SIZE = 512;
    private static int messageCounter = 0;
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

    public static void main(String[] args) {
        System.out.println("UDP Server is listening on port " + PORT + "...");

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress("0.0.0.0", PORT)); // Listen on all interfaces
            channel.setOption(StandardSocketOptions.SO_RCVBUF, 1048576); // Increase receive buffer size
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (true) {
                buffer.clear();//clean buffer to prevent message corruption
                SocketAddress clientAddress = channel.receive(buffer);//store sender's address to know where to respond + save message in buffer

                if (clientAddress != null) {
                    byte[] receivedData = new byte[buffer.position()];
                    buffer.flip();//go from writing to reading mode
                    buffer.get(receivedData); //save message

                    byte[] decryptedMessage = encryptDecrypt(receivedData, KEY); // Decrypt message
                    byte[] encryptedResponse = encryptDecrypt(decryptedMessage, KEY); // Encrypt response

                    messageCounter++;

                    ByteBuffer responseBuffer = ByteBuffer.wrap(encryptedResponse); //put encrypted response in a buffer
                    channel.send(responseBuffer, clientAddress);//send message back

                    if (messageCounter % 10 == 0) { //send acknowledgments every 10 messages
                        ByteBuffer ackBuffer = ByteBuffer.allocate(1);
                        ackBuffer.put((byte) 1);
                        ackBuffer.flip();
                        channel.send(ackBuffer, clientAddress);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
