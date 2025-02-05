package org.telecom.slr;

import org.telecom.slr.mqtt.client.MqttClient;

public class Main {
    private static final String BROKER_HOST = "localhost"; // Replace with your broker
    private static final int BROKER_PORT = 1883;
    private static final String BROKER_CLIENT = "client";

    public static void main(String[] args) {
        try (MqttClient client = MqttClient.connect(BROKER_HOST, BROKER_PORT, BROKER_CLIENT)) {
            new Thread(client::listen).start();

            Thread.sleep(100);

            client.subscribe("mqtt", 1);
            Thread.sleep(100);
            client.publish("mqtt","Publish #1 QOS 1", 1);
            Thread.sleep(100);
            client.publish("mqtt","Publish #2 QOS 1", 1);
            Thread.sleep(100);
            client.publish("mqtt","Publish #3 QOS 1", 1);
            Thread.sleep(100);
            client.publish("mqtt","Publish #4 QOS 1", 1);
            Thread.sleep(100);
            client.publish("mqtt","Publish #5 QOS 1", 1);

            Thread.sleep(100);
            client.subscribe("mqtt-2", 2);
            Thread.sleep(100);
            client.publish("mqtt-2","Publish #1 QOS 2", 2);
            Thread.sleep(100);
            client.publish("mqtt-2","Publish #2 QOS 2", 2);
            Thread.sleep(100);
            client.publish("mqtt-2","Publish #3 QOS 2", 2);
            Thread.sleep(100);
            client.publish("mqtt-2","Publish #4 QOS 2", 2);
            Thread.sleep(100);
            client.publish("mqtt-2","Publish #5 QOS 2", 2);
            Thread.sleep(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}