package br.ufc.mdcc.AT03_MQTT.alarm;

public class Verifier implements Runnable {

	private Alarm alarm;
	private long interval;

	public Verifier(Alarm alarm, long interval) {
		super();
		this.alarm = alarm;
		this.interval = interval;
	}

	@Override
	public void run() {
		while (true) {
			alarm.checkAlarm();
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}