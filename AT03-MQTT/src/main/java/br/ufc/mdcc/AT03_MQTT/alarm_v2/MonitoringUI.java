package br.ufc.mdcc.AT03_MQTT.alarm_v2;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MonitoringUI extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;

	public static final char HT_INDICATOR = 0;
	public static final char STR_INDICATOR = 1;

	private JLabel labelHT;
	private JLabel labelSTR;
	private JPanel panelLabelHT;
	private JPanel panelLabelSTR;
	private JPanel htIndicator;
	private JPanel strIndicator;
	private JScrollPane scrollPane;
	private JTextArea textArea;

	public MonitoringUI() {
		super();
		initGUI();
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

	@Override
	public void run() {
		setVisible(true);
	}

	public void insertLog(String log) {
		textArea.insert(log, 0);
	}

	public void fireAlarm(char alarmType) {
		switch (alarmType) {
		case HT_INDICATOR:
			htIndicator.setBackground(Color.RED);
			break;
		case STR_INDICATOR:
			strIndicator.setBackground(Color.RED);
			break;
		}
	}

	public void resetAlarm(char alarmType) {
		switch (alarmType) {
		case HT_INDICATOR:
			htIndicator.setBackground(Color.GREEN);
			break;
		case STR_INDICATOR:
			strIndicator.setBackground(Color.GREEN);
			break;
		}
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature/alarm";
		long durationMilis = 10000L;
		long verificationInterval = 200L;

		try {
			MonitoringUI monitoringUI = new MonitoringUI();
			Alarm alarm = new Alarm(serverURI, topic, durationMilis, monitoringUI);
			Verifier verifier = new Verifier(alarm, verificationInterval);

			new Thread(alarm).start();
			new Thread(verifier).start();

			EventQueue.invokeLater(monitoringUI);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
