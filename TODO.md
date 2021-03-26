# Remaining Work

- Review the way Spring Boot Kafka handles messages. We want to demonstrate that messages
  cannot be lost
- The SAGA actions are typically synchronous in nature. We should look at how we can use
  asynchronous code like WebClient
- Use Spring Request Reply Template for Saga
  @KafkaListener(...)
  @SendTo(...)
  The API listener will still need to listen to all partitions so that the response goes to the correct server
- Provide a custom Acknowledge object that only acks topic/partition offsets in sequence. It needs to follow these rules:
  * The amount of outstanding acks on a topic should have an upper limit (say 50 per partition). Any attempts to
    get a Acknowledge object with more than 50 outstanding acks on this partition should block (to provide a WIP limit)
  * The Acknowledge.ack() method should return a Mono<Void> to indicate that processing is finished for this message
  * How do we handle subscriptions? How do we ensure that 2 messages with the same key are not processed at the same time?
  * We will need to add an annotation to configure the listener. 
    @AsyncListener(maxOutstandingAcksPerPartition = 20, blockMessagesWithSameKey = true)
    It will need to register as a topicPartitionChangeListener so that the listener knows which topics are assigned to
    the listener. If a rebalance occurs then acks on a lost topic should emit an error()
    
NOTE: @AsyncListener should allow us to mix synchronous and asynchronous processing. We can use JPA to write to the 
database then call a WebClient endpoint and finally ACK the message.


# Interesting Technologies

- Server Send Events (SSE) - Can be used to replace long polling (which creates a new connection for
  every pull)
  
- Reactive Kafka - Reactive API for Kafka based on project Reactor.
  https://projectreactor.io/docs/kafka/release/reference/#_introduction
  
  create a feature/reactive branch that uses the Reactive APIs

- Conduktor - Desktop interface for Kafka.