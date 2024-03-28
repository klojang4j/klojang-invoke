package org.klojang.path.util;

import org.junit.Test;
import org.klojang.check.aux.Result;

import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;
import static org.klojang.path.util.MapBuilder.PathBlockedException;

public class MapBuilderTest {

  @Test
  public void set00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "12 Revolutionary Rd.")
          .set("person.address.state", "CA")
          .set("person.firstName", "John")
          .set("person.lastName", "Smith")
          .set("person.born", LocalDate.of(1967, 4, 4));
    String expected =
          "{person={address={street=12 Revolutionary Rd., state=CA}, firstName=John, lastName=Smith,"
                + " born=1967-04-04}}";
    assertEquals(expected, mb.build().toString());
  }

  @Test // Are we OK with null values?
  public void set01() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("person.address.street", "12 Revolutionary Rd.")
          .set("person.address.state", null)
          .set("person.firstName", "John")
          .set("person.lastName", null)
          .set("person.born", LocalDate.of(1967, 4, 4));
    String expected =
          "{person={address={street=12 Revolutionary Rd., state=null}, firstName=John, "
                + "lastName=null, born=1967-04-04}}";
    assertEquals(expected, mb.build().toString());
  }

  @Test(expected = PathBlockedException.class)
  public void set02() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("person.address.street", "12 Revolutionary Rd.")
          .set("person.address.street.foo", "bar");
  }

  @Test(expected = MapBuilder.PathBlockedException.class)
  public void set03() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("person.address.street", null)
          .set("person.address.street", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void set04() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", new HashMap<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void set05() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", new MapBuilder());
  }

  @Test(expected = PathBlockedException.class)
  public void set06() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("person.address", "foo")
          .set("person.address.street", "Sunset Blvd");
  }

  @Test // do we make the null -> _NULL_ -> null round trip?
  public void set07() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("foo.bar.teapot", null)
          .set("foo.bar.fun", true)
          .set("foo.bar.number", 8);
    Map<String, Object> nested = new HashMap<>();
    nested.put("teapot", null);
    nested.put("fun", true);
    nested.put("number", 8);
    Map<String, Object> expected = Map.of("foo", Map.of("bar", nested));
    assertEquals(expected, mb.build());
  }

  @Test
  public void get00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertEquals("foo", mb.poll("person.address.street").get());
    assertEquals(Map.of("street", "foo"), mb.poll("person.address").get());
    assertEquals(Map.of("address", Map.of("street", "foo")),
          mb.poll("person").get());
    assertEquals(Result.notAvailable(),
          mb.poll("person.address.street.teapot.coffee"));
    assertEquals(Result.notAvailable(), mb.poll("person.address.street.teapot"));
    assertEquals(Result.notAvailable(), mb.poll("person.address.teapot"));
    assertEquals(Result.notAvailable(), mb.poll("person.teapot"));
    assertEquals(Result.notAvailable(), mb.poll("teapot"));
  }

  @Test // do we make the null -> _NULL_ -> null round trip?
  public void get01() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", null);
    assertTrue(mb.isSet("person.address.street"));
    assertNull(mb.poll("person.address.street").get());
  }

  @Test
  public void in00() {
    MapBuilder mb = new MapBuilder();
    mb.in("person")
          .set("firstName", "John")
          .set("lastName", "Smith")
          .set("born", LocalDate.of(1967, 4, 4))
          .in("address")
          .set("street", "12 Revolutionary Rd.")
          .set("state", "CA");
    String expected =
          "{person={firstName=John, lastName=Smith, born=1967-04-04, address={street=12 "
                + "Revolutionary Rd., state=CA}}}";
    assertEquals(expected, mb.build().toString());
  }

  @Test(expected = PathBlockedException.class)
  public void in01() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo.bar.bozo", "teapot");
    try {
      mb.in("foo.bar.bozo");
    } catch (PathBlockedException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Test
  public void in02() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo.bar.bozo", "teapot");
    mb.in("foo.bar").set("ping", "pong");
    Map<String, Object> expected = Map.of("foo",
          Map.of("bar", Map.of("bozo", "teapot", "ping", "pong")));
    assertEquals(expected, mb.build());
  }

  @Test
  public void in03() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo.bar.bozo", "teapot");
    //@formatter:off
    mb
        .in("foo.bar")
        .set("ping", "pong")
        .set("boom", "bam")
        .in("physics")
        .set("big", "bang");
    //@formatter:on
    Map<String, Object> expected = Map.of("foo",
          Map.of("bar", Map.of(
                "bozo", "teapot",
                "ping", "pong",
                "boom", "bam",
                "physics", Map.of("big", "bang")
          )));
    assertEquals(expected, mb.build());
  }

  @Test(expected = PathBlockedException.class)
  public void in04() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo.bar.bozo", "teapot");
    mb
          .in("foo.bar")
          .set("ping", "pong")
          .set("boom", "bam")
          .in("bozo");
  }

  @Test
  public void up00() {
    MapBuilder mb = new MapBuilder();
    mb.in("person.address")
          .set("street", "Sunset Blvd")
          .up("person")
          .set("firstName", "John");
    assertEquals("{person={address={street=Sunset Blvd}, firstName=John}}",
          mb.build().toString());
  }

  @Test
  public void up01() {
    MapBuilder mb = new MapBuilder();
    mb.in("person.address")
          .set("street", "Sunset Blvd")
          .up("person")
          .set("firstName", "John");
    Map expected = Map.of("person",
          Map.of("firstName", "John", "address", Map.of("street", "Sunset Blvd")));
    assertEquals(expected, mb.build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void up02() {
    MapBuilder mb = new MapBuilder();
    try {
      mb.in("person.address")
            .set("street", "Sunset Blvd")
            .up("teapot");
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void up03() {
    MapBuilder mb = new MapBuilder();
    try {
      mb.up("teapot");
    } catch (IllegalStateException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Test
  public void up04() {
    MapBuilder mb = new MapBuilder();
    mb.in("department.manager.address")
          .set("street", "Sunset Blvd")
          .up("manager")
          .up("department")
          .set("foo", "bar");
    Map<String, Object> expected = Map.of(
          "department",
          Map.of("foo",
                "bar",
                "manager",
                Map.of("address", Map.of("street", "Sunset Blvd")))
    );
    assertEquals(expected, mb.build());
  }

  @Test
  public void up05() {
    MapBuilder mb = new MapBuilder();
    mb.in("department.manager.address")
          .set("street", "Sunset Blvd")
          .up("manager")
          .up("department")
          .up(null)
          .set("foo", "bar");
    Map<String, Object> expected = Map.of(
          "foo", "bar",
          "department",
          Map.of("manager",
                Map.of("address", Map.of("street", "Sunset Blvd")))
    );
    assertEquals(expected, mb.build());
  }

  @Test
  public void up06() {
    MapBuilder mb = new MapBuilder();
    mb.in("department.manager.address")
          .set("street", "Sunset Blvd")
          .up("manager")
          .up("department")
          .up(null)
          .set("foo", "bar");
    Map<String, Object> expected = Map.of(
          "foo", "bar",
          "department",
          Map.of("manager",
                Map.of("address", Map.of("street", "Sunset Blvd")))
    );
    assertEquals(expected, mb.build());
  }

  @Test
  public void reset00() {
    MapBuilder mb = new MapBuilder();
    mb.in("person.address")
          .set("street", "Sunset Blvd")
          .root()
          .set("firstName", "John");
    assertEquals("{person={address={street=Sunset Blvd}}, firstName=John}",
          mb.build().toString());
  }

  public void reset01() {
    MapBuilder mb = new MapBuilder();
    assertSame(mb, mb.root());
  }

  @Test
  public void isSet00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertTrue(mb.isSet("person.address.street.teapot"));
  }

  @Test
  public void isSet01() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertTrue(mb.isSet("person.address.street"));
  }

  @Test
  public void isSet02() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertTrue(mb.isSet("person.address"));
  }

  @Test
  public void isSet03() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertTrue(mb.isSet("person"));
  }

  @Test
  public void isSet04() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("teapot"));
  }

  @Test
  public void isSet05() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("person.teapot"));
  }

  @Test
  public void isSet06() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("person.address.teapot"));
  }

  @Test
  public void isSet07() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("person.teapot.address"));
  }

  @Test
  public void isSet08() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("person.teapot.address.coffee"));
  }

  @Test
  public void isSet09() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertFalse(mb.isSet("person.teapot.address.coffee.pot"));
  }

  @Test
  public void name00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertEquals("", mb.name());
    assertEquals("person", mb.jump("person").name());
    assertEquals("address", mb.jump("person.address").name());
  }

  @Test
  public void where00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "foo");
    assertEquals("", mb.where());
    assertEquals("person", mb.jump("person").where());
    assertEquals("person.address", mb.jump("person.address").where());
  }

  @Test
  public void unset00() {
    MapBuilder mb = new MapBuilder();
    mb.set("person", "foo");
    assertTrue(mb.isSet("person"));
    mb.unset("person");
    assertFalse(mb.isSet("person"));
  }

  @Test
  public void unset01() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address", "foo");
    assertTrue(mb.isSet("person.address"));
    assertTrue(mb.isSet("person"));
    mb.unset("person.address");
    assertFalse(mb.isSet("person.address"));
    assertTrue(mb.isSet("person"));
    mb.unset("person");
    assertFalse(mb.isSet("person"));
  }

  @Test
  public void unset02() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "Sunset Blvd");
    mb.set("person.address.zipcode", "CA 12345");

    assertFalse(mb.isSet("person.address.country"));
    // can do it nevertheless:
    mb.unset("person.address.country");
    mb.unset("person.address.country.planet");

    assertTrue(mb.isSet("person.address.street"));
    assertTrue(mb.isSet("person.address.zipcode"));
    assertTrue(mb.isSet("person.address"));
    assertTrue(mb.isSet("person"));

    mb.unset("person");

    assertFalse(mb.isSet("person.address.street"));
    assertFalse(mb.isSet("person.address.zipcode"));
    assertFalse(mb.isSet("person.address"));
    assertFalse(mb.isSet("person"));
  }

  @Test
  public void unset03() {
    MapBuilder mb = new MapBuilder();
    mb.set("person.address.street", "Sunset Blvd");
    mb.set("person.address.zipcode", "CA 12345");

    assertFalse(mb.isSet("person.address.country"));
    // can do it nevertheless:
    mb.unset("person.address.country");
    mb.unset("person.address.country.planet");

    assertTrue(mb.isSet("person.address.street"));
    assertTrue(mb.isSet("person.address.zipcode"));
    assertTrue(mb.isSet("person.address"));
    assertTrue(mb.isSet("person"));

    mb.unset("person.address.street");

    assertFalse(mb.isSet("person.address.street"));
    assertTrue(mb.isSet("person.address.zipcode"));
    assertTrue(mb.isSet("person.address"));
    assertTrue(mb.isSet("person"));
  }

  @Test
  public void sourceMap00() {
    Map<String, Object> source = Map.of("foo",
          Map.of("teapot", "coffee"),
          "bar",
          true);
    MapBuilder mb = new MapBuilder(source);
    assertEquals(source, mb.build());
    mb.set("ping", 1).set("pong", false);
    Map<String, Object> expected = Map.of("foo",
          Map.of("teapot", "coffee"),
          "bar",
          true, "ping", 1, "pong", false);
    assertEquals(expected, mb.build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void sourceMap01() {
    Map source = Map.of("", "bar");
    try {
      MapBuilder mb = new MapBuilder(source);
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Test // make null -> _NULL_ -> null round trip
  public void sourceMap02() {
    Map source = new HashMap();
    source.put("foo", null);
    MapBuilder mb = new MapBuilder(source);
    assertEquals(source, mb.build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void badSegment00() {
    MapBuilder mb = new MapBuilder();
    try {
      mb.set("person.^0.street", "foo"); // ^0 is escape sequence for null
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Test
  public void jump00() {
    MapBuilder mb = new MapBuilder();
    mb
          .set("department.person.address.street", "Main St.")
          .set("department.person.address.state", "CA")
          .set("department.person.firstName", "John")
          .set("car.brand.name", "BMW")
          .set("person.born", LocalDate.of(1967, 4, 4));
    mb = mb.jump("department.person");
    assertEquals("department.person", mb.where());
    mb = mb.jump("car.brand");
    assertEquals("car.brand", mb.where());
    mb = mb.up("car");
    assertEquals("car", mb.where());
    mb = mb.up("");
    assertEquals("", mb.where());
  }

  @Test
  public void add00() {
    MapBuilder mb = new MapBuilder();
    mb.add("foo", 1);
    mb.add("foo", 2);
    mb.add("foo", 3);
    List l = (List) mb.build().get("foo");
    assertEquals(List.of(1, 2, 3), l);
  }

  @Test
  public void add01() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo", new HashSet<>());
    mb.add("foo", 1);
    mb.add("foo", 2);
    mb.add("foo", 3);
    Set s = (Set) mb.build().get("foo");
    assertEquals(Set.of(1, 2, 3), s);
  }

  @Test(expected = PathBlockedException.class)
  public void add02() {
    MapBuilder mb = new MapBuilder();
    mb.set("foo", "hello world");
    mb.add("foo", 1);
  }

}
