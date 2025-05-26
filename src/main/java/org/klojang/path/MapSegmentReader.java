package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.Map;

import static org.klojang.path.DeadEndException.noSuchKey;
import static org.klojang.path.DeadEndException.deserializationFailed;

final class MapSegmentReader extends SegmentReader<Map<?, ?>> {

  MapSegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Result<Object> read(Map<?, ?> map, SegmentNode node) {
    Object key;
    if (keyDeserializer == null) {
      key = node.segment();
    } else {
      try {
        key = keyDeserializer.deserialize(node.path(), node.segmentIndex());
      } catch (Exception e) {
        return deadEnd(deserializationFailed(node.path(), node.segmentIndex(), e));
      }
    }
    Object val = map.get(key);
    if (val == null && !map.containsKey(key)) {
      return deadEnd(noSuchKey(node.path(), node.segmentIndex(), key));
    }
    return Result.of(val);
  }

  @Override
  Result<Object> read(Map<?, ?> map, Path path, int segment) {
    Object key;
    if (keyDeserializer == null) {
      key = path.segment(segment);
    } else {
      try {
        key = keyDeserializer.deserialize(path, segment);
      } catch (Exception e) {
        return deadEnd(deserializationFailed(path, segment, e));
      }
    }
    Object val = map.get(key);
    if (val == null && !map.containsKey(key)) {
      return deadEnd(noSuchKey(path, segment, key));
    }
    return new ObjectReader(suppressExceptions, keyDeserializer).read(val, path, ++segment);
  }

}
