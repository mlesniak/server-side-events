package com.mlesniak.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

record Tick(int tick) {
}

@RestController
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final ObjectMapper objectMapper;

    public EventController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/events")
    public SseEmitter events(@RequestParam(name = "count", required = false) Integer count) {
        var max = count == null ? 10 : count;
        log.info("/events called with count {} and max {}", count, max);

        var emitter = new SseEmitter(60 * 1000L);
        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            try {
                var i = 0;
                while (i < max) {
                    var tick = new Tick(i);
                    emitter.send(SseEmitter.event()
                            .name("tick")
                            .id(UUID.randomUUID().toString())
                            .data(objectMapper.writeValueAsString(tick)));
                    TimeUnit.MILLISECONDS.sleep(500);
                    i++;
                }
                // Without a payload, the event is not correctly processed
                // in the browser. This is actually expected behaviour,
                // see https://github.com/denoland/deno/issues/23135.
                emitter.send(SseEmitter.event().name("close").data(""));
                log.info("Closing connection");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
