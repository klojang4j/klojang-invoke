package org.klojang.path;

import java.util.OptionalInt;

import static org.klojang.path.PathWalkerException.indexExpected;
import static org.klojang.path.PathWalkerException.indexOutOfBounds;
import static org.klojang.util.InvokeMethods.getArrayElement;
import static org.klojang.util.InvokeMethods.getArrayLength;
import static org.klojang.convert.NumberMethods.toInt;

final class PrimitiveArraySegmentReader extends
    SegmentReader<Object> {

  PrimitiveArraySegmentReader(boolean suppressExceptions,
      KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Object read(Object array, Path path, int segment) {
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    int len = getArrayLength(array);
    if (idx >= 0 && idx < len) {
      Object val = getArrayElement(array, idx);
      return new ObjectReader(se, kd).read(val, path, ++segment);
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}


