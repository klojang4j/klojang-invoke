package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.OptionalInt;

import static org.klojang.convert.NumberMethods.toInt;
import static org.klojang.path.DeadEndException.indexExpected;
import static org.klojang.path.DeadEndException.indexOutOfBounds;

final class ArraySegmentReader extends SegmentReader<Object[]> {

  ArraySegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Result<Object> read(Object[] array, SegmentNode node) {
    OptionalInt opt = toInt(node.segment());
    if (opt.isPresent()) {
      int idx = opt.getAsInt();
      if (idx < array.length) {
        return Result.of(array[idx]);
      }
      return deadEnd(indexOutOfBounds(node.getArbitraryFullPath(), node.segmentIndex()));
    }
    return deadEnd(indexExpected(node.getArbitraryFullPath(), node.segmentIndex()));
  }

  @Override
  Result<Object> read(Object[] array, Path path, int segment) {
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isPresent()) {
      int idx = opt.getAsInt();
      if (idx < array.length) {
        return new ObjectReader(suppressExceptions, keyDeserializer).read(array[idx], path, ++segment);
      }
      return deadEnd(indexOutOfBounds(path, segment));
    }
    return deadEnd(indexExpected(path, segment));
  }

}
