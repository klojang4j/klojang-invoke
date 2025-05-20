package org.klojang.path;

import org.junit.Test;
import org.klojang.util.Path;

import java.io.File;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeadEndExceptionTest {

  @Test
  public void terminalValue() {
    DeadEndException.Factory excFactory = DeadEndException.terminalValue(
        Path.from("foo.bar.bozo"), 1, "teapot");
    assertTrue(excFactory.get().getMessage().contains("Terminal value encountered"));
  }

  @Test
  public void typeMismatch00() {
    DeadEndException.Factory excFactory = DeadEndException.typeMismatch(Path.from("foo.bar.bozo"),
        1,
        "my message to you");
    //System.out.println(excFactory.get().getMessage());
    assertEquals("Path foo.bar.bozo, segment 2: my message to you", excFactory.get().getMessage());
  }

  @Test
  public void typeMismatch01() {
    DeadEndException.Factory excFactory = DeadEndException.typeMismatch(
        Path.from("foo.bar.bozo"), 1, File.class, FileChannel.class);
    assertEquals(
        "Path foo.bar.bozo, segment 2: cannot assign File to FileChannel",
        excFactory.get().getMessage());
  }

  @Test
  public void segmentDeserializationFailed00() {
    DeadEndException.Factory excFactory = DeadEndException.segmentDeserializationFailed(
        Path.from("foo.bar.bozo"), 0, new Exception("no can do"));
    assertEquals(
        "Invalid path: \"foo.bar.bozo\" (segment 1). Failed to deserialize \"foo\" into map key. java.lang.Exception: no can do",
        excFactory.get().getMessage());
  }

  @Test
  public void segmentDeserializationFailed01() {
    DeadEndException.Factory excFactory = DeadEndException.segmentDeserializationFailed(
        Path.from("foo.bar.bozo"), 0, new Exception());
    assertEquals(
        "Invalid path: \"foo.bar.bozo\" (segment 1). Failed to deserialize \"foo\" into map key",
        excFactory.get().getMessage());
  }

  @Test
  public void unexpectedError00() {
    DeadEndException.Factory excFactory = DeadEndException.unexpectedError(Path.from("foo.bar.bozo"),
        0,
        new Exception("Sorry"));
    System.out.println(excFactory.get().getMessage());
    assertEquals(
        "Path foo.bar.bozo, segment 1: Unexpected error. java.lang.Exception: Sorry",
        excFactory.get().getMessage());
  }

}