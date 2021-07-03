package br.ufc.mdcc.AT03_MQTT.alarm;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Alarm extends JFrame implements MqttCallback, Runnable {
	private static final long serialVersionUID = 1L;

	private JLabel labelHT;
	private JLabel labelSTR;
	private JPanel panelLabelHT;
	private JPanel panelLabelSTR;
	private JPanel htIndicator;
	private JPanel strIndicator;
	private JScrollPane scrollPane;
	private JTextArea textArea;
	private Instant lastHT = Instant.MIN;
	private Instant lastSTR = Instant.MIN;
	private long alarmDurationMilis = 10000L;

	private MqttAsyncClient mqttClient = null;

	public Alarm(String brokerURI, long alarmDurationMilis) throws MqttException {
		super();

		initGUI();

		this.alarmDurationMilis = alarmDurationMilis;

		try {
			mqttClient = new MqttAsyncClient(brokerURI, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.setCallback(this);
			IMqttToken token = mqttClient.connect();
			token.waitForCompletion();
		} catch (MqttException e) {
			throw e;
		}
	}

	private void initGUI() {
		panelLabelHT = new JPanel();
		labelHT = new JLabel();
		htIndicator = new JPanel();
		labelSTR = new JLabel();
		strIndicator = new JPanel();
		panelLabelSTR = new JPanel();
		scrollPane = new JScrollPane();
		textArea = new JTextArea();

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("Boiler Temperature Monitoring");
		setMinimumSize(new Dimension(800, 400));
		setResizable(false);

		panelLabelHT.setLayout(new GridLayout(2, 2, 2, 2));

		labelHT.setFont(new Font("Tahoma", 1, 16));
		labelHT.setHorizontalAlignment(SwingConstants.CENTER);
		labelHT.setText("High Temperature");
		panelLabelHT.add(labelHT);

		htIndicator.setBackground(Color.GREEN);
		htIndicator.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));

		GroupLayout htIndicatorLayout = new GroupLayout(htIndicator);
		htIndicator.setLayout(htIndicatorLayout);
		htIndicatorLayout.setHorizontalGroup(
				htIndicatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 220, Short.MAX_VALUE));
		htIndicatorLayout.setVerticalGroup(
				htIndicatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 91, Short.MAX_VALUE));

		panelLabelHT.add(htIndicator);

		labelSTR.setFont(new Font("Tahoma", 1, 16));
		labelSTR.setHorizontalAlignment(SwingConstants.CENTER);
		labelSTR.setText("Sudden Temperature Raise");
		panelLabelHT.add(labelSTR);

		strIndicator.setBackground(Color.GREEN);
		strIndicator.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));

		GroupLayout strIndicatorLayout = new GroupLayout(strIndicator);
		strIndicator.setLayout(strIndicatorLayout);
		strIndicatorLayout.setHorizontalGroup(
				strIndicatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 220, Short.MAX_VALUE));
		strIndicatorLayout.setVerticalGroup(
				strIndicatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 91, Short.MAX_VALUE));

		panelLabelHT.add(strIndicator);

		getContentPane().add(panelLabelHT, BorderLayout.CENTER);

		panelLabelSTR.setLayout(new CardLayout());

		scrollPane.setAutoscrolls(true);
		scrollPane.setEnabled(false);

		textArea.setEditable(false);
		textArea.setColumns(20);
		textArea.setFont(new Font("Courier New", 0, 18));
		textArea.setRows(5);
		scrollPane.setViewportView(textArea);

		panelLabelSTR.add(scrollPane, "logs");

		getContentPane().add(panelLabelSTR, BorderLayout.SOUTH);

		pack();
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
		textArea.insert(DateTimeFormatter.ISO_INSTANT.format(lastHT) + ": HT event\n", 0);
	}

	private synchronized void setLastSTR() {
		lastSTR = Instant.now();
		textArea.insert(DateTimeFormatter.ISO_INSTANT.format(lastSTR) + ": STR event\n", 0);
	}

	public synchronized void checkAlarm() {
		checkIndicator(htIndicator, lastHT);
		checkIndicator(strIndicator, lastSTR);
	}

	private void checkIndicator(JPanel indicator, Instant instant) {
		if (isExpired(instant)) {
			if (indicator.getBackground().equals(Color.RED)) {
				indicator.setBackground(Color.GREEN);
				textArea.insert(DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ": All HT events expired\n", 0);
			}
		} else {
			indicator.setBackground(Color.RED);
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
		setVisible(true);
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature/alarm";
		long durationMilis = 10000L;
		long verificationInterval = 200L;
		Alarm alarm;

		try {
			alarm = new Alarm(serverURI, durationMilis);
			alarm.subscribe(topic);
			Verifier verifier = new Verifier(alarm, verificationInterval);

			new Thread(verifier).start();

			EventQueue.invokeLater(alarm);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
