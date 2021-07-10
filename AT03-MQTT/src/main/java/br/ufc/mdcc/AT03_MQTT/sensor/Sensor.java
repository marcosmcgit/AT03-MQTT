package br.ufc.mdcc.AT03_MQTT.sensor;

import java.util.Locale;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Sensors simulated using threads.
 */
public class Sensor implements Runnable {

	private double temperature; // current temperature
	private double minTemperature; // minimum possible temperature
	private double maxTemperature; // maximum possible temperature
	private long milisSamplingInterval; // interval between sensor data upload
	private double lowerVariation; // current temp may vary from curTemp+lowerVariation
	private double upperVariation; // current temp may vary to curTemp+upperVariation
	private double variationProbability; // probability of temperature changing
	private String topic;

	private MqttClient mqttClient = null;
	private Random random = null;

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
			mqttClient = new MqttClient(brokerURI, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.connect();
		} catch (MqttException e) {
			throw e;
		}

		random = new Random();
	}

	/*
	 * Calculates new temperature of this sensor, and sent the data to the mqtt
	 * topic, sleeping milisSamplingInterval between next probe.
	 */
	@Override
	public void run() {
		while (true) {
			calculateNewTemperature();

			try {
				mqttClient.publish(topic, String.format(Locale.US, "%.1f", temperature).getBytes(), 0, false);
				System.out.println(topic + "=" + String.format(Locale.US, "%.1f", temperature));
				Thread.sleep(milisSamplingInterval);
			} catch (MqttException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}
	}

	private void calculateNewTemperature() {
		// With a variationProbability chance it will change the current temperature.
		if (random.nextDouble() <= variationProbability) {
			// if temp reachs max or min, it will be 2/3 * (min+max), to avoid get stuck on
			// the extreme values
			if (temperature == maxTemperature || temperature == minTemperature) {
				temperature = 2. * (minTemperature + maxTemperature) / 3.;
			} else {
				// new temperature will be the current temperature plus a random value between
				// lowerVariation and upperVariation
				double newTemp = temperature + lowerVariation
						+ (random.nextDouble() * (upperVariation - lowerVariation));
				// but it never goes beyond the upper and lower bounds
				if (newTemp < minTemperature) {
					temperature = minTemperature;
				} else if (newTemp > maxTemperature) {
					temperature = maxTemperature;
				} else {
					temperature = newTemp;
				}
			}
		}
	}

	/*
	 * The main method will run sensors with threads. It's important to indicate the
	 * correct values of the MQTT broker, the topic and the number of sensors.
	 * Furthermore, to define the suitable parameters to achieve the desired sensor
	 * temperature variation.
	 */
	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883"; // MQTT broker URL
		String topic = "boiler/temperature";
		int numSensors = 12;
		try {
			for (int i = 0; i < numSensors; i++) {
				Sensor sensor = new Sensor(170., 150., 250., 60000, 0., 50., 0.9, serverURI, topic);
				new Thread(sensor).start();
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
