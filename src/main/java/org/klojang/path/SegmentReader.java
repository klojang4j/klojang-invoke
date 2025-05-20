package org.klojang.path;

import org.klojang.util.Path;

abstract sealed class SegmentReader<T> permits ArraySegmentReader, BeanSegmentReader,
    CollectionSegmentReader, MapSegmentReader, PrimitiveArraySegmentReader {

  final boolean se;
  final PathSegmentDeserializer kd;

  SegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    this.se = suppressExceptions;
    this.kd = keyDeserializer;
  }

  abstract Object read(T obj, Path path, int segment);

  Object deadEnd(PathWalkerException.Factory excFactory) {
    if (se) {
      return null;
    }
    throw excFactory.get();
  }

}
