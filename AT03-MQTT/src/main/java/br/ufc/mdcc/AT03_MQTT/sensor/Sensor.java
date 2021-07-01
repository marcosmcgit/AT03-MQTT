package br.ufc.mdcc.AT03_MQTT.sensor;

import java.time.Instant;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Sensor implements Runnable {

	private String name;
	private double temperature;
	private double minTemperature;
	private double maxTemperature;
	private long milisSamplingInterval;
	private double lowerVariation;
	private double upperVariation;
	private double variationProbability;

	MqttClient mqttClient = null;
	Random random = null;

	public Sensor(String name, double temperature, double minTemperature, double maxTemperature,
			long milisSamplingInterval, double lowerVariation, double upperVariation, double variationProbability)
			throws MqttException {
		super();
		this.name = name;
		this.temperature = temperature;
		this.minTemperature = minTemperature;
		this.maxTemperature = maxTemperature;
		this.milisSamplingInterval = milisSamplingInterval;
		this.lowerVariation = lowerVariation;
		this.upperVariation = upperVariation;
		this.variationProbability = variationProbability;

		if (mqttClient == null) {
			try {
				mqttClient = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
				mqttClient.connect();
			} catch (MqttException e) {
				throw e;
			}
		}

		if (random == null) {
			random = new Random();
		}
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

				System.out.println(this.name + "/temperature=" + String.format("%.1f", this.temperature));

				try {
					mqttClient.publish(this.name + "/temperature=", String.format(".2f", this.temperature).getBytes(),
							0, false);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			} else {
				instant2 = Instant.now();
			}
		}
	}

	public static void main(String[] args) {
		try {
			Sensor sensor = new Sensor("sensor_001", 180., 160., 280., 1000, -10., 10, 0.95);
			new Thread(sensor).start();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
