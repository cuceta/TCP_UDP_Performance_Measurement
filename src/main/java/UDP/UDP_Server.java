package UDP;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDP_Server {
    private static final int PORT = 26896;
    private static final int BUFFER_SIZE = 512;
    private static int messageCounter = 0;

    public static void main(String[] args) {
        System.out.println("UDP Server is listening on port " + PORT + "...");

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(PORT));
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (true) {
                buffer.clear();
                SocketAddress clientAddress = channel.receive(buffer);

                if (clientAddress != null) {
                    messageCounter++;

                    // Send acknowledgment for every 10 messages
                    if (messageCounter % 10 == 0) {
                        ByteBuffer ackBuffer = ByteBuffer.allocate(1);
                        ackBuffer.put((byte) 1);
                        ackBuffer.flip();
                        channel.send(ackBuffer, clientAddress);
                        System.out.println("Sent batch ACK after " + messageCounter + " messages.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
