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

package io.github.ricall.kafka.saga.kafkasaga.saga.actions;

import io.github.ricall.kafka.saga.kafkasaga.saga.SagaAction;
import io.github.ricall.kafka.saga.kafkasaga.model.Message;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageState;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageEvent;
import io.github.ricall.kafka.saga.kafkasaga.model.StandardisedMessage;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static io.github.ricall.kafka.saga.kafkasaga.saga.SagaUtilities.*;
import static io.github.ricall.kafka.saga.kafkasaga.model.MessageState.PERSISTED;
import static io.github.ricall.kafka.saga.kafkasaga.model.MessageState.STANDARDISED;
import static org.apache.commons.lang3.StringUtils.toRootUpperCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandardiseMessageAction implements SagaAction {

    private final MessageService service;

    @Override
    public MessageState handlesState() {
        return PERSISTED;
    }

    @Override
    public MessageEvent processStateChange(MessageEvent event) {
        log.info("  {} -> Standardising message: [{}]", event.getState(), event.getCorrelationId());
        Message message = service.getMessage(event.getMessageId());
        StandardisedMessage standardisedMessage = StandardisedMessage.builder()
                .id(message.getId())
                .source(message.getSource())
                .destination(message.getDestination())
                .message(message.getMessage())
                .standardisedMessage(toRootUpperCase(message.getMessage()))
                .build();

        // Randomly fail 10% of the time
        if (randomFailure(10)) {
            delay(500);
            log.warn("  Standardisation failed: [{}]", event.getCorrelationId());
            return event.toState(PERSISTED);
        }
        delayWithRandom(1000, 2000);

        service.saveStandardisedMessage(standardisedMessage);
        return event.toState(STANDARDISED);
    }

}
