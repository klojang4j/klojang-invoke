package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ObjectReaderTest {

  @Test
  public void testEmptyPath00() {
    var reader = new ObjectReader(true, null);
    var path = Path.empty();
    var node = new SegmentNode(path, 0);
    var results = createResultsMap(List.of(path), true);
    reader.read(results, Map.of("foo", "bar"), node);
  }

  @Test
  public void testEmptyPath100() {
    ObjectReader reader = new ObjectReader(true, null);
    Result<Object> result = reader.read(Map.of("foo", "bar"), Path.empty(), 0);
    assertEquals(Map.of("foo", "bar"), result.get());
  }

  static Map<Path, Result<Object>> createResultsMap(List<Path> paths, boolean suppressExceptions) {
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    if (suppressExceptions) {
      paths.forEach(path -> results.put(path, Result.notAvailable()));
    }
    return results;
  }

}
