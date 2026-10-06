package fr.auclairdeso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AuClairDeSoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuClairDeSoApplication.class, args);
	}

}
