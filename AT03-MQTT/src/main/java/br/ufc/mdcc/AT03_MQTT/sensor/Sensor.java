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

				System.out.println("temperature=" + this.temperature);
			} else {
				instant2 = Instant.now();
			}
		}
	}

	public static void main(String[] args) {
		Sensor sensor = new Sensor(180., 160., 280., 1, -10., 10, 0.95);
		new Thread(sensor).start();
	}
}
