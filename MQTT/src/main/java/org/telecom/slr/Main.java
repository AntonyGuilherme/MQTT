package org.telecom.slr;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {
    public static void main(String[] args) {
        String topic        = "labs/paho";
        String messageContent = "Message from my Lab's Paho Mqtt Client";
        int qos             = 1;
        String brokerURI       = "tcp://localhost:1883";
        String clientId     = "myClientID_Pub";


        ////instantiate a synchronous MQTT Client to connect to the targeted Mqtt Broker
        try(MqttClient mqttClient = new MqttClient(brokerURI, clientId, new MemoryPersistence())) {
            ////specify the Mqtt Client's connection options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            //clean session
            connectOptions.setCleanSession(true);
            //customise other connection options here...
            //...

            ////connect the mqtt client to the broker
            System.out.println("Mqtt Client: Connecting to Mqtt Broker running at: " + brokerURI);
            mqttClient.connect(connectOptions);
            System.out.println("Mqtt Client: sucessfully Connected.");

            ////publish a message
            System.out.println("Mqtt Client: Publishing message: " + messageContent);
            MqttMessage message = new MqttMessage(messageContent.getBytes());//instantiate the message including its content (payload)
            message.setQos(qos);//set the message's QoS
            message.setRetained(true);
            mqttClient.publish(topic, message);//publish the message to a given topic
            System.out.println("Mqtt Client: successfully published the message.");

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
        }
    }
}