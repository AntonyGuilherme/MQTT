package org.telecom.slr.subscribers;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Weather {
    public static void main(String[] args) {
        String brokerURI = "tcp://localhost:1883";
        String clientId = "weather";

        ////instantiate a synchronous MQTT Client to connect to the targeted Mqtt Broker
        try(MqttClient mqttClient = new MqttClient(brokerURI, clientId, new MemoryPersistence())) {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            mqttClient.connect(connectOptions);
            System.out.println("Mqtt Client: successfully Connected.");

            ////publish a message
            for (int i = 0; i < 100; i++) {
                publish("dht22/value", mqttClient);
                publish("dht22/value2", mqttClient);
                publish("sht30/value", mqttClient);
                publish("sht30/value2", mqttClient);
                publish("dht20", mqttClient);
                publish("sht30", mqttClient);

                Thread.sleep(1000);
            }


            ////disconnect the Mqtt Client
            mqttClient.disconnect();
            System.out.println("Mqtt Client: Disconnected.");
        }
        catch(MqttException e) {
            System.out.println("Mqtt Exception reason: " + e.getReasonCode());
            System.out.println("Mqtt Exception message: " + e.getMessage());
            System.out.println("Mqtt Exception location: " + e.getLocalizedMessage());
            System.out.println("Mqtt Exception cause: " + e.getCause());
            System.out.println("Mqtt Exception reason: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void publish(String suffix, MqttClient client) throws MqttException {
        String topic = String.format("/home/Lyon/sido/%s", suffix);
        String content = String.valueOf(Math.random() * 100);
        MqttMessage message = new MqttMessage(content.getBytes());//instantiate the message including its content (payload)
        message.setQos(0);
        client.publish(topic, message);
    }
}
