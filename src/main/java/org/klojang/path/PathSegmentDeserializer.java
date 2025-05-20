package org.klojang.path;

import org.klojang.util.Path;

import java.util.List;

/**
 * Specifies how to deserialize a path segment. A {@code PathSegmentDeserializer} can optionally be passed to
 * the {@link PathWalker#PathWalker(List, boolean, PathSegmentDeserializer) constructor} of the
 * {@link PathWalker} class. It is needed when reading or writing to maps that have a non-String key type.
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
