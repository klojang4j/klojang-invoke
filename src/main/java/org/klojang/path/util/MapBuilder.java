package org.klojang.path.util;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.check.aux.Result;
import org.klojang.path.Path;

import java.util.*;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.check.CommonProperties.strlen;
import static org.klojang.util.ObjectMethods.ifNull;
import static org.klojang.util.ObjectMethods.replaceIf;
import static org.klojang.util.StringMethods.EMPTY_STRING;

/**
 * <p>An elaborate builder class for Map&lt;String, Object&gt; (map-in-map)
 * pseudo-objects. A {@code MapBuilder} lets you write deeply nested values without having
 * to create the intermediate maps first. If they are missing, they will tacitly be
 * created. Map keys must not be {@code null} or an empty string. Map values can be
 * anything, including {@code null}.
 *
 * <p>Internally, a {@code MapBuilder} works with {@link Path} objects. See the
 * documentation for the {@code Path} class for how to specify path strings.
 *
 * <p><b>Example 1:</b>
 *
 * <blockquote><pre>{@code
 * MapBuilder mb = new MapBuilder();
 * mb.set("person.address.street", "12 Revolutionary Rd.")
 *  .set("person.address.state", "CA")
 *  .set("person.firstName", "John")
 *  .set("person.lastName", "Smith")
 *  .set("person.dateOfBirth", LocalDate.of(1967, 4, 4));
 * Map<String, Object> map = mb.build();
 * }</pre></blockquote>
 *
 * <p><b>Example 2:</b>
 *
 * <blockquote><pre>{@code
 * Map<String, Object> map = new MapBuilder()
 *  .in("person")
 *    .set("firstName", "John")
 *    .set("lastName", "Smith")
 *    .set("dateOfBirth", LocalDate.of(1967, 4, 4))
 *    .in("address")
 *      .set("street", "12 Revolutionary Rd.")
 *      .set("state", "CA")
 *      .up("person")
 *    .in("medical_status")
 *      .set("allergies", false)
 *      .set("smoker", true)
 *      .set("prescriptions", null)
 *  .build();
 * }</pre></blockquote>
 *
 * <p>Although the {@code MapBuilder} class is primarily aimed at buildings maps, you can
 * also use it to read maps:
 *
 * <blockquote><pre>{@code
 * MapBuilder mb = new MapBuilder(someMap);
 * String street = mb.get("person.address.street");
 * }</pre></blockquote>
 *
 * <p>For more flexibility, use the {@link org.klojang.path.PathWalker PathWalker} class.
 *
 * @author Ayco Holleman
 */
public final class MapBuilder {

  private static final String ERR_HOME_ALREADY = "already in root map";
  private static final String NO_SUCH_PARENT = "Parent of \"${0}\" is not \"${arg}\". Expected: \"${obj}\".";

  /**
   * Thrown when trying to write to a path that has already been set, or that extends
   * beyond a path segment with a terminal value (anything other than a map).
   *
   * @author Ayco Holleman
   */
  public static final class PathBlockedException extends IllegalArgumentException {

    private PathBlockedException(Path path, Object value) {
      super(createMessage(path, value));
    }

    private PathBlockedException(String msg) {
      super(msg);
    }

    private static String createMessage(Path path, Object value) {
      String fmt = "path \"%s\" blocked by terminal value %s";
      if (value instanceof String s) {
        value = '"' + s + '"';
      }
      return String.format(fmt, path, value);
    }

  }

  /*
   * When setting a path, or when processing the source map passed to the
   * constructor, we replace null with this value. This way, if Map.get(key) returns
   * null, we know for sure that the map does not contain the key. No need to follow
   * it up with a containsKey call. On its way out, _NULL_ is replaced again with
   * null.
   */
  private static final Object _NULL_ = new Object();


  /**
   * Creates a new {@code MapBuilder}.
   *
   * @return a new {@code MapBuilder}
   */
  public static MapBuilder begin() {
    return new MapBuilder();
  }

  /**
   * Creates a {@code MapBuilder} that starts out with the entries in the specified map.
   * The map is read, but not modified.
   *
   * @param map The initial {@code Map}
   * @return a {@code MapBuilder} that starts out with the entries in the specified map
   */
  public static MapBuilder begin(Map<String, Object> map) {
    return new MapBuilder(map);
  }

