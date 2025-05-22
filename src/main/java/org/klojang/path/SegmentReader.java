package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

abstract sealed class SegmentReader<T> permits ArraySegmentReader, BeanSegmentReader,
    CollectionSegmentReader, MapSegmentReader, PrimitiveArraySegmentReader {

  final boolean se;
  final PathSegmentDeserializer kd;

  SegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    this.se = suppressExceptions;
    this.kd = keyDeserializer;
  }

  abstract Result<Object> read(T obj, SegmentNode node);

  abstract Result<Object> read(T obj, Path path, int segment);

  Result<Object> deadEnd(DeadEndException.Factory excFactory) {
    if (se) {
      return Result.notAvailable();
    }
    throw excFactory.get();
  }

}
