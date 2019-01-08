package chapter5.reactivex.intro;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import java.util.concurrent.TimeUnit;

public class Intro {

  public static void main(String[] args) {
    Disposable subscription = Observable
      .just("--", "this", "is", "--", "a", "sequence", "of", "items", "!")
      .doOnSubscribe(d -> System.out.println("Subscribed!"))
      .delay(5, TimeUnit.SECONDS)
      .filter(s -> !s.startsWith("--"))
      .doOnNext(System.out::println)
      .map(String::toUpperCase)
      .buffer(2)
      .subscribe(
        System.out::println,
        Throwable::printStackTrace,
        () -> System.out.println(">>> Done"));

    while (!subscription.isDisposed()) ;

    Single<String> s1 = Single.just("foo");
    Single<String> s2 = Single.just("bar");
    Flowable<String> m = Single.merge(s1, s2);
    m.subscribe(System.out::println);

    while (!subscription.isDisposed()) ;
  }
}
