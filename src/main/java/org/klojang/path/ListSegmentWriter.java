package org.klojang.path;

import java.util.List;
import java.util.OptionalInt;

import static org.klojang.path.PathWalkerException.*;
import static org.klojang.convert.NumberMethods.toInt;

@SuppressWarnings({"rawtypes", "unchecked"})
final class ListSegmentWriter extends SegmentWriter<List> {

  ListSegmentWriter(boolean suppressExceptions, KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  boolean write(List list, Path path, Object value) {
    int segment = path.size() - 1;
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    if (idx < list.size()) {
      try {
        list.set(opt.getAsInt(), value);
      } catch (UnsupportedOperationException e) {
        return deadEnd(notModifiable(path.parent(), segment, List.class));
      }
      return true;
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}
