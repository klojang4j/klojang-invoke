package org.klojang.path;

import org.junit.Test;
import org.klojang.check.extra.Result;
import org.klojang.util.Path;

import static org.junit.Assert.*;


public class ArraySegmentReaderTest {

  @Test
  public void read00() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("0"), 0);
    assertEquals(Result.of("foo"), reader.read(strings, node));
  }

  @Test
  public void read01() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("1"), 0);
    assertEquals(Result.of("bar"), reader.read(strings, node));
  }

  @Test
  public void read02() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    SegmentNode node = new SegmentNode(Path.from("2"), 0);
    assertEquals(Result.notAvailable(), reader.read(strings, node));
  }

  @Test
  public void read50() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    assertEquals(Result.of("foo"), reader.read(strings, Path.from("0"), 0));
  }

  @Test
  public void read51() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    assertEquals(Result.of("bar"), reader.read(strings, Path.from("1"), 0));
  }

  @Test
  public void read52() {
    String[] strings = {"foo", "bar"};
    ArraySegmentReader reader = new ArraySegmentReader(true, null);
    assertEquals(Result.notAvailable(), reader.read(strings,  Path.from("2"), 0));
  }

}
