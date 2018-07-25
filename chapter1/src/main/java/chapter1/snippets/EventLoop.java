package chapter1.snippets;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public final class EventLoop {

  public static final class Event {
    private final String key;
    private final Object data;

    public Event(String key, Object data) {
      this.key = key;
      this.data = data;
    }
  }

  private final ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<>();
  private final ConcurrentHashMap<String, Consumer<Object>> handlers = new ConcurrentHashMap<>();

  public EventLoop on(String key, Consumer<Object> handler) {
    handlers.put(key, handler);
    return this;
  }

  public void dispatch(Event event) {
    events.add(event);
  }

  public void run() {
    while (!(events.isEmpty() && Thread.interrupted())) {
      if (!events.isEmpty()) {
        Event event = events.pop();
        if (handlers.containsKey(event.key)) {
          handlers.get(event.key).accept(event.data);
        } else {
          System.err.println("No handler for key " + event.key);
        }
      }
    }
  }

  public void stop() {
    Thread.currentThread().interrupt();
  }

  public static void main(String[] args) {
    EventLoop eventLoop = new EventLoop();

    new Thread(() -> {
      for (int n = 0; n < 6; n++) {
        delay(1000);
        eventLoop.dispatch(new EventLoop.Event("tick", n));
      }
      eventLoop.dispatch(new EventLoop.Event("stop", null));
    }).start();

    new Thread(() -> {
      delay(2500);
      eventLoop.dispatch(new EventLoop.Event("hello", "beautiful world"));
      delay(800);
      eventLoop.dispatch(new EventLoop.Event("hello", "beautiful universe"));
    }).start();

    eventLoop.dispatch(new EventLoop.Event("hello", "world!"));
    eventLoop.dispatch(new EventLoop.Event("foo", "bar"));

    eventLoop
      .on("hello", s -> System.out.println("hello " + s))
      .on("tick", n -> System.out.println("tick #" + n))
      .on("stop", v -> eventLoop.stop())
      .run();

    System.out.println("Bye!");
  }

  private static void delay(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
