# EventBus

A small, fast, type-safe publish/subscribe event bus for Java 17+.

- Lock-free, allocation-free posting; the per-class dispatch plan is cached
- Typed functional listeners and `@Subscribe` methods
- Deterministic ordering: priority, then subscription order
- Event inheritance: listeners of a supertype or interface receive subtypes
- `Subscription` handles with try-with-resources, one-shot and limited subscriptions, filters
- Cancellation and propagation stopping as separate concepts
- Per-listener async dispatch, `postAsync`, and `await` returning a `CompletableFuture`
- Typed `EventKey` channels for events that aren't identified by their class
- Configurable error handling, dispatch tracing, leak warnings, weak registrations

## [Installation](https://jitpack.io/#ClydoNetwork/EventBus)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.ClydoNetwork:EventBus:<version>")
}
```

## Quick start

```java
public class BlockBreakEvent extends CancellableEvent {

    private final Block block;

    public BlockBreakEvent(Block block) {
        this.block = block;
    }

    public Block block() {
        return this.block;
    }

}

public class Game {

    private final EventBus bus = new EventBus();

    void start(Block block) {
        // Functional listener
        Subscription subscription = this.bus.subscribe(PlayerJoinEvent.class, event -> greet(event.player()));

        // Annotated listener object
        this.bus.register(new ChatListener());

        // Post; the event is returned so its final state can be read inline
        if (!this.bus.post(new BlockBreakEvent(block)).isCancelled()) {
            block.destroy();
        }

        subscription.unsubscribe();
    }

}
```

Any object can be an event, records included. The optional base classes in
`net.clydo.eventbus.event` add cancellation and propagation stopping.

## Packages

| Package                         | Contents                                                                                                             |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `net.clydo.eventbus`            | `EventBus` and its configuration: `EventErrorHandler`, `EventTracer`, `EventDispatchException`                       |
| `net.clydo.eventbus.event`      | The event model: `Event`, `Cancellable`, `CancellableEvent`, `EventKey`                                              |
| `net.clydo.eventbus.subscriber` | Subscribing: `Subscribe`, `EventListener`, `EventPriority`, `DispatchMode`, `Subscription`, `SubscriptionOptions`    |
| `net.clydo.eventbus.internal`   | The implementation (`EventDispatcher`, `SubscriberRegistry`, method scanning). Marked `@ApiStatus.Internal`; not API |

The API packages contain only API types; `EventBus` validates arguments and delegates to the
internal `EventDispatcher`.

## Listening

### Functional listeners

```java
void listen(EventBus bus) {
    bus.subscribe(ChatEvent.class, event -> log(event.message()));

    bus.subscribe(
            ChatEvent.class,
            SubscriptionOptions.defaults()
                    .withPriority(EventPriority.HIGH)
                    .withIgnoreCancelled(true),
            event -> moderate(event)
    );

    // Filtered: rejected events don't count towards maxInvocations
    bus.subscribe(
            ChatEvent.class,
            SubscriptionOptions.once(),
            event -> event.message().startsWith("!"),
            event -> runCommand(event)
    );

    // Several at once, in order, behind one Subscription
    Subscription both = bus.subscribeAll(ChatEvent.class, this::onChat, this::onChatLogged);
}

void remove(EventBus bus, EventListener<ChatEvent> listener, EventListener<ChatEvent> other) {
    // Remove by listener identity when the Subscription isn't at hand
    bus.unsubscribe(ChatEvent.class, listener);
    bus.unsubscribeAll(ChatEvent.class, listener, other);
}
```

`EventListener.onEvent` may throw checked exceptions; they go to the bus's `EventErrorHandler`.

### Annotated listeners

```java
public class ChatListener {

