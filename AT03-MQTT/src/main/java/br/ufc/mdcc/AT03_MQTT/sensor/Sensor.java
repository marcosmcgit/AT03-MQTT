package br.ufc.mdcc.AT03_MQTT.sensor;

import java.time.Instant;
import java.util.Locale;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Sensor implements Runnable {

	private double temperature;
	private double minTemperature;
	private double maxTemperature;
	private long milisSamplingInterval;
	private double lowerVariation;
	private double upperVariation;
	private double variationProbability;
	private String topic;

	MqttClient mqttClient = null;
	Random random = null;

	public Sensor(double temperature, double minTemperature, double maxTemperature, long milisSamplingInterval,
			double lowerVariation, double upperVariation, double variationProbability, String mqttServerURI,
			String topic) throws MqttException {
		super();
		this.temperature = temperature;
		this.minTemperature = minTemperature;
		this.maxTemperature = maxTemperature;
		this.milisSamplingInterval = milisSamplingInterval;
		this.lowerVariation = lowerVariation;
		this.upperVariation = upperVariation;
		this.variationProbability = variationProbability;
		this.topic = topic;

		try {
			mqttClient = new MqttClient(mqttServerURI, MqttClient.generateClientId());
			mqttClient = new MqttClient(mqttServerURI, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.connect();
		} catch (MqttException e) {
			throw e;
		}

		random = new Random();
	}

	@Override
	public void run() {
		Instant instant1 = Instant.now();
		Instant instant2 = instant1;

		while (true) {
			if (instant2.compareTo(instant1.plusMillis(milisSamplingInterval)) > 0) {
				instant1 = Instant.now();
				if (random.nextDouble() <= this.variationProbability) {
					double newTemp = this.temperature + this.lowerVariation
							+ (random.nextDouble() * (this.upperVariation - this.lowerVariation));
					if (newTemp < this.minTemperature) {
						this.temperature = this.minTemperature;
					} else if (newTemp > this.maxTemperature) {
						this.temperature = this.maxTemperature;
					} else {
						this.temperature = newTemp;
					}
				}

				System.out.println(this.topic + "=" + String.format(Locale.US, "%.1f", this.temperature));

				try {
					mqttClient.publish(this.topic, String.format(Locale.US, "%.1f", this.temperature).getBytes(), 0,
							false);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			} else {
				instant2 = Instant.now();
			}
		}
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature";
		int numSensors = 20;
		try {
			for (int i = 0; i < numSensors; i++) {
				Sensor sensor = new Sensor(180., 170., 220., 1000, -1., 2, 0.98, serverURI, topic);
				new Thread(sensor).start();
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
