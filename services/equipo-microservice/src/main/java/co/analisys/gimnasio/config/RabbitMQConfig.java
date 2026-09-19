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

    public static final String EQUIPO_EVENTS_EXCHANGE = "gimnasio.equipo.events";
    public static final String EQUIPO_MANTENIMIENTO_QUEUE = "equipo.averia.mantenimiento";
    public static final String EQUIPO_ENTRENADORES_QUEUE = "equipo.averia.entrenadores";
    public static final String EQUIPO_APP_SOCIOS_QUEUE = "equipo.averia.app-socios";

    @Bean
    public FanoutExchange equipoEventsExchange() {
        return new FanoutExchange(EQUIPO_EVENTS_EXCHANGE);
    }

    // Las colas suscritas también se declaran en el productor (declaración idempotente):
    // si notificacion-service aún no ha arrancado, los eventos quedan encolados en lugar de descartarse.
    @Bean
    public Queue equipoMantenimientoQueue() {
        return new Queue(EQUIPO_MANTENIMIENTO_QUEUE, true);
    }

    @Bean
    public Queue equipoEntrenadoresQueue() {
        return new Queue(EQUIPO_ENTRENADORES_QUEUE, true);
    }

    @Bean
    public Queue equipoAppSociosQueue() {
        return new Queue(EQUIPO_APP_SOCIOS_QUEUE, true);
    }

    @Bean
    public Binding bindingEquipoMantenimiento(Queue equipoMantenimientoQueue, FanoutExchange equipoEventsExchange) {
        return BindingBuilder.bind(equipoMantenimientoQueue).to(equipoEventsExchange);
    }

    @Bean
    public Binding bindingEquipoEntrenadores(Queue equipoEntrenadoresQueue, FanoutExchange equipoEventsExchange) {
        return BindingBuilder.bind(equipoEntrenadoresQueue).to(equipoEventsExchange);
    }

    @Bean
    public Binding bindingEquipoAppSocios(Queue equipoAppSociosQueue, FanoutExchange equipoEventsExchange) {
        return BindingBuilder.bind(equipoAppSociosQueue).to(equipoEventsExchange);
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
