package org.telecom.slr.mqtt.client;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class MqttClient implements AutoCloseable {
    private final String host;
    private final String clientId;
    private final int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    private MqttClient(String host, int port, String clientId) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
    }

    public void publish(String topic, String message, int qos) throws IOException {
        byte[] topicBytes = topic.getBytes();
        byte[] messageBytes = message.getBytes();

        byte topicLengthMSB = (byte) (topicBytes.length >> 8);
        byte topicLengthLSB = (byte) (topicBytes.length);

        int length = 2 + topicBytes.length + messageBytes.length;
        int packetId = 0;

        // **QoS 1 and 2 need a Packet Identifier**
        if (qos > 0) {
            packetId = IdentityGenerator.generate();
            length += 2; // Add 2 bytes for Packet Identifier
        }

        byte[] packet = new byte[length + 2];

        packet[0] = (byte) (0x30 | (qos << 1)); // QoS 0 → `0x30`, QoS 1 → `0x32`, QoS 2 → `0x34`
        packet[1] = (byte) length;

        packet[2] = topicLengthMSB;
        packet[3] = topicLengthLSB;
        System.arraycopy(topicBytes, 0, packet, 4, topicBytes.length);

        int index = 4 + topicBytes.length;
        if (qos > 0) {
            byte packetIdMSB = (byte) (packetId >> 8);
            byte packetIdLSB = (byte) (packetId);
            packet[index++] = packetIdMSB;
            packet[index++] = packetIdLSB;
        }

        System.arraycopy(messageBytes, 0, packet, index, messageBytes.length);

        print("%n[PUBLISH] [%d, %s] with Qos=%d at %s",packetId, message, qos, new Date());

        out.write(packet);
        out.flush();
    }

    private void sendCONNECT() throws IOException {
        byte[] protocolName = {0x00, 0x04, 'M', 'Q', 'T', 'T'};
        byte protocolLevel = 0x04;  // MQTT 3.1.1
        byte connectFlags = 0x02;   // Clean session
        byte keepAliveMSB = 0x00;
        byte keepAliveLSB = 0x3C;   // 60 seconds

        byte[] clientIdBytes = this.clientId.getBytes();
        byte clientIdLengthMSB = (byte) (clientIdBytes.length >> 8);
        byte clientIdLengthLSB = (byte) (clientIdBytes.length);

        int length = protocolName.length + 4 + 2 + clientIdBytes.length;
        byte[] packet = new byte[length + 2];

        packet[0] = 0x10; // CONNECT packet
        packet[1] = (byte) length;
        System.arraycopy(protocolName, 0, packet, 2, protocolName.length);
        packet[8] = protocolLevel;
        packet[9] = connectFlags;
        packet[10] = keepAliveMSB;
        packet[11] = keepAliveLSB;
        packet[12] = clientIdLengthMSB;
        packet[13] = clientIdLengthLSB;
        System.arraycopy(clientIdBytes, 0, packet, 14, clientIdBytes.length);

        this.out.write(packet);
        this.out.flush();

        System.out.printf("[CONNECT] at %s", new Date());
    }

    private void sendDISCONNECT() throws IOException {
        byte[] disconnectPacket = {(byte) 0xE0, 0x00};
        out.write(disconnectPacket);
        out.flush();
        System.out.printf("%n[DISCONNECT] at %s", new Date());
    }

    public static MqttClient connect(String host, int port, String clientId) throws IOException {
        MqttClient client = new MqttClient(host, port, clientId);
        client.connect();
        return client;
    }

    public void subscribe(String topic, int qos) throws IOException {
        byte[] topicBytes = topic.getBytes();

        byte topicLengthMSB = (byte) (topicBytes.length >> 8);
        byte topicLengthLSB = (byte) (topicBytes.length);

        int packetId = IdentityGenerator.generate();
        byte packetIdMSB = (byte) (packetId >> 8);
        byte packetIdLSB = (byte) (packetId);

        byte[] packet = new byte[7 + topicBytes.length];

        // fixed header
        packet[0] = (byte) 0x82;
        packet[1] = (byte) (packet.length - 2);

        // variable header
        packet[2] = packetIdMSB;
        packet[3] = packetIdLSB;

        // payload topic and qos
        packet[4] = topicLengthMSB;
        packet[5] = topicLengthLSB;
        System.arraycopy(topicBytes, 0, packet, 6, topicBytes.length);

        packet[6 + topicBytes.length] = (byte) qos;

        out.write(packet);
        out.flush();

        print("%n[SUBSCRIBE] topic %s with QoS %d at %s", topic, qos, new Date());
    }

    public void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        sendCONNECT();
    }

    public void listen() {
        try {
            while (!socket.isClosed()) {
                if (in.available() > 0) {
                    byte[] header = new byte[2]; // 2 bytes (Fixed Header + Remaining Length)
                    in.read(header);

                    if ((header[0] & 0xF0) == (byte) 0x30) {
                        int qos = (header[0] >> 1) & 0x03;

                        int remainingLength = header[1] & 0xFF;
                        byte[] payload = new byte[remainingLength];
                        in.read(payload);

                        int topicLength = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
                        String topic = new String(payload, 2, topicLength);

                        int index = 2 + topicLength;
                        int packetId = 0;
                        if (qos > 0) {
                            packetId = ((payload[index] & 0xFF) << 8) | (payload[index + 1] & 0xFF);
                            index += 2;
                        }

                        String message = new String(payload, index, remainingLength - (index));

                        print("%n[MESSAGE] %d %s | %s at %s", packetId, topic, message, new Date());

                        if (qos == 1) {
                            byte[] puback = new byte[4];
                            puback[0] = (byte) 0x40;
                            puback[1] = 0x02;
                            puback[2] = (byte) (packetId >> 8);
                            puback[3] = (byte) (packetId);

                            out.write(puback);
                            out.flush();
                            print("%n[PUBACK] Confirming %d at %s", packetId, new Date());
                        }
                        else if (qos == 2) {
                            // Acknowledge Receipt of PUBLISH
                            byte[] pubrec = { (byte) 0x50, 0x02, (byte) (packetId >> 8), (byte) packetId };
                            out.write(pubrec);
                            out.flush();
                            print("%n[PUBREC] Sending Acknowledgement Receipt of %d at %s", packetId, new Date());

                            // Wait for PUBREL
                            byte[] pubrel = new byte[4];
                            in.read(pubrel);

                            if (pubrel[0] == (byte) 0x62 && pubrel[1] == 0x02) {
                                print("%n[PUBREL] Acknowledge Received of %d at %s", packetId, new Date());

                                // Send PUBCOMP
                                byte[] pubcomp = { (byte) 0x70, 0x02, (byte) (packetId >> 8), (byte) packetId };
                                out.write(pubcomp);
                                out.flush();
                                print("%n[PUBCOMP] Finish of %d at %s", packetId, new Date());
                            }
                        }

                    } else {
                        byte[] suback = new byte[2];
                        in.read(suback);

                        if (header[0] == (byte) 0x90) {
                            int packetIdReceived = ((suback[0] & 0xFF) << 8) | (suback[1] & 0xFF);
                            print("%n[SUBACK] Subscription confirmed of %d at %s", packetIdReceived, new Date());
                            in.read(new byte[2]);
                        }

                        if (header[0] == (byte) 0x20 && header[1] == 0x02 && suback[1] == 0x00) {
                            print("%n[CONNACK] Received confirmed at %s", new Date());
                        }

                        if (header[0] == (byte) 0x40 && header[1] == 0x02) {
                            int packetIdReceived = ((suback[0] & 0xFF) << 8) | (suback[1] & 0xFF);
                            print("%n[PUBACK] Confirmation Received! Packet ID: %d at %s", packetIdReceived, new Date());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.sendDISCONNECT();
        Thread.sleep(100);
        this.socket.close();
    }

    private synchronized void print(String content, Object... args) {
        System.out.printf(content, args);
    }

    static class IdentityGenerator {
        private static int current = 1;

        public static int generate() {
            return current++;
        }
    }
}
