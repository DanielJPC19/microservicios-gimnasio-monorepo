package co.analisys.gimnasio.config;

import co.analisys.gimnasio.exception.PagoInvalidoException;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Cola principal de procesamiento de pagos
    public static final String PAGOS_EXCHANGE = "gimnasio.pagos.exchange";
    public static final String PAGO_PROCESAR_ROUTING_KEY = "pago.procesar";
    public static final String PAGOS_QUEUE = "pagos.procesamiento";

    // Dead Letter Exchange y Dead Letter Queue para pagos fallidos
    public static final String PAGOS_DLX = "gimnasio.pagos.dlx";
    public static final String PAGO_FALLIDO_ROUTING_KEY = "pago.fallido";
    public static final String PAGOS_DLQ = "pagos.procesamiento.dlq";

    @Value("${pago.procesamiento.max-intentos}")
    private int maxIntentos;

    @Value("${pago.procesamiento.intervalo-inicial-ms}")
    private long intervaloInicialMs;

    @Value("${pago.procesamiento.multiplicador}")
    private double multiplicador;

    // -------------------------------------------------------------
    // Cola principal: los mensajes rechazados se desvían al DLX
    // -------------------------------------------------------------
    @Bean
    public DirectExchange pagosExchange() {
        return new DirectExchange(PAGOS_EXCHANGE);
    }

    @Bean
    public Queue pagosQueue() {
        return QueueBuilder.durable(PAGOS_QUEUE)
                .withArgument("x-dead-letter-exchange", PAGOS_DLX)
                .withArgument("x-dead-letter-routing-key", PAGO_FALLIDO_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingPagos(Queue pagosQueue, DirectExchange pagosExchange) {
        return BindingBuilder.bind(pagosQueue)
                .to(pagosExchange)
                .with(PAGO_PROCESAR_ROUTING_KEY);
    }

    // -------------------------------------------------------------
    // Dead Letter Exchange y Dead Letter Queue
    // -------------------------------------------------------------
    @Bean
    public DirectExchange pagosDeadLetterExchange() {
        return new DirectExchange(PAGOS_DLX);
    }

    @Bean
    public Queue pagosDeadLetterQueue() {
        return QueueBuilder.durable(PAGOS_DLQ).build();
    }

    @Bean
    public Binding bindingPagosDeadLetter(Queue pagosDeadLetterQueue, DirectExchange pagosDeadLetterExchange) {
        return BindingBuilder.bind(pagosDeadLetterQueue)
                .to(pagosDeadLetterExchange)
                .with(PAGO_FALLIDO_ROUTING_KEY);
    }

    // -------------------------------------------------------------
    // Reintentos: PagoRechazadoException se reintenta con backoff exponencial;
    // PagoInvalidoException no se reintenta. Al agotarse, el mensaje se rechaza
    // sin reencolar y RabbitMQ lo envía a la Dead Letter Queue.
    // -------------------------------------------------------------
    @Bean
    public MethodInterceptor pagoRetryInterceptor() {
        SimpleRetryPolicy politica = new SimpleRetryPolicy(
                maxIntentos,
                Map.<Class<? extends Throwable>, Boolean>of(PagoInvalidoException.class, false),
                true,
                true
        );

        return RetryInterceptorBuilder.stateless()
                .retryPolicy(politica)
                .backOffOptions(intervaloInicialMs, multiplicador, 10_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MethodInterceptor pagoRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(pagoRetryInterceptor);
        return factory;
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
