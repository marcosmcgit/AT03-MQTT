package br.ufc.mdcc.AT03_MQTT.CAT;

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

	public Cat(String brokerURI, String topicavg) throws MqttException {
		super();
		this.topicavg = topicavg;
		
		try {
			this.mqttClient = new MqttAsyncClient(brokerURI, "CAT", new MemoryPersistence());
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
		System.out.println("Connection to broker lost!" + cause.getMessage());
	}

	Double total = 0.0;
	Double last_avgtemp = 0.0;
			
	int qtd = 0;
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		Double temperature = Double.parseDouble(new String(message.getPayload()));
					
		total = total + temperature;
		qtd++;
	  
		Double avg = total/qtd;
	  
		if ( Math.abs(avg - last_avgtemp) > 5.0 ) {
			System.out.println("Aumento de temperatura repentina");
			mqttClient.publish(this.topicavg, String.format(Locale.US, "%.1f", avg).getBytes(), 0, false);
		}
	
		last_avgtemp=avg;
					
		System.out.println("\n\tTemperatura = " + temperature+
							"\n\tMedia = "+avg);
	  
		if (avg > 200.0) {
			System.out.println("Temperatura alta");
//			mqttClient.publish(this.topicavg, String.format("").getBytes(), 0, false);
		}
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
