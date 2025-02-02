package org.telecom.slr;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class Listener {
    public static void main(String[] args) {
        String topic = "labs/paho";
        String brokerURI = "tcp://localhost:1883";
        String clientId = "subscriber";

        ////instantiate a synchronous MQTT Client to connect to the targeted Mqtt Broker
        try (MqttClient mqttClient = new MqttClient(brokerURI, clientId)) {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            System.out.println("Mqtt Client: Connecting to Mqtt Broker running at: " + brokerURI);
            mqttClient.connect(connectOptions);
            System.out.println("Mqtt Client: successfully Connected.");

            ////subscribe to the topic
            mqttClient.subscribe(topic, 0);
            mqttClient.setCallback(new MessagesHandler());

            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));

            // Reading data using readLine
            while(!Objects.equals(buffer.readLine(), "finish"));

            mqttClient.disconnect();
        } catch (MqttException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
