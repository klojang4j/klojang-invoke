package org.klojang.path;

import org.junit.Test;
import org.klojang.util.Path;

import java.io.File;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathWalkerExceptionTest {

  @Test
  public void terminalValue() {
    PathWalkerException.Factory excFactory = PathWalkerException.terminalValue(
        Path.from("foo.bar.bozo"), 1, "teapot");
    assertTrue(excFactory.get().getMessage().contains("Terminal value encountered"));
  }

  @Test
  public void typeMismatch00() {
    PathWalkerException.Factory excFactory = PathWalkerException.typeMismatch(Path.from("foo.bar.bozo"),
        1,
        "my message to you");
    //System.out.println(excFactory.get().getMessage());
    assertEquals("Path foo.bar.bozo, segment 2: my message to you", excFactory.get().getMessage());
  }

  @Test
  public void typeMismatch01() {
    PathWalkerException.Factory excFactory = PathWalkerException.typeMismatch(
        Path.from("foo.bar.bozo"), 1, File.class, FileChannel.class);
    assertEquals(
        "Path foo.bar.bozo, segment 2: cannot assign File to FileChannel",
        excFactory.get().getMessage());
  }

  @Test
  public void keyDeserializationFailed00() {
    PathWalkerException.Factory excFactory = PathWalkerException.keyDeserializationFailed(
        Path.from("foo.bar.bozo"), 0, new KeyDeserializationException("no can do"));
    assertEquals(
        "Invalid path: \"foo.bar.bozo\" (segment 1). no can do",
        excFactory.get().getMessage());
  }

  @Test
  public void keyDeserializationFailed01() {
    PathWalkerException.Factory excFactory = PathWalkerException.keyDeserializationFailed(
        Path.from("foo.bar.bozo"), 0, new KeyDeserializationException());
    assertEquals(
        "Invalid path: \"foo.bar.bozo\" (segment 1). Failed to deserialize \"foo\" into map key",
        excFactory.get().getMessage());
  }

  @Test
  public void unexpectedError00() {
    PathWalkerException.Factory excFactory = PathWalkerException.unexpectedError(Path.from("foo.bar.bozo"),
        0,
        new Exception("Sorry"));
    System.out.println(excFactory.get().getMessage());
    assertEquals(
        "Path foo.bar.bozo, segment 1: Unexpected error. java.lang.Exception: Sorry",
        excFactory.get().getMessage());
  }

}