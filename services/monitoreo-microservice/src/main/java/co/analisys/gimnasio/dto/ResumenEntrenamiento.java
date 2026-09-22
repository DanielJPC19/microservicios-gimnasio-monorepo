package co.analisys.gimnasio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ResumenEntrenamiento {
    private Long miembroId;
    private long totalSesiones;
    private long totalMinutos;
    private long totalCalorias;
    private double promedioMinutosPorSesion;
    private Map<String, Long> sesionesPorTipo = new HashMap<>();
    private Instant inicioVentana;
    private Instant finVentana;

    public ResumenEntrenamiento actualizar(DatosEntrenamiento datos) {
        miembroId = datos.getMiembroId();
        totalSesiones++;
        totalMinutos += datos.getDuracionMinutos();
        totalCalorias += datos.getCalorias();
        promedioMinutosPorSesion = Math.round(totalMinutos * 10.0 / totalSesiones) / 10.0;
        sesionesPorTipo.merge(datos.getTipo(), 1L, Long::sum);
        return this;
    }

    // Copia con la ventana asignada: el agregador no conoce la ventana, solo el paso posterior.
    public ResumenEntrenamiento conVentana(Instant inicio, Instant fin) {
        ResumenEntrenamiento copia = new ResumenEntrenamiento();
        copia.miembroId = miembroId;
        copia.totalSesiones = totalSesiones;
        copia.totalMinutos = totalMinutos;
        copia.totalCalorias = totalCalorias;
        copia.promedioMinutosPorSesion = promedioMinutosPorSesion;
        copia.sesionesPorTipo = new HashMap<>(sesionesPorTipo);
        copia.inicioVentana = inicio;
        copia.finVentana = fin;
        return copia;
    }
}
