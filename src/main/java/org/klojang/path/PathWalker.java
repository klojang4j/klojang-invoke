package org.klojang.path;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonProperties.size;
import static org.klojang.util.ClassMethods.cast;

/**
 * <p>A {@code PathWalker} lets you read from, and write to objects using {@link Path} objects. The value you
 * read or write can be deeply nested inside the object. The {@code Path} object specifies where inside the
 * object the value is located. A {@code PathWalker} can read almost any type of object it encounters as it
 * walks down the path towards the last path segment: JavaBeans, records, maps, collections, arrays and scalar
 * values. It can also write to most of them. The {@code PathWalker} class has various use cases:
 * <ul>
 *   <li>When processing large batches of sparsely populated objects
 *   <li>When processing large batches of variously typed objects
 *   <li>When it does not really matter whether a deeply nested value is {@code null} or just not present at
 *   all
 *   <li>To keep your code concise and clean when reading a deeply nested value.
 * </ul>
 * <p>By default, a {@code PathWalker} will not throw an exception if it cannot read or write a value
 * &#8212; that is, if it cannot walk a path all the way down to the last path segment. That would defy the
 * purposes listed above. Instead, it just returns {@link Result#notAvailable()} when reading values and
 * {@code false} when writing values. However, the {@code PathWalker} contains a constructor that enables
 * you to enable and disable exception suppression. Without exception suppression a {@code PathWalker} will
 * throw a {@link DeadEndException} when failing to read/write a value, which may be useful when debugging.
 *
 * @author Ayco Holleman
 */
@SuppressWarnings({"unchecked"})
public final class PathWalker {

  /**
   * Returns the value at the specified path. If the value could not be read a
   * {@link java.util.NoSuchElementException} exception is thrown (see {@link Result#get()}). This method is
   * useful if you already know for sure that the specified path can be traced through the specified host
   * object.
   *
   * @param host the object from which to read the value
   * @param path the path specifying where to find the value
   * @param <T> the type of the value
   * @return the value at the specified path
   */
  public static <T> T get(Object host, String path) {
    return (T) read(host, path).get();
  }

  /**
   * Returns a {@code Result} object containing the value at the specified path or
   * {@link Result#notAvailable()} if the value could not be retrieved.
   *
   * @param host the object from which to read the value
   * @param path the path specifying where to find the value
   * @param <T> the type of the value
   * @return a {@code Result} object containing the value at the specified path or
   *     {@link Result#notAvailable()} if the value could not be retrieved
   */
  public static <T> Result<T> read(Object host, String path) {
    return new PathWalker(path).read(host);
  }

  private static final String PATHS = "paths";

  private final List<Path> paths;
  private final boolean suppressExceptions;
  private final PathSegmentDeserializer segmentDeserializer;

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths One or more paths representing possibly deeply-nested properties
   */
  public PathWalker(Path... paths) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    this.paths = List.of(paths);
    this.suppressExceptions = true;
    this.segmentDeserializer = null;
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The paths to walk through the provided host objects
   */
  public PathWalker(String... paths) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    this.paths = Arrays.stream(paths).map(Path::from).toList();
    this.suppressExceptions = true;
    this.segmentDeserializer = null;
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The paths to walk through the provided host objects
   */
  public PathWalker(List<Path> paths) {
    this(paths, true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The action to take if a path could not be read or written
   * @param suppressExceptions If {@code true}, the {@code read} methods will return {@code null} for
   *     paths that could not be read. The {@code write} methods will quietly return without having written
   *     the value. If {@code false}, a {@link DeadEndException} will be thrown detailing the error.
   */
  public PathWalker(List<Path> paths, boolean suppressExceptions) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    this.paths = List.copyOf(paths);
    this.suppressExceptions = suppressExceptions;
    this.segmentDeserializer = null;
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The paths to walk
   * @param suppressExceptions If {@code true}, the {@code read} methods will return {@code null} for
   *     paths that could not be read. The {@code write} methods will quietly return without having written
   *     the value. If {@code false}, a {@link DeadEndException} will be thrown detailing the error.
   * @param segmentDeserializer A function that converts path segments to map keys. You need to provide
   *     this when reading from, or writing to {@code Map} objects with a non-String key type.
   */
  public PathWalker(
      List<Path> paths,
      boolean suppressExceptions,
      PathSegmentDeserializer segmentDeserializer) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    Check.notNull(segmentDeserializer, "segment deserializer");
    this.paths = List.copyOf(paths);
    this.suppressExceptions = suppressExceptions;
    this.segmentDeserializer = segmentDeserializer;
  }

  // For internal use
  PathWalker(Path path, boolean suppressExceptions, PathSegmentDeserializer segmentDeserializer) {
    this.paths = List.of(path);
    this.suppressExceptions = suppressExceptions;
    this.segmentDeserializer = segmentDeserializer;
  }

  /**
   * Returns the values of all paths specified through the constructor.
   *
   * @param host the object to read the values from
   * @return the values of all paths specified through the constructor
   * @throws DeadEndException If {@code suppressExceptions} is false and the {@code PathWalker} fails to
   *     retrieve the values of one or more paths.
   */
  public List<Result<Object>> readValues(Object host) throws DeadEndException {
    ObjectReader reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    return paths.stream().map(path -> reader.read(host, path, 0)).toList();
  }

  /**
   * Reads the value of the first path specified through the constructor. Convenient if you specified just one
   * path.
   *
   * @param <T> The type of the value being returned
   * @param host the object from which to read the value
   * @return the value of the first path specified through the constructor
   * @throws DeadEndException If {@code suppressExceptions} is false and the {@code PathWalker} fails to
   *     retrieve the value of the first path.
   */
  public <T> Result<T> read(Object host) {
    return cast(new ObjectReader(suppressExceptions, segmentDeserializer).read(host, paths.getFirst(), 0));
  }

  /**
   * Sets the values of the paths specified through the constructor. The provided array of values must have
   * the same length as the number of paths.
   *
   * @param host the object to which to write the values
   * @param values The values to write
   * @return a {@code boolean} array indicating which paths could successfully be set, and which could not.
   */
  public boolean[] writeValues(Object host, Object... values) {
    return writeValues(host, List.of(values));
  }

  public boolean[] writeValues(Object host, List<Object> values) {
    Check.notNull(values, Tag.VALUES).has(size(), eq(), paths.size());
    ObjectWriter writer = new ObjectWriter(suppressExceptions, segmentDeserializer);
    boolean[] result = new boolean[paths.size()];
    for (int i = 0; i < paths.size(); ++i) {
      if (writer.write(host, paths.get(i), values.get(i))) {
        result[i] = true;
      }
    }
    return result;
  }

  /**
   * Sets the value of the first path specified through the constructor. Convenient if you specified just one
   * path.
   *
   * @param host the object to write the value to
   * @param value The value to write
   * @return {@code true} if the value was successfully written
   */
  public boolean write(Object host, Object value) {
    return new ObjectWriter(suppressExceptions, segmentDeserializer).write(host, paths.getFirst(), value);
  }

}
