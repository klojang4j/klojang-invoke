package org.klojang.path;


import org.klojang.util.Path;

import java.util.function.Supplier;

import static org.klojang.util.ClassMethods.className;
import static org.klojang.util.ClassMethods.simpleClassName;

/**
 * Thrown by a {@link PathWalker} if a path-read or path-write error occurs.
 */
public final class DeadEndException extends RuntimeException {

  interface Factory extends Supplier<DeadEndException> {}

  private static final String INVALID_PATH = "Invalid path: \"%s\" (segment %d). ";
  private static final String PATH_SEGMENT = "Path %s, segment %s: ";

  static Factory noSuchProperty(Path path, int segment, Class<?> clazz) {
    return () -> {
      String fmt = INVALID_PATH + "No accessible property named \"%s\" in %s";
      String msg = fmt.formatted(path, segment + 1, path.segment(segment), className(clazz));
      return new DeadEndException(msg);
    };
  }

  static Factory noSuchKey(Path path, int segment, Object key) {
    return () -> {
      String fmt = INVALID_PATH + "No such key: \"%s\"";
      String msg = fmt.formatted(path, segment + 1, key);
      return new DeadEndException(msg);
    };
  }

  static Factory indexExpected(Path path, int segment) {
    return () -> {
      String fmt = INVALID_PATH + "Array or list index expected. Found: \"%s\"";
      String msg = fmt.formatted(path, segment + 1, path.segment(segment));
      return new DeadEndException(msg);
    };
  }

  static Factory indexOutOfBounds(Path path, int segment) {
    return () -> {
      String fmt = INVALID_PATH + "Index out of bounds: %s";
      String msg = fmt.formatted(path, segment + 1, path.segment(segment));
      return new DeadEndException(msg);
    };
  }

  static Factory nullValue(Path path, int segment) {
    return () -> {
      String fmt = INVALID_PATH + "Terminal value encountered at segment \"%s\": null";
      String msg = fmt.formatted(path, segment + 1, path.segment(segment));
      return new DeadEndException(msg);
    };
  }

  static Factory terminalValue(Path path, int segment, Object value) {
    return () -> {
      String fmt = INVALID_PATH + "Terminal value encountered at segment \"%s\": (%s) %s";
      String className = simpleClassName(value.getClass());
      String msg = fmt.formatted(path, segment + 1, path.segment(segment), className, value);
      return new DeadEndException(msg);
    };
  }

  static Factory emptySegment(Path path, int segment) {
    return () -> {
      String fmt = INVALID_PATH + "Segment must not be null or empty";
      String msg = fmt.formatted(path, segment + 1);
      return new DeadEndException(msg);
    };
  }

  static Factory typeMismatch(Path path, int segment, String message) {
    return () -> {
      String fmt = PATH_SEGMENT + "%s";
      String msg = fmt.formatted(path, segment + 1, message);
      return new DeadEndException(msg);
    };
  }

  static Factory typeMismatch(Path path, int segment, Class<?> expected, Class<?> actual) {
    return () -> {
      String fmt = PATH_SEGMENT + "cannot assign %s to %s";
      String scn0 = simpleClassName(expected);
      String scn1 = simpleClassName(actual);
      String msg = fmt.formatted(path, segment + 1, scn0, scn1);
      return new DeadEndException(msg);
    };
  }

  static Factory notModifiable(Path path, int segment, Class<?> type) {
    return () -> {
      String fmt = PATH_SEGMENT + "%s at segment \"%s\" not modifiable";
      String scn = simpleClassName(type);
      String msg = fmt.formatted(path, segment + 1, scn, path.segment(segment));
      return new DeadEndException(msg);
    };
  }

  static Factory segmentDeserializationFailed(Path path, int segment, Exception exc) {
    return () -> {
      String fmt;
      if (exc.getMessage() == null) {
        fmt = INVALID_PATH + "Failed to deserialize \"%s\" into map key";
      } else {
        fmt = INVALID_PATH + "Failed to deserialize \"%s\" into map key. " + exc;
      }
      String msg = fmt.formatted(path, segment + 1, path.segment(segment));
      return new DeadEndException(msg);
    };
  }

  static Factory unexpectedError(Path path, int segment, Throwable t) {
    return () -> {
      String fmt = PATH_SEGMENT + "Unexpected error. %s";
      String msg = fmt.formatted(path, segment + 1, t);
      return new DeadEndException(msg);
    };
  }

  private DeadEndException(String message) {
    super(message);
  }

}
