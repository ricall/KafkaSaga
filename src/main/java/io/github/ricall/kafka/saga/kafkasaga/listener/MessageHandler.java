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

package io.github.ricall.kafka.saga.kafkasaga.listener;

import io.github.ricall.kafka.saga.kafkasaga.Kafka.AsyncAcknowledge;
import io.github.ricall.kafka.saga.kafkasaga.model.Message;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageEvent;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageService;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static io.github.ricall.kafka.saga.kafkasaga.model.MessageState.PERSISTED;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler {

    private final MessageService service;
    private final MessageEventService eventService;

    @KafkaListener(
            topics = "${kafka.topics.message}",
            groupId = "message-handler",
            containerFactory = "jsonListenerFactory"
    )
    public void onMessage(@Header(KafkaHeaders.CORRELATION_ID) String correlationId, Message message, Acknowledgment ack) {
        log.info("Received message {} [{}]", message, correlationId);

        message = service.saveMessage(correlationId, message);
        log.info("  Wrote message: [{}]", correlationId);

        eventService.saveEvent(MessageEvent.builder()
                .correlationId(correlationId)
                .messageId(message.getId())
                .state(PERSISTED)
                .build()).block();
        ack.acknowledge();
    }

}
