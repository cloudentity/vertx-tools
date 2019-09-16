How Service Discover and Circuit Breakers work

Service discovery implemented in this library blends in concept of circuit breaking. The class that handles low-level
service discovery is singleton SdVerticle. It publishes/serves information about available service nodes. It uses standard
vertx service discovery module. Every X seconds it publishes discovered nodes for service (unless the nodes of given service didn't change).
It's agnostic about the service discovery mechanism, i.e. one should register service importer (see ConsulSdProvider, FixedSdProvider)

SmartSd is a class that provides interface to service-discovery + circuit-breaking mechanism. It offers one method
`discover` that returns optional service node location (host, port, ssl) along with accompanying circuit breaker.
Every service node accessed by SmartSd has its own circuit breaker. However, there might be many SmartSd instances
accessing the same service node, hence there might be multiple circuit breakers for the same service node, but there
is no logical connection between these circuit breakers, i.e. if one circuit-breaker goes into open state, it doesn't
affect other circuit-breakers. SmartSd serves service nodes received from SdVerticle that are in healthy state.
It also checks if accompanying circuit breaker is open. SmartSd never serves service node with open circuit breaker.

SmartSd implements load balancing strategy. The strategy is chosen at the creation of SmartSd. Atm there are two
strategies: round-robin and min-load. Min-load is not tested yet - it picks a node that has minimal load value.
Round-robin is the default strategy.

SmartSd can control how the CircuitBreakers are created. The default method tries to read CircuitBreakerOptions from
configuration file, when it's missing default CircuitBreakerOptions is used.