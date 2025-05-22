package org.klojang.path;

import org.klojang.util.Path;

import java.util.Map;

import static org.klojang.path.DeadEndException.notModifiable;
import static org.klojang.path.DeadEndException.deserializationFailed;

@SuppressWarnings({"rawtypes", "unchecked"})
final class MapSegmentWriter extends SegmentWriter<Map> {

  MapSegmentWriter(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  boolean write(Map map, Path path, Object value) {
    int segment = path.size() - 1;
    Object key;
    if (kd == null) {
      key = path.segment(segment);
    } else {
      try {
        key = kd.deserialize(path, segment);
      } catch (Exception e) {
        return deadEnd(deserializationFailed(path, segment, e));
      }
    }
    try {
      map.put(key, value);
    } catch (UnsupportedOperationException e) {
      return deadEnd(notModifiable(path, segment, Map.class));
    }
    return true;
  }

}
