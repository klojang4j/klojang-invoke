package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.Collection;
import java.util.Map;

import static org.klojang.path.DeadEndException.nullValue;
import static org.klojang.util.ClassMethods.isPrimitiveArray;

final class ObjectReader {

  private final boolean suppressExceptions;
  private final PathSegmentDeserializer keyDeserializer;

  ObjectReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    this.suppressExceptions = suppressExceptions;
    this.keyDeserializer = keyDeserializer;
  }

  void read(Map<Path, Result<Object>> results, Object obj, SegmentNode node) {
    if (node.segmentIndex() == node.path().size()) {
      results.put(node.path(), Result.of(obj));
    } else if (obj == null) {
      if (!suppressExceptions) {
        throw nullValue(node.path(), node.segmentIndex()).get();
      } // Otherwise: we have already initialized all values to Result.notAvailable()
    } else {
      Result<Object> next;
      if (obj instanceof Collection<?> x) {
        next = new CollectionSegmentReader(suppressExceptions, keyDeserializer).read(x, node);
      } else if (obj instanceof Object[] x) {
        next = new ArraySegmentReader(suppressExceptions, keyDeserializer).read(x, node);
      } else if (obj instanceof Map<?, ?> x) {
        next = new MapSegmentReader(suppressExceptions, keyDeserializer).read(x, node);
      } else if (isPrimitiveArray(obj)) {
        next = new PrimitiveArraySegmentReader(suppressExceptions, keyDeserializer).read(obj, node);
      } else {
        next = new BeanSegmentReader(suppressExceptions, keyDeserializer).read(obj, node);
      }
      if (next.isAvailable()) {
        if (node.isLeaf()) {
          results.put(node.path(), next);
        } else {
          node.children().values().forEach(child -> read(results, next.get(), child));
        }
      }
    }
  }

  Result<Object> read(Object obj, Path path, int segmentIndex) {
    if (segmentIndex == path.size()) {
      return Result.of(obj);
    } else if (obj == null) {
      return deadEnd(nullValue(path, segmentIndex));
    } else if (obj instanceof Collection<?> x) {
      return new CollectionSegmentReader(suppressExceptions, keyDeserializer).read(x, path, segmentIndex);
    } else if (obj instanceof Object[] x) {
      return new ArraySegmentReader(suppressExceptions, keyDeserializer).read(x, path, segmentIndex);
    } else if (obj instanceof Map<?, ?> x) {
      return new MapSegmentReader(suppressExceptions, keyDeserializer).read(x, path, segmentIndex);
    } else if (isPrimitiveArray(obj)) {
      return new PrimitiveArraySegmentReader(suppressExceptions, keyDeserializer)
          .read(obj, path, segmentIndex);
    }
    return new BeanSegmentReader(suppressExceptions, keyDeserializer).read(obj, path, segmentIndex);
  }

  private Result<Object> deadEnd(DeadEndException.Factory excFactory) {
    if (suppressExceptions) {
      return Result.notAvailable();
    }
    throw excFactory.get();
  }

}
