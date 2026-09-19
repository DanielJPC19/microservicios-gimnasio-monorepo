package co.analisys.gimnasio.exception;

/**
 * Error permanente: los datos del pago son inválidos, reintentar no cambia el resultado.
 * El mensaje se envía directo a la Dead Letter Queue sin reintentos.
 */
public class PagoInvalidoException extends RuntimeException {
    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
