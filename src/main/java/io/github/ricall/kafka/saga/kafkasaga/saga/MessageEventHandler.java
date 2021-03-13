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

package io.github.ricall.kafka.saga.kafkasaga.saga;

import io.github.ricall.kafka.saga.kafkasaga.model.MessageState;
import io.github.ricall.kafka.saga.kafkasaga.model.MessageEvent;
import io.github.ricall.kafka.saga.kafkasaga.service.MessageEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.ricall.kafka.saga.kafkasaga.model.MessageState.PROCESSED;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventHandler {

    private final MessageEventService stateService;
    private final List<SagaAction> actionList;

    private Map<MessageState, SagaAction> actions;

    @PostConstruct
    public void init() {
        actions = actionList.stream()
                .collect(Collectors.toMap(SagaAction::handlesState, Function.identity()));
    }

    @KafkaListener(
            topics = "${kafka.topics.messageEvent}",
            groupId = "event.handler",
            containerFactory = "jsonListenerFactory"
    )
    public void onMessageEvent(@Payload MessageEvent event) {
        SagaAction action = actions.get(event.getState());

        MessageEvent nextEvent;
        if (action == null) {
            log.warn("Unsupported state change: {}", event);
            nextEvent = event.toState(PROCESSED);
        } else {
            nextEvent = action.processStateChange(event);
        }
        stateService.saveEvent(nextEvent).block();
    }

}
