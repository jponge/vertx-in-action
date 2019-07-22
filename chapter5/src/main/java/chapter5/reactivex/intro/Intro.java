package chapter5.reactivex.intro;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public class Intro {

  public static void main(String[] args) throws InterruptedException {
    Observable.just(1, 2, 3)
      .map(Object::toString)
      .map(s -> "@" + s)
      .subscribe(System.out::println);

    Observable.<String>error(() -> new RuntimeException("Woops"))
      .map(String::toUpperCase)
      .subscribe(System.out::println, Throwable::printStackTrace);

    Single<String> s1 = Single.just("foo");
    Single<String> s2 = Single.just("bar");
    Flowable<String> m = Single.merge(s1, s2);
    m.subscribe(System.out::println);

    Observable
      .just("--", "this", "is", "--", "a", "sequence", "of", "items", "!")
      .doOnSubscribe(d -> System.out.println("Subscribed!"))
      .delay(5, TimeUnit.SECONDS)
      .filter(s -> !s.startsWith("--"))
      .doOnNext(x -> System.out.println("doOnNext: " + x))
      .map(String::toUpperCase)
      .buffer(2)
      .subscribe(
        pair -> System.out.println("next: " + pair),
        Throwable::printStackTrace,
        () -> System.out.println("~Done~"));

    Thread.sleep(10_000);
  }
}
