package org.telecom.slr;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.logging.Logger;

public class MessagesHandler implements MqttCallback {
    Logger log = Logger.getLogger(this.getClass().getName());

    @Override
    public void connectionLost(Throwable throwable) {
        log.severe("Connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        System.out.println(String.format("Message arrived %s %s", topic, new String(mqttMessage.getPayload())));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        try {
            log.info(String.format("Delivery complete %s", iMqttDeliveryToken.getMessage()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
}
