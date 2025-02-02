package org.telecom.slr.subscribers;

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
        String clientId = "subs";

        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));

        try (MqttClient mqttClient = new MqttClient(brokerURI, clientId)) {
            System.out.print("It Should Clean Session:");
            boolean cleanSession = buffer.readLine().equals("true");

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(cleanSession);
            System.out.println("Mqtt Client: Connecting to Mqtt Broker running at: " + brokerURI);
            mqttClient.connect(connectOptions);
            System.out.println("Mqtt Client: successfully Connected.");

            System.out.print("Inform Qos:");
            int qos = Integer.parseInt(buffer.readLine());

            mqttClient.disconnect();
            mqttClient.connect(connectOptions);

            ////subscribe to the topic
            mqttClient.subscribe(topic, qos);
            mqttClient.setCallback(new MessagesHandler());



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
