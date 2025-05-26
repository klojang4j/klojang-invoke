package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.JSONObject;
import org.klojang.util.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ObjectReaderTest {

  @Test
  public void testEmptyPath00() {
    var reader = new ObjectReader(true, null);
    var path = Path.empty();
    var node = new SegmentNode(path, 0);
    var results = createResultsMap(List.of(path), true);
    reader.read(results, Map.of("foo", "bar"), node);
    assertEquals(1, results.size());
    assertEquals(Map.of("foo", "bar"), results.values().iterator().next().get());
  }

  @Test
  public void test00() {
    var reader = new ObjectReader(true, null);
    var path = Path.from("person.address.city");
    var node = SegmentNode.buildTree(List.of(path));
    Map<String, Object> map = JSONObject.empty().set("person.address.city", "Leiden").build();
    var results = createResultsMap(List.of(path), true);
    reader.read(results, map, node.children().values().iterator().next());
    assertEquals(1, results.size());
    assertEquals("Leiden", results.values().iterator().next().get());
  }

  @Test
  public void test01() {
    var reader = new ObjectReader(true, null);
    var path = Path.from("person.address.city");
    var node = SegmentNode.buildTree(List.of(path));
    Map<String, Object> map = JSONObject.empty().set("person.address", null).build();
    var results = createResultsMap(List.of(path), true);
    reader.read(results, map, node.children().values().iterator().next());
    assertEquals(1, results.size());
    assertEquals(Result.notAvailable(), results.values().iterator().next());
  }

  @Test
  public void test02() {
    var reader = new ObjectReader(false, null);
    var path = Path.from("person.address.city");
    var node = SegmentNode.buildTree(List.of(path));
    Map<String, Object> map = JSONObject.empty().set("person.address", null).build();
    var results = createResultsMap(List.of(path), true);
    try {
      reader.read(results, map, node.children().values().iterator().next());
    } catch (DeadEndException e) {
      assertEquals(DeadEndException.nullValue(path, 1).get().getMessage(), e.getMessage());
      return;
    }
    fail();
  }

  @Test
  public void testEmptyPath100() {
    ObjectReader reader = new ObjectReader(true, null);
    Result<Object> result = reader.read(Map.of("foo", "bar"), Path.empty(), 0);
    assertEquals(Map.of("foo", "bar"), result.get());
  }


  @Test
  public void test102() {
    var reader = new ObjectReader(false, null);
    var path = Path.from("person.address.city");
    Map<String, Object> map = JSONObject.empty().set("person.address", null).build();
    try {
      reader.read(map, path, 0);
    } catch (DeadEndException e) {
      assertEquals(DeadEndException.nullValue(path, 1).get().getMessage(), e.getMessage());
      return;
    }
    fail();
  }


  static Map<Path, Result<Object>> createResultsMap(List<Path> paths, boolean suppressExceptions) {
    Map<Path, Result<Object>> results = HashMap.newHashMap(paths.size());
    if (suppressExceptions) {
      paths.forEach(path -> results.put(path, Result.notAvailable()));
    }
    return results;
  }

}