  private final Map<String, Object> map;
  private final Path root;
  private final MapBuilder parent;

  /**
   * Creates a new {@code MapBuilder}.
   */
  public MapBuilder() {
    this(new LinkedHashMap<>());
  }

  /**
   * Creates a {@code MapBuilder} that starts out with the entries in the specified map.
   * The map is read, but not modified.
   *
   * @param map The initial {@code Map}
   */
  public MapBuilder(Map<String, Object> map) {
    Check.notNull(map, Tag.MAP);
    this.map = LinkedHashMap.newLinkedHashMap(map.size() + 10);
    this.root = Path.empty();
    this.parent = null;
    init(this, map);
  }

  private MapBuilder(Path root, MapBuilder parent) {
    this.root = root;
    this.map = new LinkedHashMap<>();
    this.parent = parent;
  }

  /**
   * <p>Sets the specified path to the specified value. It is not allowed to
   * overwrite the value of a path that has already been set, even if set to {@code null}.
   * If necessary, use {@link #unset(String)} to unset the path's value first.
   *
   * <p>The value can be anything except a {@code Map} or {@code MapBuilder}. Use the
   * {@link #in(String) in()} method to create a new map at the specified path. It is
   * allowed to set a path's value to {@code null}.
   *
   * @param path the path at which to write the value
   * @param value the value
   * @return this {@code MapBuilder}
   */
  public MapBuilder set(String path, Object value) {
    Check.notNull(path, Tag.PATH);
    set(this, Path.from(path), value);
    return this;
  }

  /**
   * <p>Appends the specified element to the {@code Collection} found at the
   * specified path. If the path has not been set yet, it will first be set to an empty
   * {@link ArrayList}, to which the element will then be added. If the path is already
   * set to a non-{@code Collection} type, a {@link PathBlockedException} is thrown.
   *
   * @param path the path
   * @param element the element to add to the collection found or created at the
   *       specified path
   * @return this {@code MapBuilder}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public MapBuilder add(String path, Object element) {
    Check.notNull(path, Tag.PATH);
    Result<Object> result = poll(path);
    if (result.isAvailable()) {
      Object obj = result.get();
      Check.on(PathBlockedException::new, obj, path).is(instanceOf(), Collection.class);
      ((Collection) obj).add(element);
    } else {
      List<Object> list = new ArrayList<>();
      list.add(element);
      set(path, list);
    }
    return this;
  }

  /**
   * Returns a {@link Result} object containing the value of the specified path, or
   * {@link Result#notAvailable} if the path is not set.
   *
   * @param path the path
   * @return a {@link Result} object containing the value of the specified path, or
   *       {@link Result#notAvailable} if the path is not set
   * @see #isSet(String)
   */
  public Result<Object> poll(String path) {
    Check.notNull(path, Tag.PATH);
    return poll(this, Path.from(path));
  }

  /**
   * Returns the value of the specified path if set, else {@code null}.
   *
   * @param path the path
   * @param <T> The type to cast the path's value to
   * @return the value of the specified path if set, else {@code null}
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String path) {
    return (T) poll(path).orElse(null);
  }

  /**
   * Returns a {@code MapBuilder} for the map at the specified path. Once this method has
   * been called, <i>all subsequently specified paths</i> (including for subsequent calls
   * to {@code in()}) are taken relative to the specified path. If there is no map yet at
   * the specified path, it will be created. Ancestral maps will be created as well, as
   * and when needed. If any of the segments in the path (including the last segment) has
   * already been set, a {@link PathBlockedException} is thrown.
   *
   * @param path the path to be used as the base path. The path will itself be
   *       interpreted as relative to the <i>current</i> base path
   * @return a {@code MapBuilder} for the map found or created at the specified path
   */
  public MapBuilder in(String path) {
    Check.notNull(path, Tag.PATH);
    return in(this, Path.from(path));
  }

