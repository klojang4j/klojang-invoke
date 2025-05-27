package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.JSONObject;
import org.klojang.util.Path;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PathTreeWalkerTest {

  @Test
  public void readAll00() {
    Map<String, Object> map = JSONObject.empty().
        in("person.address")
        .set("street", "Main St.")
        .set("zip", "CA12345")
        .set("city", "San Francisco")
        .backTo("person")
        .set("hobbies", List.of("football", "tennis"))
        .set("insurance", null)
        .build();
    PathTreeWalker treeWalker = new PathTreeWalker("person.address.street",
        "person.address.zip",
        "person.hobbies.0",
        "person.hobbies.1",
        "foo",
        "person.insurance");
    Map<Path, Result<Object>> results = treeWalker.readAll(map);
    assertEquals("Main St.", results.get(Path.from("person.address.street")).get());
    assertEquals("CA12345", results.get(Path.from("person.address.zip")).get());
    assertEquals("football", results.get(Path.from("person.hobbies.0")).get());
    assertEquals("tennis", results.get(Path.from("person.hobbies.1")).get());
    assertEquals(Result.notAvailable(), results.get(Path.from("foo")));
    assertNull(results.get(Path.from("person.insurance")).get());
  }

  @Test
  public void readIntoMap00() {
    Map<String, Object> map = JSONObject.empty().
        in("person.address")
        .set("street", "Main St.")
        .set("zip", "CA12345")
        .set("city", "San Francisco")
        .backTo("person")
        .set("hobbies", List.of("football", "tennis"))
        .set("insurance", null)
        .build();
    PathTreeWalker treeWalker = new PathTreeWalker("person.address.street",
        "person.address.zip",
        "person.hobbies.0",
        "person.hobbies.1",
        "foo",
        "person.insurance");
    Map<String, Result<Object>> results = treeWalker.readIntoMap(map);
    assertEquals("Main St.", results.get("person.address.street").get());
    assertEquals("CA12345", results.get("person.address.zip").get());
    assertEquals("football", results.get("person.hobbies.0").get());
    assertEquals("tennis", results.get("person.hobbies.1").get());
    assertEquals(Result.notAvailable(), results.get("foo"));
    assertNull(results.get("person.insurance").get());
  }

  @Test
  public void readIntoList00() {
    Map<String, Object> map = JSONObject.empty().
        in("person.address")
        .set("street", "Main St.")
        .set("zip", "CA12345")
        .set("city", "San Francisco")
        .backTo("person")
        .set("hobbies", List.of("football", "tennis"))
        .set("insurance", null)
        .build();
    PathTreeWalker treeWalker = new PathTreeWalker("person.address.street",
        "person.address.zip",
        "person.hobbies.0",
        "person.hobbies.1",
        "foo",
        "person.insurance");
    List<Result<Object>> results = treeWalker.readIntoList(map);
    assertEquals("Main St.", results.get(0).get());
    assertEquals("CA12345", results.get(1).get());
    assertEquals("football", results.get(2).get());
    assertEquals("tennis", results.get(3).get());
    assertEquals(Result.notAvailable(), results.get(4));
    assertNull(results.get(5).get());
  }

}
