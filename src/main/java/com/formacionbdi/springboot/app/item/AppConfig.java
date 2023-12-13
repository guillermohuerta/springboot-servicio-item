package com.formacionbdi.springboot.app.item;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
public class AppConfig {

    @Bean("clienteRest")
    @LoadBalanced
    RestTemplate registrarRestTemplate() {
		return new RestTemplate();
	}
    
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizerGuillermo(){
    	return factory -> factory.configureDefault(id -> {
    		return new Resilience4JConfigBuilder(id)
    				.circuitBreakerConfig(CircuitBreakerConfig.custom()
    						.slidingWindowSize(10)
    						.failureRateThreshold(50)
    						.waitDurationInOpenState(Duration.ofSeconds(10L))
    						.permittedNumberOfCallsInHalfOpenState(5)
    						.slowCallRateThreshold(50)
    						.slowCallDurationThreshold(Duration.ofSeconds(2L))//Tiempo maximo que puede durar una llamada, si dura mas de 2s significa que es una llamada lenta, no dara timeout pero si hay mas del 50% de llamadas lentas abre circuito
    						.build())
    				.timeLimiterConfig(TimeLimiterConfig.custom()
    						.timeoutDuration(Duration.ofSeconds(3L))//Este valor es la duracion maxima para que salte al metodo alternativo y de timeout.
    						.build())
    				.build();
    	}); 
    }
}
