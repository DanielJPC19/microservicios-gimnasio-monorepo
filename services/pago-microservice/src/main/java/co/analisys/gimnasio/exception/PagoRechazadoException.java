package co.analisys.gimnasio.exception;

/**
 * Error de la pasarela de pagos (rechazo o indisponibilidad).
 * Se reintenta; si se agotan los intentos el mensaje termina en la Dead Letter Queue.
 */
public class PagoRechazadoException extends RuntimeException {
    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
