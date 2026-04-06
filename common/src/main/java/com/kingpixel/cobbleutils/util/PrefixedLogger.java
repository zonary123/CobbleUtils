package com.kingpixel.cobbleutils.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.io.Serial;
import java.util.Objects;

public class PrefixedLogger extends AbstractLogger {

  private final transient AbstractLogger delegate;
  private final String modId;

  public PrefixedLogger(Logger delegate, String modId) {
    super(delegate.getName(), delegate.getMessageFactory());
    this.delegate = requireAbstractLogger(delegate);
    this.modId = normalizeModId(modId, delegate.getName());
  }

  @Override
  public Level getLevel() {
    return delegate.getLevel();
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
    return delegate.isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t) {
    return delegate.isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
    return delegate.isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
    return delegate.isEnabled(level, marker, message, t);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message) {
    return delegate.isEnabled(level, marker, message);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
    return delegate.isEnabled(level, marker, message, params);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0) {
    return delegate.isEnabled(level, marker, message, p0);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1) {
    return delegate.isEnabled(level, marker, message, p0, p1);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
    return delegate.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  @Override
  public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
    Message prefixedMessage = new PrefixedMessage(modId, message);
    Throwable throwable = t != null ? t : prefixedMessage.getThrowable();
    delegate.logMessage(fqcn, level, marker, prefixedMessage, throwable);
  }

  private static AbstractLogger requireAbstractLogger(Logger delegate) {
    if (delegate instanceof AbstractLogger abstractLogger) {
      return abstractLogger;
    }
    throw new IllegalArgumentException("Delegate logger must extend AbstractLogger: " + delegate.getClass().getName());
  }

  protected static String normalizeModId(String modId, String fallback) {
    if (modId == null || modId.isBlank()) {
      return Objects.requireNonNullElse(fallback, "unknown");
    }
    return modId;
  }

  private static final class PrefixedMessage implements Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String modId;
    private final Message delegate;

    private PrefixedMessage(String modId, Message delegate) {
      this.modId = normalizeModId(modId, "unknown");
      this.delegate = delegate;
    }

    @Override
    public String getFormattedMessage() {
      return "[" + modId + "] " + getDelegateFormattedMessage();
    }

    @Override
    public String getFormat() {
      return "[" + modId + "] " + (delegate == null ? "null" : delegate.getFormat());
    }

    @Override
    public Object[] getParameters() {
      return delegate == null ? null : delegate.getParameters();
    }

    @Override
    public Throwable getThrowable() {
      return delegate == null ? null : delegate.getThrowable();
    }

    private String getDelegateFormattedMessage() {
      return delegate == null ? "null" : delegate.getFormattedMessage();
    }
  }
}