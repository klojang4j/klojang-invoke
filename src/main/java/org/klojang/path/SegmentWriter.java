package org.klojang.path;

import org.klojang.util.Path;

abstract sealed class SegmentWriter<T> permits
    ArraySegmentWriter, BeanSegmentWriter,
    ListSegmentWriter, MapSegmentWriter, PrimitiveArraySegmentWriter {

  final boolean se;
  final PathSegmentDeserializer kd;

  SegmentWriter(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    this.se = suppressExceptions;
    this.kd = keyDeserializer;
  }

  abstract boolean write(T obj, Path path, Object value);

  boolean deadEnd(PathWalkerException.Factory excFactory) {
    if (se) {
      return false;
    }
    throw excFactory.get();
  }

}
