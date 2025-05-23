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
 * <p>The {@code PathTreeWalker} is functionally the same as the {@link PathWalker} class. However, a
 * {@code PathTreeWalker} will turn the paths you specify through the constructors into a tree of path
 * segments. This guarantees that it makes a minimal amount of "movements" through the object from which to
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
   * @param paths the paths to read or write
   */
  public PathTreeWalker(Path... paths) {
    this(List.of(Check.notNull(paths, PATHS).ok()));
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
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
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
    Check.that(paths, PATHS).isNot(empty()).is(deepNotNull());
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
  public Map<Path, Result<Object>> readValues(Object host) throws DeadEndException {
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    if (suppressExceptions) {
      paths.forEach(path -> results.put(path, Result.notAvailable()));
    }
    ObjectReader reader = new ObjectReader(suppressExceptions, segmentDeserializer);
    root.children().values().forEach(child -> reader.read(results, host, child));
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
    Map<String, Result<Object>> results = HashMap.newHashMap(paths.size());
    readValues(host).forEach((path, result) -> results.put(path.toString(), result));
    return results;
  }

  private static List<Path> toPathList(String[] paths) {
    Check.notNull(paths, PATHS);
    return Arrays.stream(paths).map(Path::from).toList();
  }

}
