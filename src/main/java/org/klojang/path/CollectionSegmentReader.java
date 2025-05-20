package org.klojang.path;

import org.klojang.util.Path;

import java.util.*;

import static org.klojang.path.PathWalkerException.indexExpected;
import static org.klojang.path.PathWalkerException.indexOutOfBounds;
import static org.klojang.convert.NumberMethods.toInt;

@SuppressWarnings("rawtypes")
final class CollectionSegmentReader extends SegmentReader<Collection> {

  CollectionSegmentReader(boolean suppressExceptions,
      PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Object read(Collection collection, Path path, int segment) {
    OptionalInt opt = toInt(path.segment(segment));
    if (opt.isEmpty()) {
      return deadEnd(indexExpected(path, segment));
    }
    int idx = opt.getAsInt();
    if (idx < collection.size()) {
      Object elem;
      if (collection instanceof List list) {
        elem = list.get(idx);
      } else {
        Iterator iter = collection.iterator();
        for (; idx != 0 && iter.hasNext(); --idx, iter.next());
        elem = iter.next();
      }
      return new ObjectReader(se, kd).read(elem, path, ++segment);
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}
