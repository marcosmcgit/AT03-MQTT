package br.ufc.mdcc.AT03_MQTT.alarm;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Alarm implements MqttCallback {

	private MqttAsyncClient mqttClient = null;

	public Alarm(String brokerURI) throws MqttException {
		super();
		try {
			this.mqttClient = new MqttAsyncClient(brokerURI, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.setCallback(this);
			IMqttToken token = this.mqttClient.connect();
			token.waitForCompletion();
		} catch (MqttException e) {
			throw e;
		}
	}

	public void subscribe(String topic) throws MqttException {
		IMqttToken token = this.mqttClient.subscribe(topic, 0);
		token.waitForCompletion();
	}

	@Override
	public void connectionLost(Throwable cause) {
		cause.printStackTrace();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		System.out.println(topic + "=" + new String(message.getPayload()));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature/average";
		try {
			Alarm alarm = new Alarm(serverURI);
			alarm.subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
