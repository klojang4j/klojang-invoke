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
 *   <li>When processing large batches of differently structured objects
 *   <li>When it does not really matter whether a deeply nested value is {@code null} or just not present at
 *   all
 *   <li>To keep your code concise and clean when reading a deeply nested value.
 * </ul>
 *
 * <h2>Exception Suppression</h2>
 * <p>By default, a {@code PathWalker} will not throw an exception if it cannot read or write a value
 * &#8212; that is, if it cannot walk a path all the way down to the last path segment. That would defy the
 * purposes listed above. Instead, it just returns {@link Result#notAvailable()} when failing to read a
 * value, and {@code false} when failing to write a value. However, the {@code PathWalker} class contains
 * {@linkplain #PathWalker(List, boolean) constructors} that allow you to disable exception suppression.
 * With exception suppression disabled a {@code PathWalker} will throw a {@link DeadEndException} when
 * failing to read/write a value, which may be useful when debugging. Note that even with exception
 * suppression enabled, runtime exceptions may still occur. The only exceptions that are actively suppressed
 * are those anticipated by the {@code PathWalker} as it moves through sparsely populated or differently
 * structured objects.
 *
 * <h2>Path Segment Deserialization</h2>
 * <p>A {@code PathWalker} has no problem reading from, or writing to {@code Map<String, Object} objects.
 * However, if you want a {@code PathWalker} to be able to read from, or write to maps with a non-String
 * key type, you must instruct the {@code PathWalker} how to deserialize the path segment representing the
 * key into an object of the appropriate type. This is done using a {@link PathSegmentDeserializer}. The
 * {@code PathWalker} class has a constructor that enables you to specify a {@code PathSegmentDeserializer}.
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
   * @param paths the paths to read or write
   */
  public PathWalker(Path... paths) {
    this(List.of(Check.notNull(paths, PATHS).ok()));
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   */
  public PathWalker(String... paths) {
    this(toPathList(paths), true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   */
  public PathWalker(List<Path> paths) {
    this(paths, true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   * @param suppressExceptions whether to enable exception suppression
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
   * @param paths the paths to read or write
   * @param suppressExceptions whether to enable exception suppression
   * @param segmentDeserializer a function that deserializes path segments into non-String map keys
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
   * Returns the values of the paths specified through the constructor. The returned map maps the paths to
   * their values.
   *
   * @param host the object to read the values from
   * @return the values of all paths specified through the constructor
   * @throws DeadEndException if exception suppression is disabled and the {@code PathWalker} fails to
   *     retrieve the values of one or more paths.
   */
  public Map<Path, Result<Object>> readValues(Object host) throws DeadEndException {
    ObjectReader reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    paths.forEach(path -> results.put(path, reader.read(host, path, 0)));
    return results;
  }

  /**
   * Returns the values of the paths specified through the constructor. The returned map maps the path strings
   * to their values.
   *
   * @param host the object to read the values from
   * @return the values of all paths specified through the constructor
   * @throws DeadEndException if exception suppression is disabled and the {@code PathWalker} fails to
   *     retrieve the values of one or more paths.
   */
  public Map<String, Result<Object>> readAll(Object host) throws DeadEndException {
    ObjectReader reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    Map<String, Result<Object>> results = HashMap.newHashMap(paths.size());
    paths.forEach(path -> results.put(path.toString(), reader.read(host, path, 0)));
    return results;
  }

  /**
   * Reads the value of the first path specified through the constructor. Convenient if you specified just one
   * path.
   *
   * @param <T> The type of the value being returned
   * @param host the object from which to read the value
   * @return the value of the first path specified through the constructor
   * @throws DeadEndException if exception suppression is disabled and the {@code PathWalker} fails to
   *     retrieve the values of one or more paths.
   */
  public <T> Result<T> read(Object host) {
    return cast(new ObjectReader(suppressExceptions, segmentDeserializer).read(host, paths.getFirst(), 0));
  }

  /**
   * Sets the values of the paths specified through the constructor. The provided array of values must have
   * the same length as the number of paths.
   *
   * @param host the object to which to write the values
   * @param values the values to write
   * @return a {@code boolean} array indicating which paths could successfully be set, and which could not.
   */
  public boolean[] writeValues(Object host, Object... values) {
    return writeValues(host, List.of(values));
  }

  /**
   * Sets the values of the paths specified through the constructor. The provided {@code List} of values must
   * have the same size as the number of paths.
   *
   * @param host the object to which to write the values
   * @param values the values to write
   * @return a {@code boolean} array indicating which paths could successfully be set, and which could not.
   */
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
   * @param value the value to write
   * @return {@code true} if the value was successfully written
   */
  public boolean write(Object host, Object value) {
    return new ObjectWriter(suppressExceptions, segmentDeserializer).write(host, paths.getFirst(), value);
  }


  private static List<Path> toPathList(String[] paths) {
    Check.notNull(paths, PATHS);
    return Arrays.stream(paths).map(Path::from).toList();
  }

}
