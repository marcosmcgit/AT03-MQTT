package br.ufc.mdcc.AT03_MQTT.CAT;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class Cat implements MqttCallback {

	private MqttAsyncClient mqttClient = null;
	private String topicavg;
	private ArrayList<Message> lastMessages;
	private int xTime = 1;
	
	
	public Cat(String brokerURI, String topicavg) throws MqttException {
		super();
		this.topicavg = topicavg;
		
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

	public void funcao1() {
		this.lastMessages.removeIf(message -> message.getTimestamp() < System.currentTimeMillis()-(this.xTime*1000));
		
	}
	
	public double getAvgTemperature() {
		double total=0;
		for (Message message : this.lastMessages) {
			total += message.getTemperature();
		}
		
		System.out.println(this.lastMessages.size());
		
		return total/this.lastMessages.size();
		
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("Connection to broker lost!" + cause.getMessage());
	}

	double last_avgtemp = 0.0;
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		this.funcao1(); // limpa buffer. janela
		Double temperature = Double.parseDouble(new String(message.getPayload()));
		this.lastMessages.add(new Message(temperature,System.currentTimeMillis()));
		
		double avg_test = this.getAvgTemperature();
		
		System.out.println(avg_test);
		
		
		if ( Math.abs(avg_test - last_avgtemp) > 5.0 ) {
			System.out.println("Aumento de temperatura repentina");
			mqttClient.publish(this.topicavg, String.format(Locale.US, "%.1f", avg_test).getBytes(), 0, false);
		}
//	
		last_avgtemp=avg_test;
//					
//		System.out.println("\n\tTemperatura = " + temperature+
//							"\n\tMedia = "+avg);
//		
//		if (avg > 200.0) {
//			System.out.println("Temperatura alta");
////			mqttClient.publish(this.topicavg, String.format("").getBytes(), 0, false);
//		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature";
		String topicavg = "boiler/temperature/average";
		try {
			Cat alarm = new Cat(serverURI,topicavg);
			alarm.subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
