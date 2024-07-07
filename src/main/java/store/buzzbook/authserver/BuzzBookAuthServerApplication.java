package store.buzzbook.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class BuzzBookAuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuzzBookAuthServerApplication.class, args);
	}

}
