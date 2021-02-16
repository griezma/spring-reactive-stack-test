package com.example.springreactive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Configuration
class WebSocketConfig {
    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
        log.info("**Websocket config picked up");
        return new SimpleUrlHandlerMapping() {
            {
                setUrlMap(Map.of("/ws/greetings", wsh));
                setOrder(10);
            }
        };
    }

    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    WebSocketHandler webSocketHandler(GreetingProducer gp) {
        return new WebSocketHandler() {
            @Override
            public Mono<Void> handle(WebSocketSession session) {
                Flux<WebSocketMessage> response = session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .map(GreetingRequest::new)
                        .flatMap(gp::greet)
                        .map(GreetingResponse::getMessage)
                        .map(session::textMessage);

                return session.send(response);
            }
        };
    }
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