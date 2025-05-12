package com.app.casefakeserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//Start with
//http://localhost:8080/cas/login?service=http://localhost:8080/cas/validate

//Run the app: mvn spring-boot:run
//
//Access login:
//http://localhost:8080/cas/login?service=http://localhost:8081/myapp
//
//		After login, redirected with ticket:
//http://localhost:8081/myapp?ticket=ST-xxx
//
//Validate it:
//
//Programmatically: http://localhost:8080/cas/validate?servicehttp://localhost:8081/myapp=&ticket=ST-8f702573-cc56-4203-ba89-7a0b345175d0
//
//Manually: /cas/validate


@SpringBootApplication
public class CasefakeserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(CasefakeserverApplication.class, args);
	}

}
