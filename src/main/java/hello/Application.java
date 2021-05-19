
package hello;

import javax.jms.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;

import hello.pojos_out.MyMoveInt;


@SpringBootApplication
@EnableJms
public class Application {

	@Bean
	public ConnectionFactory jmsConnectionFactory() {
		RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
		connectionFactory.setUsername("guest");
		connectionFactory.setPassword("guest");
		connectionFactory.setVirtualHost("/");
		connectionFactory.setHost("10.220.16.30");
		connectionFactory.setPort(5672);
		return connectionFactory;
	}

	public static Destination jmsDestination() {
		RMQDestination jmsDestination = new RMQDestination();
		jmsDestination.setDestinationName("my_dest_name");
		jmsDestination.setAmqpExchangeName("zbrt");
		jmsDestination.setAmqpRoutingKey("zportal.prebill");

		return jmsDestination;
	}


//	@Bean // Serialize message content to json using TextMessage
//	public MessageConverter jacksonJmsMessageConverter() {
//		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//		converter.setTargetType(MessageType.TEXT);
//		converter.setTypeIdPropertyName("_type");
//		return converter;
//	}


	@Bean
	public MessageConverter messageConverter(){

		MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
		ObjectMapper objectMapper = new ObjectMapper();
		messageConverter.setObjectMapper(objectMapper);
		messageConverter.setTargetType(MessageType.TEXT);

		messageConverter.setTypeIdPropertyName("ichihedge.queue.json.classname");
		//now set idMappings for serialization/deserialization
		HashMap<String, Class<?>> idMapping = new HashMap<String, Class<?>>();
		idMapping.put(MyMoveInt.class.getName(), MyMoveInt.class);
		messageConverter.setTypeIdMappings(idMapping);

		return messageConverter;
	}

	public static void main(String[] args) throws JsonProcessingException {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

		JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
		String messageStr =  "{\"message\":{\"classId\":\"32\",\"messageHeader\":{\"version\":\"1.0\",\"sender\":\"TOS\",\"receiver\":\"WEB\",\"created\":\"2021-05-19T00:20:26\",\"messageId\":\"55bdf069-6a2b-4cf2-b2c4-af953eac9883\"},\"msgBody\":{\"move_int\":{\"action\":\"save\",\"action_date\":\"2021-05-19T00:20:26\",\"terminal_syncid\":\"BRT/TOS-V6/Stakeholder/8\",\"move_time\":\"2021-05-19T00:20:26\",\"container\":{\"cnt_syncid\":\"BRT/BRT-TOS/Container/52060\",\"cnt_number\":\"TGHU1997126\"},\"loader\":{\"loader_syncid\":\"BRT-TEST/TOS/Loaders/4879\",\"loader_name\":\"RS02\"},\"begin_cell\":\"B43-3-4\",\"begin_tier\":\"2\",\"end_cell\":\"B45-3-5\",\"end_tier\":\"2\",\"from_wh\":\"WFULL\",\"to_wh\":\"WFULL\",\"move_syncid\":\"CTTK/TOS-V64/CNT_OPERATION/242619\",\"move_type\":\"W\",\"work_cnt_syncid\":null,\"work_type\":\"AP\",\"worker\":{\"worker_syncid\":\"BRT/BRT-TOS/Worker/5606\",\"worker_name\":\"DEN\"}}}},\"Event-Category\":\"prebill\",\"Event-Name\":\"move_int\"}";
		ObjectMapper objectMpr = new ObjectMapper();
		objectMpr.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
		objectMpr.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MyMoveInt myMoveInt = objectMpr.readValue(messageStr, MyMoveInt.class);

		System.out.println("Sending move_int message.");

//		jmsTemplate.convertAndSend(jmsDestination(), myMoveInt);

//		jmsTemplate.convertAndSend(jmsDestination(), myMoveInt, s -> {
//
//			s.setStringProperty("contentType", "application/json");
//			s.setStringProperty("content_type", "application/json");
//			s.setStringProperty("content-type", "application/json");
//			return s;
//		});

		jmsTemplate.convertAndSend(jmsDestination(), myMoveInt, new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				message.setStringProperty("my_property", "my_value");
				message.setStringProperty("contentType", "application/json");
				message.setStringProperty("content_type", "application/json");
				message.setStringProperty("content-type", "application/json");

				return message;
			}
		});

	}

}
