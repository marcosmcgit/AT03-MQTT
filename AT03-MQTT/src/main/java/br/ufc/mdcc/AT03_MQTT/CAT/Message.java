package br.ufc.mdcc.AT03_MQTT.CAT;

public class Message {
	private double temperature;
	private long timestamp;

	public Message(double temperature, long timestamp) {
		this.temperature = temperature;
		this.timestamp = timestamp;
	}
	
	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
