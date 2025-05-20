package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.Map;

import static org.klojang.path.DeadEndException.segmentDeserializationFailed;
import static org.klojang.path.DeadEndException.noSuchKey;

final class MapSegmentReader extends SegmentReader<Map<?, ?>> {

  MapSegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Result<Object> read(Map<?, ?> map, Path path, int segment) {
    Object key;
    if (kd == null) {
      key = path.segment(segment);
    } else {
      try {
        key = kd.deserialize(path, segment);
      } catch (Exception e) {
        return deadEnd(segmentDeserializationFailed(path, segment, e));
      }
    }
    Object val = map.get(key);
    if (val == null && !map.containsKey(key)) {
      return deadEnd(noSuchKey(path, segment, key));
    }
    return new ObjectReader(se, kd).read(val, path, ++segment);
  }

}