  /**
   * Jumps to another branch in the tree of nested maps. The difference between
   * {@code jump} and {@link #in(String) in} is that the path passed to {@code jump} is
   * always taken as an absolute path (i.e. relative to the root map), while the path
   * passed to {@code in} is taken relative to the path(s) passed to previous calls to
   * {@code in} and {@code jump}.
   *
   * @param path the absolute path to be used as the base path
   * @return a {@code MapBuilder} for the map found or created at the specified path
   * @see #in(String)
   */
  public MapBuilder jump(String path) {
    return parent == null ? in(path) : root().in(path);
  }

  /**
   * <p>Returns a {@code MapBuilder} for the parent map of the map currently being
   * edited. All subsequently specified paths will be taken relative to the parent map's
   * path. An {@link IllegalStateException} is thrown when trying to exit out of the root
   * map. This method must be passed the name of the parent map (the last segment of the
   * parent map's path). An {@link IllegalArgumentException} is thrown if the argument
   * does not equal the parent map's name. This is to make sure you will not accidentally
   * start writing to the wrong map, and it makes the map-building code more
   * intelligible.
   *
   * <blockquote><pre>{@code
   * Map<String, Object> map = new MapBuilder()
   *  .in("person")
   *    .set("firstName", "John")
   *    .set("lastName", "Smith")
   *    .in("address")
   *      .set("street", "12 Revolutionary Rd.")
   *      .set("state", "CA")
   *      .up("person")
   *    .set("dateOfBirth", LocalDate.of(1967, 4, 4))
   *  .build();
   * }</pre></blockquote>
   *
   * <p>You can chain {@code exit} calls. To exit from a map directly under the root
   * map, specify {@code null} or {@code ""} (an empty string):
   *
   * <blockquote><pre>{@code
   * MapBuilder mb = new MapBuilder();
   *  .in("department.manager.address")
   *    .set("street", "Sunset Blvd")
   *    .up("manager")
   *    .up("department")
   *    .up(null)
   *  .set("foo", "bar");
   * }</pre></blockquote>
   *
   * @param parent the name of the parent map
   * @return a {@code MapBuilder} for the parent map of the map currently being written to
   */
  public MapBuilder up(String parent) {
    MapBuilder mother = this.parent;
    Check.on(STATE, mother).is(notNull(), ERR_HOME_ALREADY);
    if (root.size() == 1) {
      Check.that(parent).is(empty(), "specify null or \"\" to exit to root map");
    } else {
      Check.that(parent).is(EQ(), mother.name(), NO_SUCH_PARENT, name());
    }
    return this.parent;
  }

  /**
   * Takes you back to the root map. All paths you specify will be interpreted as absolute
   * paths again.
   *
   * @return a {@code MapBuilder} for the root map
   */
  public MapBuilder root() {
    if (parent == null) {
      return this;
    }
    MapBuilder mb = parent;
    while (mb.parent != null) {
      mb = mb.parent;
    }
    return mb;
  }

  /**
   * Returns the key used to embed the current map within the parent map. If the current
   * map is the root map, an empty string is returned.
   *
   * @return the key used to embed the current map within the parent map
   */
  public String name() {
    if (parent == null) {
      return EMPTY_STRING;
    }
    return name(root);
  }

  /**
   * Returns the full path to the current map. That is, the path relative to which all
   * {@code path} arguments (e.g. for the {@link #set(String, Object) set()} method) are
   * taken.
   *
   * @return the full path to the current map
   */
  public String where() {
    return root.toString();
  }

  /**
   * Returns whether the specified path is set to a terminal value (and hence cannot be
   * extended).
   *
   * @param path the path
   * @return whether it is set to a terminal value
   */
  public boolean isSet(String path) {
    Check.notNull(path);
    return isSet(this, Path.from(path));
  }

  /**
   * Unsets the value of the specified path. This method returns quietly for non-existent
   * paths.
   *
   * @param path the path to unset.
   * @return this {@code MapBuilder}
   */
  public MapBuilder unset(String path) {
    Check.notNull(path);
    unset(this, Path.from(path));
    return this;
  }

  /**
   * Returns the {@code Map} resulting from the write actions. The returned map is
   * modifiable and retains the order in which the paths (now keys) were written. You can
   * continue to use the {@code MapBuilder} after a call to this method.
   *
   * @return the {@code Map} resulting from the write actions
   */
  public Map<String, Object> build() {
    MapBuilder mb = this;
    for (; mb.parent != null; mb = mb.parent)
      ;
    return createMap(mb);
  }

