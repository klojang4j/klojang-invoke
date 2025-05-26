package org.klojang.path;

import org.klojang.check.Check;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.*;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonProperties.size;

/**
 * <p>The {@code PathTreeWalker} is functionally equivalent to the {@link PathWalker} class. However, a
 * {@code PathTreeWalker} will turn the paths specified through the constructors into a tree of path segments.
 * This enables a {@code PathTreeWalker} to make a minimal amount of "movements" through the object from which
 * it retrieves the values. For example, say you have specified the following paths:
 *
 * <blockquote><pre>{@code
 * person.address.street
 * person.address.zipCode
 * parson.address.city
 * }</pre></blockquote>
 *
 * A {@code PathWalker} would perform 9 read operations as it retrieves the values for {@code street},
 * {@code zipCode}, and {@code city}. The {@code PathTreeWalker} on the other hand performs only 5 read
 * operations, because it retrieves the values for {@code person} and {@code address} just once. Thus the
 * {@code PathTreeWalker} is likely to be more performant when reading a relatively large number of paths
 * that, together, constitute a somewhat flat hierarchy.
 *
 * @author Ayco Holleman
 */
public final class PathTreeWalker {

  private final List<Path> paths;
  private final SegmentNode root;
  private final boolean suppressExceptions;
  private final PathSegmentDeserializer segmentDeserializer;

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   */
  public PathTreeWalker(Path... paths) {
    this(List.of(Check.notNull(paths, "paths").ok()));
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   */
  public PathTreeWalker(String... paths) {
    this(toPathList(paths), true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   */
  public PathTreeWalker(List<Path> paths) {
    this(paths, true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths the paths to read or write
   * @param suppressExceptions whether to enable exception suppression
   */
  public PathTreeWalker(List<Path> paths, boolean suppressExceptions) {
    Check.that(paths, "paths").isNot(empty()).is(deepNotNull());
    Check.that(new HashSet<>(paths)).has(size(), eq(), paths.size(), "paths must be unique");
    this.paths = paths;
    this.root = SegmentNode.buildTree(paths);
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
  public PathTreeWalker(
      List<Path> paths,
      boolean suppressExceptions,
      PathSegmentDeserializer segmentDeserializer) {
    Check.that(paths, "paths").isNot(empty()).is(deepNotNull());
    Check.that(new HashSet<>(paths)).has(size(), eq(), paths.size(), "paths must be unique");
    Check.notNull(segmentDeserializer, "segment deserializer");
    this.paths = paths;
    this.root = SegmentNode.buildTree(paths);
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
  public Map<Path, Result<Object>> readAll(Object host) throws DeadEndException {
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    var reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    root.children().values().forEach(child -> reader.read(results, host, child));
    for (Path path : paths) {
      if (!results.containsKey(path)) {
        results.put(path, Result.notAvailable());
      }
    }
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
  public Map<String, Result<Object>> readIntoMap(Object host) throws DeadEndException {
    Map<String, Result<Object>> results = HashMap.newHashMap(paths.size());
    readAll(host).forEach((path, result) -> results.put(path.toString(), result));
    return results;
  }

  /**
   * Returns the values of the paths specified through the constructor. The values are returned in the same
   * order as the paths.
   *
   * @param host the object to read the values from
   * @return the values of the paths specified through the constructor
   * @throws DeadEndException if exception suppression is disabled and the {@code PathWalker} fails to
   *     retrieve the values of one or more paths.
   */
  public List<Result<Object>> readIntoList(Object host) throws DeadEndException {
    Map<Path, Result<Object>> results = LinkedHashMap.newLinkedHashMap(paths.size());
    paths.forEach(path -> results.put(path, Result.notAvailable()));
    var reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    root.children().values().forEach(child -> reader.read(results, host, child));
    return List.copyOf(results.values());
  }

  private static List<Path> toPathList(String[] paths) {
    Check.notNull(paths, "paths");
    return Arrays.stream(paths).map(Path::from).toList();
  }

}
