package org.klojang.path;

import java.util.OptionalInt;

import static org.klojang.path.PathWalkerException.indexExpected;
import static org.klojang.path.PathWalkerException.indexOutOfBounds;
import static org.klojang.util.InvokeMethods.getArrayLength;
import static org.klojang.util.InvokeMethods.setArrayElement;
import static org.klojang.util.NumberMethods.toInt;

final class PrimitiveArraySegmentWriter extends
    SegmentWriter<Object> {

  PrimitiveArraySegmentWriter(boolean suppressExceptions,
      KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  boolean write(Object array, Path path, Object value) {
    int segment = path.size() - 1;
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    int len = getArrayLength(array);
    if (idx >= 0 && idx < len) {
      setArrayElement(array, idx, value);
      return true;
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}
