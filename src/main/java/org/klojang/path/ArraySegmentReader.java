package org.klojang.path;

import java.util.OptionalInt;

import static org.klojang.path.PathWalkerException.indexExpected;
import static org.klojang.path.PathWalkerException.indexOutOfBounds;
import static org.klojang.util.NumberMethods.toInt;

final class ArraySegmentReader extends
    SegmentReader<Object[]> {

  ArraySegmentReader(boolean suppressExceptions, KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Object read(Object[] array, Path path, int segment) {
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    if (idx < array.length) {
      return new ObjectReader(se, kd).read(array[idx], path, ++segment);
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}