    @Subscribe(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onChat(ChatEvent event) {
        // ...
    }

    @Subscribe(mode = DispatchMode.ASYNC)
    void audit(ChatEvent event) throws IOException {
        // ...
    }

}

void register(EventBus bus, ChatListener chat, ModerationListener moderation, AuditListener audit) {
    Subscription registration = bus.register(chat);  // covers every @Subscribe method
    bus.registerWeak(moderation);                    // released when the listener is collected
    bus.unregister(chat);                            // by identity

    bus.registerAll(chat, moderation, audit);        // all or nothing
    bus.unregisterAll(chat, moderation, audit);
}
```

Methods may have any visibility, must return `void`, and must take exactly one non-primitive
parameter. Inherited annotated methods are registered too, and overriding one does not register it
twice. Each class is scanned once. Invocation then goes through a cached `MethodHandle`, with no
per-call reflection. In a named module, open the listener's package to the event bus.

### Subscriptions

`Subscription` is a `CloseableScope` from ClytilLib, so it works with try-with-resources and
Clytil's `closeThen` / `closeOnce` / `runAndClose`. Its own helpers combine handles while keeping
them `Subscription`s:

```java
private Subscription active = Subscription.empty();  // inert starting value, never null

void enable(EventBus bus) {
    this.active = bus.subscribe(JoinEvent.class, this::onJoin)
            .and(bus.subscribe(ChatEvent.class, this::onChat))
            .and(bus.subscribe(QuitEvent.class, this::onQuit));  // flat, however long the chain
}

void disable() {
    this.active.unsubscribe();
    this.active = Subscription.empty();
}

void scoped(EventBus bus, List<Subscription> handles) {
    try (Subscription temporary = bus.subscribe(TickEvent.class, this::onTick)) {
        runForAWhile();
    }

    Subscription batch = Subscription.all(handles);  // varargs or any Collection
    boolean active = batch.isActive();                // false after unsubscribe, after the last allowed
                                                      // invocation, or after a weak listener was collected
}
```

Once `unsubscribe()` returns, the listener is never invoked by a dispatch that starts afterwards.

### Awaiting an event

```java
void welcomeNext(EventBus bus, Player target) {
    bus.await(PlayerJoinEvent.class, event -> event.player().equals(target))
            .orTimeout(10, TimeUnit.SECONDS)
            .thenAccept(this::welcome);
}
```

The future completes after every other listener has run, so it sees the event's final state.
Canceling the future, or letting it time out, unsubscribes it.

## Dispatch semantics

| Concept              | Behaviour                                                                                                                                                                  |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Order                | Higher `priority` first; ties in subscription order, across the whole type hierarchy.                                                                                      |
| Hierarchy            | A posted event reaches listeners of its class, superclasses and interfaces. `subscribe(Object.class, …)` sees every class-based event.                                     |
| Cancellation         | `Cancellable` (or `CancellableEvent`) is a veto flag. Canceled events keep propagating; listeners with `ignoreCancelled` skip them, and later listeners may `resume()` them. |
| Propagation          | `Event.stopPropagation()` prevents every later listener from receiving the event. It is independent of cancellation.                                                       |
| `DispatchMode.SYNC`  | Runs on the posting thread, in order. The default.                                                                                                                         |
| `DispatchMode.ASYNC` | Handed to the bus executor at its priority position. Filters and invocation limits are applied at dispatch time; the listener should treat the event as read-only.         |
| `postAsync`          | Runs the whole dispatch on the executor and completes with the event.                                                                                                      |
| Re-entrancy          | Listeners may post, subscribe and unsubscribe. New subscriptions apply from the next post.                                                                                 |

## Event keys

When the payload class alone doesn't identify the event, use a typed key. Keyed dispatch is
exact: it doesn't involve class hierarchies.

```java
public static final EventKey<String> CHAT = EventKey.of("chat", String.class);

void chat(EventBus bus) {
    bus.subscribe(CHAT, message -> System.out.println(message));
    bus.post(CHAT, "hello");

    CompletableFuture<String> next = bus.await(CHAT);
}
```

## Configuration

```java
private final EventBus bus = EventBus.builder()
        .executor(Threads.newCachedPool("events-%d", null))  // ClytilLib; runs ASYNC listeners and postAsync. Default: common ForkJoinPool
        .errorHandler(EventErrorHandler.rethrowing())       // default: logging()
        .tracer(EventTracer.logging())                      // default: none, which costs nothing
        .listenerWarningThreshold(64)                       // warn once per type above 64 listeners. Default: off
        .build();
```

- **`EventErrorHandler`** receives the exception, the event and the failing `Subscription`, which
  it may unsubscribe. Returning normally continues the dispatch; throwing aborts it. `logging()`
  logs through `System.Logger` and continues. `rethrowing()` throws an `EventDispatchException`.
  `Error`s always propagate.
- **`EventTracer`** sees `onPost(event, subscriberCount)`; a count of 0 means a dead event. It
  also sees `onDelivered(event, subscription, durationNanos)` for each invocation.

For code that has no better place to get a bus from, `EventBus.global()` returns a shared
instance with the default configuration. Passing your own instances around is preferred.

## Introspection

```java
void inspect(EventBus bus) {
    boolean wanted = bus.hasSubscribers(ExpensiveEvent.class);  // skip building events nobody listens to
    int listeners = bus.subscriberCount(ChildEvent.class);      // includes listeners of supertypes
    int chatListeners = bus.subscriberCount(CHAT);
}
```

## Bridging

Bridging is just a subscription:

```java
void bridge(EventBus source, EventBus target) {
    Subscription everything = source.subscribe(Object.class, target::post);
    Subscription chatOnly = source.subscribe(ChatEvent.class, target::post);
}
```

Avoid cycles between buses, because they recurse.

## Performance

Measured with JMH on one development machine (JDK 17, average time per `post`). **No post path
allocates**, including render frames and concurrent posting.

| Benchmark                                               | ns/op |
|---------------------------------------------------------|-------|
| Direct listener call (baseline)                         | 2.4   |
| Post, no listeners                                      | 2.8   |
| Post, 1 lambda                                          | 5.2   |
| Post, 1 `@Subscribe` method                             | 8.5   |
| Post, 10 lambdas                                        | 27.4  |
| Post through a 3-level hierarchy, 3 listeners           | 20.9  |
| Render frame: reset a reused event, post to 3 listeners | 22.7  |
| Post from 4 threads at once, per post                   | 10.0  |
| Subscribe + unsubscribe                                 | 132.9 |

How it stays fast:

- Each posted class maps to a cached, pre-merged, pre-sorted listener array, so steady-state
  dispatch is one lock-free table lookup plus an array walk.
- A listener without filter, limit, async mode or weak reference (and no tracer on the bus) takes
  a direct path: one volatile read, one branch, one call.
- Writes copy only the affected array and drop only the affected cache entries.
- Whether an event class is `Event` or `Cancellable` is computed once per class. On JDK 17 a *failing* `instanceof`
  against an interface costs tens of nanoseconds.
- Limited subscriptions claim invocations with a single CAS, so their limit is exact even under
  concurrent posting.

Compile-time generated invokers and `LambdaMetafactory` were considered and left out. A cached
`MethodHandle` keeps annotated listeners about 3 ns behind lambdas. Generated lambda classes would be
tied to the bus's class loader and would pin listener class loaders.

## Memory

The bus holds nothing beyond what is subscribed. This was verified through the garbage collector,
by loading event and listener classes into throwaway class loaders and checking that those
loaders unload.

- A discarded bus is fully collectible, along with everything subscribed to it.
- The route cache holds posted event classes **weakly**. Posting a class never pins it or its
  class loader, even on an application-lifetime bus.
- Per-class metadata (hierarchies, `@Subscribe` method handles) lives in `ClassValue`s attached to
  the classes themselves, so it unloads with them.
- Unsubscribed listeners are released immediately. `registerWeak` listeners are purged as soon as
  they are collected, on the next subscribe or unsubscribe, even if their events are never posted.

You are responsible for:

- Unsubscribing strong registrations you no longer need. `listenerWarningThreshold` helps find
  forgotten ones.
- Completing, canceling or timing out `await` futures. A future that never completes keeps its
  subscription.

## Render loops

For per-frame events:

```java
private final RenderFrameEvent frame = new RenderFrameEvent();  // reused every frame

void render(float partialTick) {
    if (!this.bus.hasSubscribers(RenderFrameEvent.class)) {     // skip all work when nobody listens
        return;
    }
    this.frame.reset();                                          // clears propagation and cancellation
    this.frame.setPartialTick(partialTick);
    this.bus.post(this.frame);
}
```

- Reuse one event instance and call `reset()` before each post. `Event.reset()` clears propagation;
  `CancellableEvent.reset()` also clears cancellation. Subclasses with their own per-frame state
  override it and call `super.reset()`.
- Keep hot listeners on the direct path. Filters, invocation limits, `ASYNC` mode and weak
  registration each add a little work per event, and a tracer adds timing to every listener.
- Prefer lambdas or method references over `@Subscribe` methods for the hottest listeners.
- Posting is lock-free, so a render thread never waits on other threads subscribing or
  unsubscribing.

## Upgrading from 1.0

| Before                                                                                     | Now                                                                                               |
|--------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| static `EventBus.call(event)` returning whether it was canceled                            | `bus.post(event)` returning the event (`EventBus.global()` for a shared bus)                      |
| `event.SubscribeEvent`                                                                     | `subscriber.Subscribe` (adds `mode` and `ignoreCancelled`)                                        |
| `event.StandardPriorities` (lower ran first)                                               | `subscriber.EventPriority` (higher runs first)                                                    |
| `@Cancelable` + `Event.cancel()` / `resume()` / `cancelled()`                              | `Cancellable` (`cancel()`, `resume()`, `isCancelled()`) or `CancellableEvent`                     |
| `subscriber.EventCaller`                                                                   | `subscriber.EventListener`                                                                        |
| `subscribe(type, new LambdaSubscriber<>(priority, caller))`                                | `subscribe(type, SubscriptionOptions.defaults().withPriority(priority), listener)`                |
| `unsubscribe(type, subscriber)` / `unsubscribeAll(type, …)`                                | same names, matching listeners by identity, or `subscription.unsubscribe()`                       |
| `registerAll` / `unregisterAll` / `subscribeAll`                                           | same names; `registerAll` is now all-or-nothing                                                   |
| `hasSub(type)`                                                                             | `hasSubscribers(type)`                                                                            |
| `exception.InvokeEventException` (checked)                                                 | `EventErrorHandler`; `EventDispatchException` with `rethrowing()`                                 |
| public `SubscriberRegistry`, `EventSubscriber`, `MethodSubscriber`, `MethodSubscriberUtil` | `internal` package (`SubscriberRegistry`, `EventSubscriber`, `MethodListener`, `ListenerMethods`) |

## License

See the [LICENSE](LICENSE) file for details.
