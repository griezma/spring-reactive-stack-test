package com.example.springreactive;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
//@Configuration
class R2dbcConfiguration {
    @Bean
    ConnectionFactory connectionFactory(@Value("${spring.r2dbc.url}") String url) {
        log.info("Db connection url: " + url);
        return ConnectionFactories.get(url);
    }
}
@SpringBootApplication
public class SpringReactiveServerApplication {
    private static final Logger log = LoggerFactory.getLogger(SpringReactiveServerApplication.class);

    @Bean
    ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        return initializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringReactiveServerApplication.class, args);
    }
}

@Component
@Slf4j
@RequiredArgsConstructor
class DataInitializr {
    private final ReservationRepository rr;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        var saveNames = Flux.just("Mani", "Gabi", "Susi", "Josh", "Olga", "Moni")
            .map(name -> new Reservation(null, name))
            .flatMap(rr::save);

        rr.deleteAll()
                .thenMany(saveNames)
                .subscribe(reservation -> log.info(reservation.toString()));
    }
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Long> {

    @Query("SELECT * FROM Reservation WHERE name=:name")
    Flux<Reservation> findByName(String name);

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {
    @Id
    private Long id;

    private String name;
}

@RequiredArgsConstructor
@RestController
class ReservationController {
    private final ReservationRepository rr;

    @GetMapping("/reservations")
    Flux<Reservation> getReservations() {
        return rr.findAll();
    }
}

@Configuration
class HttpConfiguration {
    @Bean
    RouterFunction<ServerResponse> routes(ReservationRepository rr) {
        return route()
                .GET("/reservations-flux", serverRequest -> ok().body(rr.findAll(), Reservation.class))
                .build();
    }
}


