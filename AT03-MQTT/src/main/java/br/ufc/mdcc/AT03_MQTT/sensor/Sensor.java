package br.ufc.mdcc.AT03_MQTT.sensor;

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
			double lowerVariation, double upperVariation, double variationProbability, String brokerURI, String topic)
			throws MqttException {
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
			this.mqttClient = new MqttClient(brokerURI, MqttClient.generateClientId(), new MemoryPersistence());
			this.mqttClient.connect();
		} catch (MqttException e) {
			throw e;
		}

		random = new Random();
	}

	@Override
	public void run() {
		while (true) {
			calculateNewTemperature();

			try {
				mqttClient.publish(this.topic, String.format(Locale.US, "%.1f", this.temperature).getBytes(), 0, false);
				System.out.println(this.topic + "=" + String.format(Locale.US, "%.1f", this.temperature));
				Thread.sleep(milisSamplingInterval);
			} catch (MqttException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}
	}

	private void calculateNewTemperature() {
		if (this.random.nextDouble() <= this.variationProbability) {
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
