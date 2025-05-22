package org.klojang.path;

import org.klojang.util.Path;

/**
 * Specifies how to deserialize a path segment. A {@code PathSegmentDeserializer} can optionally be specified
 * for {@link PathWalker} and {@link PathTreeWalker} instances. It is needed when reading or writing to maps
 * that have a non-String key type.
 */
@FunctionalInterface
public interface PathSegmentDeserializer {

  /**
   * Converts the path segment at the specified index to a map key.
   *
   * @param path the path containing the segment
   * @param segmentIndex the index of the segment
   * @return the map key
   */
  Object deserialize(Path path, int segmentIndex) throws Exception;

}
