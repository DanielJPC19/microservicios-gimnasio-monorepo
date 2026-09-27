package co.analisys.gimnasio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Fanout Exchange (Pub/Sub) para cambios en el horario de clases
    public static final String CLASE_HORARIO_EXCHANGE = "gimnasio.clase.horario.events";
    public static final String CLASE_HORARIO_APP_MOVIL_QUEUE = "clase.horario.app-movil";
    public static final String CLASE_HORARIO_EMAIL_QUEUE = "clase.horario.email";
    public static final String CLASE_HORARIO_AUDITORIA_QUEUE = "clase.horario.auditoria";

    @Bean
    public FanoutExchange claseHorarioExchange() {
        return new FanoutExchange(CLASE_HORARIO_EXCHANGE);
    }

    @Bean
    public Queue claseHorarioAppMovilQueue() {
        return new Queue(CLASE_HORARIO_APP_MOVIL_QUEUE, true);
    }

    @Bean
    public Queue claseHorarioEmailQueue() {
        return new Queue(CLASE_HORARIO_EMAIL_QUEUE, true);
    }

    @Bean
    public Queue claseHorarioAuditoriaQueue() {
        return new Queue(CLASE_HORARIO_AUDITORIA_QUEUE, true);
    }

    @Bean
    public Binding bindingClaseHorarioAppMovil(Queue claseHorarioAppMovilQueue, FanoutExchange claseHorarioExchange) {
        return BindingBuilder.bind(claseHorarioAppMovilQueue).to(claseHorarioExchange);
    }

    @Bean
    public Binding bindingClaseHorarioEmail(Queue claseHorarioEmailQueue, FanoutExchange claseHorarioExchange) {
        return BindingBuilder.bind(claseHorarioEmailQueue).to(claseHorarioExchange);
    }

    @Bean
    public Binding bindingClaseHorarioAuditoria(Queue claseHorarioAuditoriaQueue, FanoutExchange claseHorarioExchange) {
        return BindingBuilder.bind(claseHorarioAuditoriaQueue).to(claseHorarioExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
