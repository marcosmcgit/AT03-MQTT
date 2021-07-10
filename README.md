# AT03-MQTT

O projeto está organizado em 3 pacotes:
1. `br.ufc.mdcc.AT03_MQTT.sensor`
2. `br.ufc.mdcc.AT03_MQTT.CAT`
3. `br.ufc.mdcc.AT03_MQTT.alarm`

No pacote `br.ufc.mdcc.AT03_MQTT.alarm`, a lógica está dividida nas seguintes classes:

* `Alarm.java`: trata da subscrição no tópico `boiler/temperature/alarm` do broker MQTT e da manipulação da região crítica reponsável por controlar a sinalização de alarmes;
* `MonitoringUI.java`: trata da manipulação de componentes da interface gráfica com o usuário;
* `Verifier.java`: trata da checagem periódica da ocorrência ou expiração de alarmes.

Em nossos testes, utilizamos o broker MQTT [Mosquitto](https://mosquitto.org/), executando tudo na mesma máquina e usando o endereço localhost. Para utilizar máquinas diferentes ou ainda utilizar outros endereços na mesma máquina, é necessário ajustar os endereços do broker. Para tanto, basta alterar o valor da variável `serverURI` nos métodos `main` das classes `Sensor.java`, `Cat.Java` e `MonitoringUI.java`.

Para executá-lo, é necessário ter o broker MQTT em execução. Satisfeita essa condição, basta seguir os seguintes passos, em qualquer ordem:
1. `br.ufc.mdcc.AT03_MQTT.alarm.MonitoringUI.main`: vai mostrar uma GUI para exibir alarmes de alta temperatura (HT) ou aumento repentino de temperatura (STR);
2. `br.ufc.mdcc.AT03_MQTT.CAT.Cat.main`: vai monitorar o tópico `boiler/temperature`, em que os sensores publicam as temperaturas coletadas. Caso detecte uma situação de HT ou ou STR, publicará uma mensagem no tópico `boiler/temperature/alarm`;
3. `br.ufc.mdcc.AT03_MQTT.sensor.Sensor`: vai executar, através de threads, um conjunto de sensores que vão enviar, de tempos em tempos, as temperaturas coletadas para o tópico `boiler/temperature`. Os parâmetros de simulação da variação de temperatura podem ser configurados na chamada do construtor da classe. Também pode ser configurada a quantidade de sensores, através da variável `numSensors`.
