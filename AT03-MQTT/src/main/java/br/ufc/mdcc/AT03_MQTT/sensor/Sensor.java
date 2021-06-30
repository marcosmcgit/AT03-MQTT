package br.ufc.mdcc.AT03_MQTT.sensor;

import java.time.Instant;
import java.util.Random;

public class Sensor implements Runnable {

	private double temperature;
	private double minTemperature;
	private double maxTemperature;
	private long milisSamplingInterval;
	private double lowerVariation;
	private double upperVariation;
	private double variationProbability;

	public Sensor(double temperature, double minTemperature, double maxTemperature, long milisSamplingInterval,
			double lowerVariation, double upperVariation, double variationProbability) {
		super();
		this.temperature = temperature;
		this.minTemperature = minTemperature;
		this.maxTemperature = maxTemperature;
		this.milisSamplingInterval = milisSamplingInterval;
		this.lowerVariation = lowerVariation;
		this.upperVariation = upperVariation;
		this.variationProbability = variationProbability;
	}

	@Override
	public void run() {
		Instant instant1 = Instant.now();
		Instant instant2 = instant1;
		Random random = new Random();

		while (true) {
			if (instant2.compareTo(instant1.plusMillis(milisSamplingInterval)) > 0) {
				instant1 = Instant.now();
				System.out.println(random.nextDouble());
			} else {
				instant2 = Instant.now();
			}
		}
	}

	public static void main(String[] args) {
		Sensor sensor = new Sensor(0., 0., 0., 1000, 0., 0., 0.5);
		new Thread(sensor).start();
	}
}
