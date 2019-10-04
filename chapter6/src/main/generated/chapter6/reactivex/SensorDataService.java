/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package chapter6.reactivex;

import java.util.Map;
import io.reactivex.Observable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;


@io.vertx.lang.rx.RxGen(chapter6.SensorDataService.class)
public class SensorDataService {

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SensorDataService that = (SensorDataService) o;
    return delegate.equals(that.delegate);
  }
  
  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  public static final io.vertx.lang.rx.TypeArg<SensorDataService> __TYPE_ARG = new io.vertx.lang.rx.TypeArg<>(    obj -> new SensorDataService((chapter6.SensorDataService) obj),
    SensorDataService::getDelegate
  );

  private final chapter6.SensorDataService delegate;
  
  public SensorDataService(chapter6.SensorDataService delegate) {
    this.delegate = delegate;
  }

  public chapter6.SensorDataService getDelegate() {
    return delegate;
  }

  public static chapter6.reactivex.SensorDataService create(io.vertx.reactivex.core.Vertx vertx) { 
    chapter6.reactivex.SensorDataService ret = chapter6.reactivex.SensorDataService.newInstance(chapter6.SensorDataService.create(vertx.getDelegate()));
    return ret;
  }

  public static chapter6.reactivex.SensorDataService createProxy(io.vertx.reactivex.core.Vertx vertx, String address) { 
    chapter6.reactivex.SensorDataService ret = chapter6.reactivex.SensorDataService.newInstance(chapter6.SensorDataService.createProxy(vertx.getDelegate(), address));
    return ret;
  }

  public void valueFor(String sensorId, Handler<AsyncResult<JsonObject>> handler) { 
    delegate.valueFor(sensorId, handler);
  }

  public void valueFor(String sensorId) {
    valueFor(sensorId, ar -> { });
  }

  public Single<JsonObject> rxValueFor(String sensorId) { 
    return io.vertx.reactivex.impl.AsyncResultSingle.toSingle(handler -> {
      valueFor(sensorId, handler);
    });
  }

  public void average(Handler<AsyncResult<JsonObject>> handler) { 
    delegate.average(handler);
  }

  public void average() {
    average(ar -> { });
  }

  public Single<JsonObject> rxAverage() { 
    return io.vertx.reactivex.impl.AsyncResultSingle.toSingle(handler -> {
      average(handler);
    });
  }


  public static  SensorDataService newInstance(chapter6.SensorDataService arg) {
    return arg != null ? new SensorDataService(arg) : null;
  }
}
