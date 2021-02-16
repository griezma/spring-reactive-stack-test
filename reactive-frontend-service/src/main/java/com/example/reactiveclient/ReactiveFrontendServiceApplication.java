package com.example.reactiveclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class ReactiveFrontendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveClientApplication.class, args);
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction(ReservationClient rc) {
        return route()
                .GET("/reservations/names", serverRequest -> {
                   Flux<String> names = rc.getReservations()
                           .map(Reservation::getName)
                           .onErrorResume(e -> Flux.just("EEEK!"));
                   return ServerResponse.ok().body(names, String.class);
                })
                .build();
    }
}

@Component
@RequiredArgsConstructor
class ReservationClient {
    private final WebClient wc;

    Flux<Reservation> getReservations() {
        return wc.get().uri("http://localhost:8080/reservations-flux")
                .retrieve()
                .bodyToFlux(Reservation.class);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {
    private Long id;

    private String name;
}
@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingRequest {
    String name;
}
@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingResponse {
    String message;
}
@Component
class GreetingProducer {
    Flux<GreetingResponse> greet(GreetingRequest request) {
        return Flux.fromStream(Stream.generate(() ->
                new GreetingResponse("Hello " + request.getName() + " @ " + Instant.now())))
                .delayElements(Duration.ofSeconds(1));
    }
}