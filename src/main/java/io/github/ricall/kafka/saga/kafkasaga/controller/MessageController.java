/*
 * Copyright (c) 2021 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.ricall.kafka.saga.kafkasaga.controller;

import io.github.ricall.kafka.saga.kafkasaga.model.Message;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageState;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageEvent;
import io.github.ricall.kafka.saga.kafkasaga.model.StandardisedMessage;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageService;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/v1/message")
public class MessageController {

    private final MessageService service;
    private final MessageEventService stateService;

    @PostMapping("/")
    public Mono<ResponseEntity<StandardisedMessage>> addMessage(@RequestBody Message message) {
        String correlationId = UUID.randomUUID().toString();

        return service.sendMessage(correlationId, message)
                .timeout(Duration.ofSeconds(15))
                .flatMap(this::handleResponse)
                .onErrorResume(this::handleError);
    }

    private Mono<ResponseEntity<StandardisedMessage>> handleResponse(SendResult<String, Message> result) {
        String correlationId = result.getProducerRecord().key();
        log.info("Message Sent: {}", correlationId);
        return stateService.eventsFor(correlationId)
                .filter(s -> s.getState() == MessageState.STANDARDISED)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::loadStandardisedMessage)
                .next();
    }

    private Mono<ResponseEntity<StandardisedMessage>> loadStandardisedMessage(MessageEvent stateChange) {
        StandardisedMessage standardisedMessage = service.getStandardisedMessage(stateChange.getMessageId());

        return Mono.just(ResponseEntity.ok(standardisedMessage));
    }

    private Mono<ResponseEntity<StandardisedMessage>> handleError(Throwable throwable) {
        log.error("Failed to process message", throwable);
        return Mono.just(ResponseEntity.unprocessableEntity()
                .build());
    }

}
