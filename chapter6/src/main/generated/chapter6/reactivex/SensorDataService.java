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

import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.ObservableHelper;
import io.vertx.reactivex.FlowableHelper;
import io.vertx.reactivex.impl.AsyncResultMaybe;
import io.vertx.reactivex.impl.AsyncResultSingle;
import io.vertx.reactivex.impl.AsyncResultCompletable;
import io.vertx.reactivex.WriteStreamObserver;
import io.vertx.reactivex.WriteStreamSubscriber;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import io.vertx.lang.rx.MappingIterator;


@RxGen(chapter6.SensorDataService.class)
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

  public static final TypeArg<SensorDataService> __TYPE_ARG = new TypeArg<>(    obj -> new SensorDataService((chapter6.SensorDataService) obj),
    SensorDataService::getDelegate
  );

  private final chapter6.SensorDataService delegate;
  
  public SensorDataService(chapter6.SensorDataService delegate) {
    this.delegate = delegate;
  }

  public SensorDataService(Object delegate) {
    this.delegate = (chapter6.SensorDataService)delegate;
  }

  public chapter6.SensorDataService getDelegate() {
    return delegate;
  }

  public static chapter6.reactivex.SensorDataService create(io.vertx.reactivex.core.Vertx vertx) { 
    chapter6.reactivex.SensorDataService ret = chapter6.reactivex.SensorDataService.newInstance((chapter6.SensorDataService)chapter6.SensorDataService.create(vertx.getDelegate()));
    return ret;
  }

  public static chapter6.reactivex.SensorDataService createProxy(io.vertx.reactivex.core.Vertx vertx, String address) { 
    chapter6.reactivex.SensorDataService ret = chapter6.reactivex.SensorDataService.newInstance((chapter6.SensorDataService)chapter6.SensorDataService.createProxy(vertx.getDelegate(), address));
    return ret;
  }

  public void valueFor(String sensorId, Handler<AsyncResult<JsonObject>> handler) { 
    delegate.valueFor(sensorId, handler);
  }

  public void valueFor(String sensorId) {
    valueFor(sensorId, ar -> { });
  }

  public io.reactivex.Single<JsonObject> rxValueFor(String sensorId) { 
    return AsyncResultSingle.toSingle($handler -> {
      valueFor(sensorId, $handler);
    });
  }

  public void average(Handler<AsyncResult<JsonObject>> handler) { 
    delegate.average(handler);
  }

  public void average() {
    average(ar -> { });
  }

  public io.reactivex.Single<JsonObject> rxAverage() { 
    return AsyncResultSingle.toSingle($handler -> {
      average($handler);
    });
  }

  public static SensorDataService newInstance(chapter6.SensorDataService arg) {
    return arg != null ? new SensorDataService(arg) : null;
  }

}
