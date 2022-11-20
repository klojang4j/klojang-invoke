package org.klojang.path;

import java.util.Collection;
import java.util.Map;

import static org.klojang.util.ClassMethods.isPrimitiveArray;
import static org.klojang.path.PathWalkerException.nullValue;

final class ObjectReader {

  private final boolean se;
  private final KeyDeserializer kd;

  ObjectReader(boolean suppressExceptions, KeyDeserializer keyDeserializer) {
    this.se = suppressExceptions;
    this.kd = keyDeserializer;
  }

  Object read(Object obj, Path path, int segment) {
    if (segment == path.size()) {
      return obj;
    } else if (obj == null) {
      return deadEnd(nullValue(path, segment));
    } else if (obj instanceof Collection c) {
      return new CollectionSegmentReader(se, kd).read(c, path, segment);
    } else if (obj instanceof Object[] o) {
      return new ArraySegmentReader(se, kd).read(o, path, segment);
    } else if (obj instanceof Map m) {
      return new MapSegmentReader(se, kd).read(m, path, segment);
    } else if (isPrimitiveArray(obj)) {
      return new PrimitiveArraySegmentReader(se, kd).read(obj, path, segment);
    }
    return new BeanSegmentReader(se, kd).read(obj, path, segment);
  }

  Object deadEnd(PathWalkerException.Factory excFactory) {
    if (se) {
      return null;
    }
    throw excFactory.get();
  }

}
