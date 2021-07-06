package br.ufc.mdcc.AT03_MQTT.CAT;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Cat implements MqttCallback {

	private MqttAsyncClient mqttClient = null;
	private String topicAlarm;
	private ArrayList<Message> lastMessages;
	private int xTime = 12;

	public Cat(String brokerURI, String topicAlarm) throws MqttException {
		super();
		this.topicAlarm = topicAlarm;

		try {
			this.mqttClient = new MqttAsyncClient(brokerURI, "CAT", new MemoryPersistence());
			mqttClient.setCallback(this);
			IMqttToken token = this.mqttClient.connect();
			token.waitForCompletion();
			lastMessages = new ArrayList<Message>();
		} catch (MqttException e) {
			throw e;
		}
	}

	public void subscribe(String topic) throws MqttException {
		IMqttToken token = this.mqttClient.subscribe(topic, 0);
		token.waitForCompletion();
	}

	public void removeMessageInTime() {
		this.lastMessages
				.removeIf(message -> message.getTimestamp() < System.currentTimeMillis() - (this.xTime * 1000));
	}

	public double getAvgTemperature() {
		double total = 0;
		for (Message message : this.lastMessages) {
			total += message.getTemperature();
		}

//		System.out.println(this.lastMessages.size());

		return total / this.lastMessages.size();
	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("Connection to broker lost!" + cause.getMessage());
	}

	double currentAvgTemp = 0.0;
	double lastAvgTemp = currentAvgTemp;

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		this.removeMessageInTime(); // limpa buffer. janela
		Double temperature = Double.parseDouble(new String(message.getPayload()));
		this.lastMessages.add(new Message(temperature, System.currentTimeMillis()));

		currentAvgTemp = this.getAvgTemperature();

		System.out.println("Current mean: " + String.format(Locale.US, "%.1f", currentAvgTemp));

		if (lastAvgTemp > 0.0 && currentAvgTemp - lastAvgTemp > 5.0) {
			System.out.println("EVENT: STR!");
			mqttClient.publish(this.topicAlarm, String.format("STR").getBytes(), 0, false);
		}

		lastAvgTemp = currentAvgTemp;

		if (currentAvgTemp > 200.0) {
			System.out.println("EVENT: High Temperature!");
			mqttClient.publish(this.topicAlarm, String.format("HT").getBytes(), 0, false);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature";
		String topicAlarm = "boiler/temperature/alarm";
		try {
			Cat alarm = new Cat(serverURI, topicAlarm);
			alarm.subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
