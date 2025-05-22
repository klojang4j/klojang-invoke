package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class MapSegmentReaderTest {

  public record Person(int id, String name, LocalDate birthday) {}

  @Test
  public void read00() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("id"), 0);
    assertEquals(Result.of(1), reader.read(person, node));
  }

  @Test
  public void read01() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("name"), 0);
    assertEquals(Result.of("John Doe"), reader.read(person, node));
  }

  @Test
  public void read02() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("huh?"), 0);
    assertEquals(Result.notAvailable(), reader.read(person, node));
  }

  @Test
  public void read03() {
    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
    PathSegmentDeserializer deserializer = (path, index) -> Double.valueOf(path.segment(index));
    MapSegmentReader reader = new MapSegmentReader(true, deserializer);
    SegmentNode node = new SegmentNode(Path.from("2"), 0);
    assertEquals(Result.of("John Doe"), reader.read(person, node));
  }

  @Test
  public void read04() {
    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
    PathSegmentDeserializer deserializer = (_, _) -> Double.valueOf("not a number");
    MapSegmentReader reader = new MapSegmentReader(true, deserializer);
    SegmentNode node = new SegmentNode(Path.from("2"), 0);
    assertEquals(Result.notAvailable(), reader.read(person, node));
  }

  @Test
  public void read50() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    assertEquals(Result.of(1), reader.read(person, Path.from("id"), 0));
  }

  @Test
  public void read51() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("name"), 0);
    assertEquals(Result.of("John Doe"), reader.read(person, node));
  }

  @Test
  public void read52() {
    Map<String, Object> person = Map.of("id", 1, "name", "John Doe", "birthDay", LocalDate.of(1990, 1, 1));
    MapSegmentReader reader = new MapSegmentReader(true, null);
    assertEquals(Result.notAvailable(), reader.read(person, Path.from("huh?"), 0));
  }

  @Test
  public void read53() {
    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
    PathSegmentDeserializer deserializer = (path, index) -> Double.valueOf(path.segment(index));
    MapSegmentReader reader = new MapSegmentReader(true, deserializer);
    assertEquals(Result.of("John Doe"), reader.read(person, Path.from("2"), 0));
  }


  @Test
  public void read54() {
    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
    PathSegmentDeserializer deserializer = (_, _) -> Double.valueOf("not a number");
    MapSegmentReader reader = new MapSegmentReader(true, deserializer);
    assertEquals(Result.notAvailable(), reader.read(person, Path.from("2"), 0));
  }

}
