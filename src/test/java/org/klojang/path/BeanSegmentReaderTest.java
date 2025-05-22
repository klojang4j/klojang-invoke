package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;


public class BeanSegmentReaderTest {

  public record Person(int id, String name, LocalDate birthday) {}

  @Test
  public void read00() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("id"), 0);
    assertEquals(Result.of(1), reader.read(person, node));
  }

  @Test
  public void read01() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("name"), 0);
    assertEquals(Result.of("John Doe"), reader.read(person, node));
  }

  @Test
  public void read02() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("huh?"), 0);
    assertEquals(Result.notAvailable(), reader.read(person, node));
  }

  @Test
  public void read50() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    assertEquals(Result.of(1), reader.read(person, Path.from("id"), 0));
  }

  @Test
  public void read51() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("name"), 0);
    assertEquals(Result.of("John Doe"), reader.read(person, node));
  }

  @Test
  public void read52() {
    Person person = new Person(1, "John Doe", LocalDate.of(1990, 1, 1));
    BeanSegmentReader reader = new BeanSegmentReader(true, null);
    assertEquals(Result.notAvailable(), reader.read(person, Path.from("huh?"), 0));
  }

}
