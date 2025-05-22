package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.OptionalInt;

import static org.klojang.convert.NumberMethods.toInt;
import static org.klojang.path.DeadEndException.indexExpected;
import static org.klojang.path.DeadEndException.indexOutOfBounds;
import static org.klojang.util.InvokeMethods.getArrayElement;
import static org.klojang.util.InvokeMethods.getArrayLength;

final class PrimitiveArraySegmentReader extends SegmentReader<Object> {

  PrimitiveArraySegmentReader(boolean suppressExceptions,
      PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  Result<Object> read(Object array, SegmentNode node) {
    OptionalInt opt = toInt(node.segment());
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(node.getArbitraryFullPath(), node.segmentIndex()));
    }
    int idx = opt.getAsInt();
    int len = getArrayLength(array);
    if (idx >= 0 && idx < len) {
      Object val = getArrayElement(array, idx);
      return Result.of(val);
    }
    return deadEnd(indexOutOfBounds(node.getArbitraryFullPath(), node.segmentIndex()));
  }

  @Override
  Result<Object> read(Object array, Path path, int segment) {
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    int len = getArrayLength(array);
    if (idx >= 0 && idx < len) {
      Object val = getArrayElement(array, idx);
      return new ObjectReader(suppressExceptions, keyDeserializer).read(val, path, ++segment);
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}


