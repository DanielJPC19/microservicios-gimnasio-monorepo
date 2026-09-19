package co.analisys.gimnasio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MIEMBRO_EXCHANGE = "gimnasio.miembro.exchange";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";
    public static final String MIEMBRO_NOTIFICACION_QUEUE = "miembro.inscripcion.notificacion";

    @Bean
    public DirectExchange miembroExchange() {
        return new DirectExchange(MIEMBRO_EXCHANGE);
    }

    // La cola y el binding también se declaran en el productor (declaración idempotente):
    // si notificacion-service aún no ha arrancado, el evento queda encolado en lugar de descartarse.
    @Bean
    public Queue miembroNotificacionQueue() {
        return new Queue(MIEMBRO_NOTIFICACION_QUEUE, true);
    }

    @Bean
    public Binding bindingMiembroInscrito(Queue miembroNotificacionQueue, DirectExchange miembroExchange) {
        return BindingBuilder.bind(miembroNotificacionQueue)
                .to(miembroExchange)
                .with(MIEMBRO_INSCRITO_ROUTING_KEY);
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
