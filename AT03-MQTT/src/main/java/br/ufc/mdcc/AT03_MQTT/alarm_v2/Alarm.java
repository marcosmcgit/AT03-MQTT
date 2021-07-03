package br.ufc.mdcc.AT03_MQTT.alarm_v2;

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

public class Alarm implements MqttCallback, Runnable {
	private long alarmDurationMilis = 10000L;
	private Instant lastHT = Instant.MIN;
	private Instant lastSTR = Instant.MIN;
	private MqttAsyncClient mqttClient = null;
	private String topic;
	private MonitoringUI monitoringUI;
	private boolean htFiring = false;
	private boolean strFiring = false;

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

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String alarmType = new String(message.getPayload());
		if (alarmType.equals("HT")) {
			setLastHT();
		} else if (alarmType.equals("STR")) {
			setLastSTR();
		}
	}

	private synchronized void setLastHT() {
		lastHT = Instant.now();
		htFiring = true;
		monitoringUI.insertLog(DateTimeFormatter.ISO_INSTANT.format(lastHT) + ": HT event\n");
	}

	private synchronized void setLastSTR() {
		lastSTR = Instant.now();
		strFiring = true;
		monitoringUI.insertLog(DateTimeFormatter.ISO_INSTANT.format(lastSTR) + ": STR event\n");
	}

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

	private synchronized boolean isExpired(Instant instant) {
		if (instant != null && instant.plusMillis(alarmDurationMilis).isBefore(Instant.now())) {
			return true;
		}

		return false;
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	@Override
	public void run() {
		try {
			subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
