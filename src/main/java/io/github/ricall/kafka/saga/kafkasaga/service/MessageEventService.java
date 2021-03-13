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

package io.github.ricall.kafka.saga.kafkasaga.service;

import io.github.ricall.kafka.saga.kafkasaga.configuration.KafkaProps;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import static io.github.ricall.kafka.saga.kafkasaga.model.MessageState.PROCESSED;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventService {

    private final KafkaTemplate<String, MessageEvent> template;
    private final KafkaProps config;

    private transient Many<MessageEvent> sink = Sinks.many()
            .multicast()
            .directBestEffort();

    public Mono<SendResult<String, MessageEvent>> saveEvent(MessageEvent event) {
        String key = event.getCorrelationId();
        MessageEvent value = event.getState() == PROCESSED ? null : event;

        // PROCESSED events will be written as NULL (tombstone)
        return Mono.create(callback -> template
                .send(config.getTopics().getMessageEvent(), key, value)
                .addCallback(callback::success, callback::error));
    }

    public Flux<MessageEvent> eventsFor(String correlationId) {
        return sink.asFlux()
                .filter(state -> correlationId.equals(state.getCorrelationId()));
    }

    @KafkaListener(
            topics = "${kafka.topics.messageEvent}",
            groupId = "event.service${INSTANCE_ID:1}",
            topicPartitions = @TopicPartition(
                    topic = "${kafka.topics.messageEvent}",
                    partitions = "0-#{${kafka.partitions}-1}",
                    partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0", relativeToCurrent = "true")),
            containerFactory = "latestJsonListenerFactory"
    )
    public void onMessageEvent(@Payload(required = false) MessageEvent event) {
        if (event != null) {
            sink.tryEmitNext(event);
        }
    }
}
