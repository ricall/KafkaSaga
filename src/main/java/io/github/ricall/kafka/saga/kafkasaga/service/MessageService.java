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
import io.github.ricall.kafka.saga.kafkasaga.entity.MessageEntity;
import io.github.ricall.kafka.saga.kafkasaga.model.Message;
import io.github.ricall.kafka.saga.kafkasaga.model.StandardisedMessage;
import io.github.ricall.kafka.saga.kafkasaga.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class MessageService {

    private final KafkaTemplate<String, Message> template;
    private final KafkaProps config;
    private final MessageRepository repository;

    public Mono<SendResult<String, Message>> sendMessage(String id, Message message) {
        ProducerRecord<String, Message> record = new ProducerRecord<>(config.getTopics().getMessage(), id, message);
        record.headers().add(KafkaHeaders.CORRELATION_ID, id.getBytes(StandardCharsets.UTF_8));
        return Mono.create(callback ->
                template.send(record)
                    .addCallback(callback::success, callback::error));
    }

    public Message saveMessage(String correlationId, Message message) {
        MessageEntity entity = MessageEntity.builder()
                .correlationId(correlationId)
                .source(message.getSource())
                .destination(message.getDestination())
                .message(message.getMessage())
                .build();

        MessageEntity saved = repository.save(entity);

        message.setId(saved.getId());
        return message;
    }

    public StandardisedMessage saveStandardisedMessage(StandardisedMessage message) {
        MessageEntity entity = repository.findById(message.getId())
                .orElseThrow(IllegalArgumentException::new);

        entity.setStandardisedMessage(message.getStandardisedMessage());
        repository.save(entity);

        return message;
    }

    public Message getMessage(Long id) {
        return repository.findById(id)
                .map(entity -> Message.builder()
                    .id(entity.getId())
                    .source(entity.getSource())
                    .destination(entity.getDestination())
                    .message(entity.getMessage())
                    .build())
                .orElse(null);
    }

    public StandardisedMessage getStandardisedMessage(Long id) {
        return repository.findById(id)
                .map(entity -> StandardisedMessage.builder()
                    .id(entity.getId())
                    .source(entity.getSource())
                    .destination(entity.getDestination())
                    .message(entity.getMessage())
                    .standardisedMessage(entity.getStandardisedMessage())
                    .build())
                .orElse(null);
    }

}