  /**
   * Returns a string representation of the map created thus far.
   *
   * @return a string representation of the map created thus far
   */
  @Override
  public String toString() {
    return build().toString();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void init(MapBuilder writer, Map map) {
    map.forEach((key, val) -> processEntry(writer, key, val));
  }

  @SuppressWarnings("rawtypes")
  private static void processEntry(MapBuilder writer, Object key, Object val) {
    Check.that(key)
          .isNot(NULL(), "illegal null key in source map")
          .isNot(empty(), "illegal empty key in source map")
          .is(instanceOf(), String.class, "illegal key type in source map: ${type}");
    String k = key.toString();
    if (val instanceof Map nested) {
      Path path = writer.root.append(k);
      MapBuilder mb = new MapBuilder(path, writer);
      writer.map.put(k, mb);
      init(mb, nested);
    } else {
      Check.that(val).isNot(instanceOf(), MapBuilder.class); // stifle nasty usage
      writer.map.put(k, ifNull(val, _NULL_));
    }
  }

  private static void set(MapBuilder writer, Path path, Object val) {
    String key = firstSegment(path);
    if (path.size() == 1) {
      if (writer.map.containsKey(key)) {
        throw alreadySet(writer, key);
      }
      Check.that(val, Tag.VALUE)
            .isNot(instanceOf(), Map.class)
            .isNot(instanceOf(), MapBuilder.class); // stifle nasty usage
      writer.map.put(key, ifNull(val, _NULL_));
    } else {
      set(getNestedWriter(writer, key), path.shift(), val);
    }
  }

  private static Result<Object> poll(MapBuilder writer, Path path) {
    String key = path.segment(0);
    Object val = writer.map.get(key);
    if (val instanceof MapBuilder nested) {
      if (path.size() == 1) {
        return Result.of(createMap(nested));
      }
      return poll(nested, path.shift());
    } else if (path.size() == 1 && val != null) {
      return Result.of(replaceIf(val, sameAs(), _NULL_, null));
    }
    return Result.notAvailable();
  }

  private static MapBuilder in(MapBuilder writer, Path path) {
    if (path.isEmpty()) {
      return writer;
    }
    String key = firstSegment(path);
    return in(getNestedWriter(writer, key), path.shift());
  }

  private static boolean isSet(MapBuilder writer, Path path) {
    String key = firstSegment(path);
    Object val = writer.map.get(key);
    if (val == null) {
      return false;
    } else if (path.size() == 1 || !(val instanceof MapBuilder)) {
      return true;
    }
    return isSet((MapBuilder) val, path.shift());
  }

  private static void unset(MapBuilder writer, Path path) {
    String key = firstSegment(path);
    if (path.size() == 1) {
      writer.map.remove(key);
    } else {
      unset(getNestedWriter(writer, key), path.shift());
    }
  }

  private static Map<String, Object> createMap(MapBuilder writer) {
    Map<String, Object> m = LinkedHashMap.newLinkedHashMap(writer.map.size());
    writer.map.forEach((key, val) -> {
      if (val instanceof MapBuilder mb) {
        m.put(key, createMap(mb));
      } else {
        m.put(key, replaceIf(val, sameAs(), _NULL_, null));
      }
    });
    return m;
  }

  private static MapBuilder getNestedWriter(MapBuilder writer, String key) {
    Path root = writer.root.append(key);
    Object val = writer.map.computeIfAbsent(key, k -> new MapBuilder(root, writer));
    if (val instanceof MapBuilder mb) {
      return mb;
    }
    throw new PathBlockedException(root, val);
  }

  private static PathBlockedException alreadySet(MapBuilder writer, String key) {
    Path absPath = writer.root.append(key);
    Object curVal = writer.map.get(key);
    return new PathBlockedException(absPath, curVal);
  }

  private static String name(Path path) { return path.segment(-1); }

  private static String firstSegment(Path path) {
    return Check.that(path.segment(0))
          .isNot(NULL(), "illegal null segment in path \"${0}\"", path)
          .has(strlen(), gt(), 0, "illegal empty segment in path \"${0}\"", path)
          .ok();
  }

}
