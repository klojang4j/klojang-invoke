package org.klojang.path;

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalInt;

import static org.klojang.path.PathWalkerException.indexExpected;
import static org.klojang.path.PathWalkerException.indexOutOfBounds;
import static org.klojang.convert.NumberMethods.toInt;

@SuppressWarnings("rawtypes")
final class CollectionSegmentReader extends SegmentReader<Collection> {

  CollectionSegmentReader(boolean suppressExceptions,
      KeyDeserializer keyDeserializer) {
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
      Iterator iter = collection.iterator();
      for (; idx != 0 && iter.hasNext(); --idx, iter.next())
        ;
      if (iter.hasNext()) {
        return new ObjectReader(se, kd).read(iter.next(), path, ++segment);
      }
    }
    return deadEnd(indexOutOfBounds(path, segment));
  }

}
