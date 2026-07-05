package org.gh7035.ieumson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IeumSonApplication {

    public static void main(String[] args) {
        SpringApplication.run(IeumSonApplication.class, args);
    }

}
