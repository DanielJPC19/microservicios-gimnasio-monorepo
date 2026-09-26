package co.analisys.gimnasio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    // Constantes Punto 2: Notificaciones de Miembros (Direct Exchange)
    public static final String MIEMBRO_EXCHANGE = "gimnasio.miembro.exchange";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";
    public static final String MIEMBRO_NOTIFICACION_QUEUE = "miembro.inscripcion.notificacion";

    // Constantes Punto 3: Avería de Equipos (Fanout Exchange - Pub/Sub)
    public static final String EQUIPO_EVENTS_EXCHANGE = "gimnasio.equipo.events";
    public static final String EQUIPO_MANTENIMIENTO_QUEUE = "equipo.averia.mantenimiento";
    public static final String EQUIPO_ENTRENADORES_QUEUE = "equipo.averia.entrenadores";
    public static final String EQUIPO_APP_SOCIOS_QUEUE = "equipo.averia.app-socios";

    // Constantes Punto 3: Cambio de Horario de Clases (Fanout Exchange - Pub/Sub)
    public static final String CLASE_HORARIO_EXCHANGE = "gimnasio.clase.horario.events";
    public static final String CLASE_HORARIO_APP_MOVIL_QUEUE = "clase.horario.app-movil";
    public static final String CLASE_HORARIO_EMAIL_QUEUE = "clase.horario.email";
    public static final String CLASE_HORARIO_AUDITORIA_QUEUE = "clase.horario.auditoria";

    // -------------------------------------------------------------
    // Beans Punto 2: Miembro Direct Exchange, Cola y Binding
    // -------------------------------------------------------------
    @Bean
    public DirectExchange miembroExchange() {
        return new DirectExchange(MIEMBRO_EXCHANGE);
    }

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

    // -------------------------------------------------------------
    // Beans Punto 3: Fanout Exchange y 3 Colas Suscritas
    // -------------------------------------------------------------
    @Bean
    public FanoutExchange equipoEventsExchange() {
        return new FanoutExchange(EQUIPO_EVENTS_EXCHANGE);
    }

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

    // -------------------------------------------------------------
    // Beans Punto 3: Fanout Exchange Cambio de Horario de Clases y 3 Colas Suscritas
    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
    // Serialización JSON para RabbitMQ
    // -------------------------------------------------------------
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
