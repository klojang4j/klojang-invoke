package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;


public class PrimitiveArraySegmentReaderTest {

  public record Person(int id, String name, LocalDate birthday) {}

  @Test
  public void read00() {
    int[] person = new int[] {7, 13, 17};
    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("1"), 0);
    assertEquals(Result.of(13), reader.read(person, node));
  }

  @Test
  public void read01() {
    int[] person = new int[] {7, 13, 17};
    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("not an index"), 0);
    assertEquals(Result.notAvailable(), reader.read(person, node));
  }

  @Test
  public void read02() {
    int[] person = new int[] {7, 13, 17};
    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("10"), 0); // index out of bounds
    assertEquals(Result.notAvailable(), reader.read(person, node));
  }

//  @Test
//  public void read02() {
//    int[] person = new int[] {7, 13, 17};
//    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
//    SegmentNode node = new SegmentNode(Path.from("huh?"), 0);
//    assertEquals(Result.notAvailable(), reader.read(person, node));
//  }

//  @Test
//  public void read03() {
//    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
//    PathSegmentDeserializer deserializer = (path, index) -> Double.valueOf(path.segment(index));
//    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, deserializer);
//    SegmentNode node = new SegmentNode(Path.from("2"), 0);
//    assertEquals(Result.of("John Doe"), reader.read(person, node));
//  }
//
//  @Test
//  public void read04() {
//    Map<Double, Object> person = Map.of(1.0, 1, 2.0, "John Doe", 3.0, LocalDate.of(1990, 1, 1));
//    PathSegmentDeserializer deserializer = (_, _) -> Double.valueOf("not a number");
//    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, deserializer);
//    SegmentNode node = new SegmentNode(Path.from("2"), 0);
//    assertEquals(Result.notAvailable(), reader.read(person, node));
//  }

  @Test
  public void read50() {
    int[] person = new int[] {7, 13, 17};
    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
    assertEquals(Result.of(13), reader.read(person, Path.from("1"), 0));
  }

  @Test
  public void read51() {
    int[] person = new int[] {7, 13, 17};
    PrimitiveArraySegmentReader reader = new PrimitiveArraySegmentReader(true, null);
    assertEquals(Result.notAvailable(), reader.read(person, Path.from("not an index"), 0));
  }

}
