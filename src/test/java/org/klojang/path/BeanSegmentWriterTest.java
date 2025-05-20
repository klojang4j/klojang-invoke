package org.klojang.path;

import org.junit.Test;
import org.klojang.util.Path;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BeanSegmentWriterTest {

  @Test(expected = DeadEndException.class)
  public void test00() {
    PathWalker pw = new PathWalker(List.of(Path.from("foo")), false);
    try {
      Person person = new Person();
      pw.write(person, 666);
    } catch (DeadEndException e) {
      assertTrue(e.getMessage().contains("No accessible property"));
      throw e;
    }
  }

  @Test
  public void test01() {
    BeanSegmentWriter bsw = new BeanSegmentWriter(true, null);
    Person person = new Person();
    bsw.write(person, Path.from("foo"), 666);
    // not much happenin'
  }

  @Test(expected = DeadEndException.class)
  public void test02() {
    BeanSegmentWriter bsw = new BeanSegmentWriter(false, null);
    Person person = new Person();
    try {
      bsw.write(person, Path.from("firstName.hash"), 666);
    } catch (DeadEndException e) {
      assertTrue(e.getMessage().contains("No accessible property"));
      throw e;
    }
  }

  @Test(expected = DeadEndException.class)
  public void test03() {
    PathWalker pw = new PathWalker(List.of(Path.from("manager.address.street")),
        false);
    Department dept = new Department();
    try {
      pw.write(dept, 666);
    } catch (DeadEndException e) {
      assertTrue(e.getMessage().contains("Terminal value encountered"));
      throw e;
    }
  }

  @Test(expected = DeadEndException.class)
  public void test04() {
    PathWalker pw = new PathWalker(List.of(Path.from("manager..street")), false);
    Department dept = new Department();
    dept.setManager(new Employee());
    try {
      pw.write(dept, 666);
    } catch (DeadEndException e) {
      assertTrue(e.getMessage().contains("Segment must not be null"));
      throw e;
    }
  }

  @Test(expected = DeadEndException.class)
  public void test05() {
    PathWalker pw = new PathWalker(List.of(Path.from("firstName")), false);
    Person person = new Person();
    try {
      pw.write(person, new File("/tmp/foo.txt"));
    } catch (DeadEndException e) {
      assertTrue(e.getMessage().contains("cannot assign"));
      throw e;
    }
  }

}
