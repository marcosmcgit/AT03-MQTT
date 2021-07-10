package br.ufc.mdcc.AT03_MQTT.CAT;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

// O Cat representa uma entidade que consume valores de um tópico "boiler/temperature" 
// e, baseado nesses valores, publica uma mensagem de alerta no tópico "boiler/temperature/alarm"
// Esses tópicos são parametros que podem ser alterados quando instanciados e passados na classe main.

// Cat implementa MqttCallback definida em org.eclipse.paho.client.mqttv3. 
public class Cat implements MqttCallback {

	private MqttAsyncClient mqttClient = null;
	private String topicAlarm; // Representa o tópico de envio de notificações mediante o cálculo da média
	private ArrayList<Message> lastMessages; // Utilizado para armazenar as últimas temperaturas recebidas
	private int xTime = 120; // Essa variável representa o tempo de observação para cálculo da média

//	Quando for criado uma instância de Cat será necessário especificar o broker e o tópico para envio de mensagens que notificam um alarme.
	public Cat(String brokerURI, String topicAlarm) throws MqttException {
		super();
		this.topicAlarm = topicAlarm;

		try {
			// Instanciando a classe MqttAsyncClient para conexão com o broker MQTT.
			this.mqttClient = new MqttAsyncClient(brokerURI, "CAT", new MemoryPersistence());
			
			// setCallback é um método usado para ler eventos de um tópico. Dentre esses 
			// eventos podemos citar: nova mensagem, conexão perdida e entrega de mensagem ao broker
			mqttClient.setCallback(this);
			IMqttToken token = this.mqttClient.connect();
			token.waitForCompletion();
			lastMessages = new ArrayList<Message>();
		} catch (MqttException e) {
			throw e;
		}
	}

	public void subscribe(String topic) throws MqttException {
		IMqttToken token = this.mqttClient.subscribe(topic, 0);
		token.waitForCompletion();
	}

	// O seguinte método é usado para retirar mensagens do ArrayList do tipo Message de acordo com o tempo. 
	// Ou seja, o método removeMessageInTime() auxiliar no cálculo da média 
	// das temperaturas nos últimos xTime segundos.
	
	public void removeMessageInTime() {
		this.lastMessages
				.removeIf(message -> message.getTimestamp() < System.currentTimeMillis() - (this.xTime * 1000));
	}

	// O método getAvgTemperature() calcula a média das temperaturas contidas no ArrayList<Message> lastMessages.
	public double getAvgTemperature() {
		double total = 0;
		for (Message message : this.lastMessages) {
			total += message.getTemperature();
		}

//		System.out.println(this.lastMessages.size());

		return total / this.lastMessages.size();
	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("Connection to broker lost!" + cause.getMessage());
	}

	double currentAvgTemp = 0.0;
	double lastAvgTemp = currentAvgTemp;

	// O método a seguir é executado sempre na chegada de uma nova mensagem. O messageArrived recebe uma String com um tópico e uma instância de MqttMessage.	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		this.removeMessageInTime(); 
		// Limpa o 'buffer'. Sempre que uma nova mensagem for notificada
		// esse método é executado para manter apenas aquelas recebidas nos ultimos xTime segundos.					
		
		// O método getPayload() é usado para recuperar a mensagem como um array de bytes
		// Em seguida a mensagem é convertida em double, pois trata-se de uma abstração de temperatura
		// recuperado por diferentes sensores.
		
		Double temperature = Double.parseDouble(new String(message.getPayload()));
		
		// A mensagem é adicionada ao ArrayList passando como parâmetro a temperatura
		// e o tempo que foi recebida.
		this.lastMessages.add(new Message(temperature, System.currentTimeMillis()));

		currentAvgTemp = this.getAvgTemperature();

		System.out.println("Current mean: " + String.format(Locale.US, "%.1f", currentAvgTemp));

		if (lastAvgTemp > 0.0 && currentAvgTemp - lastAvgTemp > 5.0) {
			System.out.println("EVENT: STR!");
			
			// Caso a diferença entre as duas últimas médias de temperatura for maior que cinco
			// O Cat publica uma mensagem "STR" indicando aumento de temperatura repentina
			mqttClient.publish(this.topicAlarm, String.format("STR").getBytes(), 0, false);
		}

		lastAvgTemp = currentAvgTemp;

		if (currentAvgTemp > 200.0) {
			System.out.println("EVENT: High Temperature!");
			
			// Caso a média de temperatura for maior que 200.0 C
			// O Cat publica uma mensagem "HT" indicando alta temperatura.
			mqttClient.publish(this.topicAlarm, String.format("HT").getBytes(), 0, false);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	public static void main(String[] args) {
		String serverURI = "tcp://localhost:1883";
		String topic = "boiler/temperature";
		String topicAlarm = "boiler/temperature/alarm";
		try {
			Cat alarm = new Cat(serverURI, topicAlarm);
			alarm.subscribe(topic);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

}
