package org.klojang.path;

import java.util.Map;

import static org.klojang.path.PathWalkerException.keyDeserializationFailed;
import static org.klojang.path.PathWalkerException.noSuchKey;

final class MapSegmentReader extends SegmentReader<Map<?, ?>> {

  MapSegmentReader(boolean suppressExceptions, KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Object read(Map<?, ?> map, Path path, int segment) {
    Object key;
    if (kd == null) {
      key = path.segment(segment);
    } else {
      try {
        key = kd.deserialize(path, segment);
      } catch (KeyDeserializationException e) {
        return deadEnd(keyDeserializationFailed(path, segment, e));
      }
    }
    Object val = map.get(key);
    if (val == null && !map.containsKey(key)) {
      return deadEnd(noSuchKey(path, segment, key));
    }
    return new ObjectReader(se, kd).read(val, path, ++segment);
  }

}
