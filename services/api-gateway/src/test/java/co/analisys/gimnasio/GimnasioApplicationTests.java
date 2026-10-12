package co.analisys.gimnasio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class GimnasioApplicationTests {

	@Test
	void contextLoads() {
	}

}
