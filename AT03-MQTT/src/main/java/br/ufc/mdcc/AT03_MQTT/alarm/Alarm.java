package br.ufc.mdcc.AT03_MQTT.alarm;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * This class will subscribe to the MQTT topic to monitor sensor temperatures.
 * It will check if an alarm has fired, based on receiving MQTT messages through
 * a callback (and if it is still firing, based on expiration method provided by
 * the MonitoringUI class).
 */
public class Alarm implements MqttCallback, Runnable {
	private long alarmDurationMilis = 10000L; // length of time of an alarm
	private MqttAsyncClient mqttClient = null;
	private String topic;
	private MonitoringUI monitoringUI;
	private Instant lastHT = Instant.MIN; // last time of a high temperature occurrence. It's in the critical region.
	private Instant lastSTR = Instant.MIN; // last time of a sudden temperature rise occurrence. It's in the critical
											// region.
	private boolean htFiring = false; // controls if an HT alarm is firing. It's in the critical region.
	private boolean strFiring = false; // controls if an STR alarm is firing. It's a critical area

	public Alarm(String brokerURI, String topic, long alarmDurationMilis, MonitoringUI monitoringUI)
			throws MqttException {
		super();
		try {
			mqttClient = new MqttAsyncClient(brokerURI, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.setCallback(this);
			IMqttToken token = mqttClient.connect();
			token.waitForCompletion();
		} catch (MqttException e) {
			throw e;
		}
		this.topic = topic;
		this.alarmDurationMilis = alarmDurationMilis;
		this.monitoringUI = monitoringUI;
	}

	public void subscribe(String topic) throws MqttException {
		IMqttToken token = mqttClient.subscribe(topic, 0);
		token.waitForCompletion();
	}

	@Override
	public void connectionLost(Throwable cause) {
		cause.printStackTrace();
	}

	/*
	 * When a message arrives, it verifies the type of alarm (HT or STR) and calls
	 * methods to set the correct occurrences
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String alarmType = new String(message.getPayload());
		if (alarmType.equals("HT")) {
			setLastHT();
		} else if (alarmType.equals("STR")) {
			setLastSTR();
		}
	}

	/*
	 * Method handling critical region. It will set the lastHT as now(), and the
	 * indication that an HT alarm has fired. Futhermore, it will log a message in
	 * the GUI.
	 */
	private synchronized void setLastHT() {
		lastHT = Instant.now();
		htFiring = true;
		monitoringUI.insertLog(DateTimeFormatter.ISO_INSTANT.format(lastHT) + ": HT event\n");
	}

	/*
	 * Method handling critical region. It will set the lastSTR as now(), and the
	 * indication that an ST alarm has fired. Futhermore, it will log a message in
	 * the GUI.
	 */
	private synchronized void setLastSTR() {
		lastSTR = Instant.now();
		strFiring = true;
		monitoringUI.insertLog(DateTimeFormatter.ISO_INSTANT.format(lastSTR) + ": STR event\n");
	}

	/*
	 * Method handling critical region. If will checks if an alarm has expired of is
	 * still firing, calling the proper methods from the MonitoringUI object (GUI).
	 * It will then fire or reset the alarm, based on the values in the critical
	 * region.
	 */
	public synchronized void checkAlarm() {
		if (isExpired(lastHT)) {
			if (htFiring) {
				htFiring = false;
				monitoringUI.resetAlarm(MonitoringUI.HT_INDICATOR);
				monitoringUI
						.insertLog(DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ": All HT events expired\n");
			}
		} else {
			monitoringUI.fireAlarm(MonitoringUI.HT_INDICATOR);
		}

		if (isExpired(lastSTR)) {
			if (strFiring) {
				strFiring = false;
				monitoringUI.resetAlarm(MonitoringUI.STR_INDICATOR);
				monitoringUI
						.insertLog(DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ": All STR events expired\n");
			}
		} else {
			monitoringUI.fireAlarm(MonitoringUI.STR_INDICATOR);
		}
	}

	/*
	 * Checks if the current moment is ahead of a certain moment parameter (instant)
	 * by alarmDurationMilis (ms).
	 */
	private synchronized boolean isExpired(Instant instant) {
		if (instant != null && instant.plusMillis(alarmDurationMilis).isBefore(Instant.now())) {
			return true;
		}

		return false;
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	/*
	 * The run method will only subscribe this object on the proper topic of the
	 * MQTT broker.
	 */
	@Override
	public void run() {
		try {
			subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
