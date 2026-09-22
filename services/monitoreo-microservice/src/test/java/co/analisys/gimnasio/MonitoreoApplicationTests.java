package co.analisys.gimnasio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:monitoreo-test",
		"spring.kafka.listener.auto-startup=false",
		"spring.kafka.streams.auto-startup=false",
		"recuperacion.activo=false"
})
class MonitoreoApplicationTests {

	@Test
	void contextLoads() {
	}

}
