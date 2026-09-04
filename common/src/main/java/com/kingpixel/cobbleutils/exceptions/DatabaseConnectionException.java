package com.kingpixel.cobbleutils.exceptions;

/**
 * @author Carlos Varas Alonso
 */
public class DatabaseConnectionException extends RuntimeException {
  public DatabaseConnectionException(String dbType) {
    super("Failed to connect to database of type: " + dbType);
  }

  public DatabaseConnectionException(String dbType, Throwable cause) {
    super("Failed to connect to database of type: " + dbType, cause);
  }
}
