# Remaining Work

- Review the way Spring Boot Kafka handles messages. We want to demonstrate that messages
  cannot be lost
- The SAGA actions are typically synchronous in nature. We should look at how we can use
  asynchronous code like WebClient
  
# Interesting Technologies

- Server Send Events (SSE) - Can be used to replace long polling (which creates a new connection for
  every pull)
  
- Reactive Kafka - Reactive API for Kafka based on project Reactor.
  https://projectreactor.io/docs/kafka/release/reference/#_introduction
  
  create a feature/reactive branch that uses the Reactive APIs

- Conduktor - Desktop interface for Kafka.