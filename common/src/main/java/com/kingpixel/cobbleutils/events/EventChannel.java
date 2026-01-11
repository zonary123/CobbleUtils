package com.kingpixel.cobbleutils.events;

import java.util.ArrayList;
import java.util.List;

public class EventChannel<T> {
  private final List<EventListener<T>> listeners = new ArrayList<>();

  public void subscribe(EventListener<T> listener) {
    listeners.add(listener);
  }

  public void unsubscribe(EventListener<T> listener) {
    listeners.remove(listener);
  }

  public void emit(T data) {
    if (listeners.isEmpty()) return;
    for (EventListener<T> l : listeners) {
      l.onEvent(data);
    }
  }

  public boolean isEmpty() {
    return listeners.isEmpty();
  }
}

