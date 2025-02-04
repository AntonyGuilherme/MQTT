package org.telecom.slr.mqtt.client;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class MqttClient implements AutoCloseable {
    private static final String BROKER_HOST = "localhost"; // Replace with your broker
    private static final int BROKER_PORT = 1883;
    private static final String BROKER_CLIENT = "client";

    private final String host;
    private final int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;

    private MqttClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
        try (MqttClient client = connect(BROKER_HOST, BROKER_PORT)) {
            client.sendCONNECT();
            client.publish("topic", "MQTT is awesome");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void publish(String topic, String message) throws IOException, InterruptedException {
        byte[] topicBytes = topic.getBytes();
        byte[] messageBytes = message.getBytes();

        byte topicLengthMSB = (byte) (topicBytes.length >> 8);
        byte topicLengthLSB = (byte) (topicBytes.length);

        int length = 2 + topicBytes.length + 2 + messageBytes.length;
        byte[] packet = new byte[length + 2];

        packet[0] = (byte) 0x32;
        packet[1] = (byte) length;
        packet[2] = topicLengthMSB;
        packet[3] = topicLengthLSB;
        System.arraycopy(topicBytes, 0, packet, 4, topicBytes.length);
        int packetId = 25; // Fixed Packet Identifier for QoS 1
        byte packetIdMSB = (byte) (packetId >> 8);
        byte packetIdLSB = (byte) (packetId);

        int index = 4 + topicBytes.length;
        packet[index++] = packetIdMSB;
        packet[index++] = packetIdLSB;

        System.arraycopy(messageBytes, 0, packet, index, messageBytes.length);

        out.write(packet);
        out.flush();

        byte[] puback = new byte[4];
        int bytesRead = in.read(puback);
        if (bytesRead == 4 && puback[0] == (byte) 0x40 && puback[1] == 0x02) {
            int packetIdReceived = ((puback[2] & 0xFF) << 8) | (puback[3] & 0xFF);
            System.out.println("PUBACK received! Packet ID: " + packetIdReceived);
        } else {
            throw new RuntimeException("PUBACK not received or incorrect response: " + Arrays.toString(puback));
        }
    }

    private void sendCONNECT() throws IOException {
        byte[] protocolName = {0x00, 0x04, 'M', 'Q', 'T', 'T'};
        byte protocolLevel = 0x04;  // MQTT 3.1.1
        byte connectFlags = 0x02;   // Clean session
        byte keepAliveMSB = 0x00;
        byte keepAliveLSB = 0x3C;   // 60 seconds

        byte[] clientIdBytes = BROKER_CLIENT.getBytes();
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

        byte[] connack = new byte[4];
        int readBytes = in.read(connack);

        if (readBytes == 4 && connack[0] == (byte) 0x20 && connack[1] == 0x02 && connack[3] == 0x00) {
            System.out.printf("CONNACK received %s%n", connack);
        } else {
            throw new IOException("CONNACK failed: " + Arrays.toString(connack));
        }
    }

    private void sendDISCONNECT() throws IOException {
        byte[] disconnectPacket = {(byte) 0xE0, 0x00};
        out.write(disconnectPacket);
        out.flush();
        System.out.println("DISCONNECT");
    }

    public static MqttClient connect(String host, int port) throws IOException {
        MqttClient client = new MqttClient(host, port);
        client.connect();
        return client;
    }

    @Override
    public void close() throws Exception {
        this.sendDISCONNECT();
        this.socket.close();
    }

    private void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
    }
}
