package org.klojang.path;

import org.klojang.check.Check;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.deepNotNull;
import static org.klojang.check.CommonChecks.empty;

/**
 * <p>The {@code PathTreeWalker} is functionally the same as the {@link PathWalker} class. However, it will
 * turn the paths you specify through one of the constructors into a tree of path segments. This guarantees
 * that the {@code PathTreeWalker} makes a minimal amount of "movements" through the object from which to
 * retrieve the values. For example, say you have specified the following paths:
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

  private static final String PATHS = "paths";

  private final List<Path> paths;
  private final SegmentNode root;
  private final boolean suppressExceptions;
  private final PathSegmentDeserializer segmentDeserializer;

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths One or more paths representing possibly deeply-nested properties
   */
  public PathTreeWalker(Path... paths) {
    this(List.of(Check.notNull(paths, PATHS).ok()));
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The paths to walk through the provided host objects
   */
  public PathTreeWalker(String... paths) {
    this(toPathList(paths), true);
  }

  /**
   * Creates a {@code PathWalker} for the specified paths.
   *
   * @param paths The paths to walk through the provided host objects
   */
  public PathTreeWalker(List<Path> paths) {
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
  public PathTreeWalker(List<Path> paths, boolean suppressExceptions) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    this.paths = paths;
    this.root = SegmentNode.buildTree(paths);
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
  public PathTreeWalker(
      List<Path> paths,
      boolean suppressExceptions,
      PathSegmentDeserializer segmentDeserializer) {
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
    Check.notNull(segmentDeserializer, "segment deserializer");
    this.paths = paths;
    this.root = SegmentNode.buildTree(paths);
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
  public Map<Path, Result<Object>> read(Object host) throws DeadEndException {
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    paths.forEach(path -> results.put(path, Result.notAvailable()));
    ObjectReader reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    reader.read(results, host, root);
    return results;
  }

  private static List<Path> toPathList(String[] paths) {
    Check.notNull(paths, PATHS);
    return Arrays.stream(paths).map(Path::from).toList();
  }

}
