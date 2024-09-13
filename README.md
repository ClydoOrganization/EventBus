# Event System Library

A Java library for flexible and extensible event management. This library allows efficient handling and dispatching of
events with support for method-based and lambda-based subscribers, including event cancellation capabilities.

## [Installation](https://jitpack.io/#ClydoNetwork/EventBus)

## Features

- **Event Management**: Register, unregister, and manage event listeners.
- **Subscriber Registry**: Maintain a map of event types to their subscribers with support for prioritization.
- **Method Subscription**: Annotate methods with `@SubscribeEvent` to handle events via reflection.
- **Lambda Subscriptions**: Subscribe to events using lambda expressions for functional event handling.
- **Event Cancellation**: Check and handle event cancellations.

## Components

### `Event`

Represents an event with the ability to be canceled.

- **`cancel()`**: Marks the event as canceled.
- **`resume()`**: Marks the event as not canceled.
- **`cancelable()`**: Checks if the event can be canceled.
- **`cancelled()`**: Checks if the event is canceled.

### `Cancelable`

Annotation to mark events that can be canceled.

### `SubscribeEvent`

Annotation to mark methods as event subscribers with configurable priority.

### `EventCaller<E>`

Functional interface for event handling.

### `LambdaSubscriber<E>`

Implementation of `EventSubscriber` that uses lambda expressions for event handling.

### `SubscriberRegistry`

Manages the registration, unregistration, and subscription of event handlers.

### `EventBus`

Central class for:

- Registering and unregistering listeners.
- Subscribing and unsubscribing to events.
- Dispatching events to registered subscribers.
- Checking if there are subscribers for a specific event type.

## Usage

1. **Register Listeners**: Use `EventBus.register(Object listener)` to register your event listeners.

2. **Subscribe to Events**: Use `EventBus.subscribe(Class<E> eventType, LambdaSubscriber<E> subscriber)` to add event
   subscribers.

3. **Dispatch Events**: Use `EventBus.call(Object rawEvent)` to dispatch events to registered subscribers.

4. **Unregister Listeners**: Use `EventBus.unregister(Object listener)` to remove your event listeners.

## Example

```java
// Define an event
public class MyEvent extends Event {
}

// Define a listener
public class MyListener {
    @SubscribeEvent
    public void onMyEvent(MyEvent event) {
        System.out.println("Event received!");
    }
}

// Register listener and dispatch event
EventBus.register(new MyListener());
EventBus.call(new MyEvent());
```

## License

See the [LICENSE](LICENSE) file for details.