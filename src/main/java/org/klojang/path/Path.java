package org.klojang.path;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.check.aux.Emptyable;
import org.klojang.util.ArrayMethods;
import org.klojang.util.ObjectMethods;
import org.klojang.util.StringMethods;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOfRange;
import static org.klojang.check.CommonChecks.indexOf;
import static org.klojang.check.CommonChecks.lt;
import static org.klojang.check.CommonExceptions.INDEX;
import static org.klojang.util.ArrayMethods.EMPTY_STRING_ARRAY;
import static org.klojang.util.ArrayMethods.implode;

/**
 * Specifies a path to a value within an object. For example:
 * {@code employee.address.city}. A path string consists of path segments separated by the
 * dot character ('.'). Array indices are specified as separate path segments. For
 * example: {@code employees.3.address.city} &#8212; the city component of the address of
 * the fourth employee in a list or array of {@code Employee} instances. Non-numeric
 * segments can be either bean properties or map keys. Therefore the {@code Path} class
 * does not impose any constraints on what constitutes a valid path segment. A map key,
 * after all, can be anything &#8212; including {@code null} and the empty string. Of
 * course, if the path segment represents a JavaBean property, it must be a valid Java
 * identifier.
 *
 * <h2>Escaping</h2>
 * <p>These are the escaping rules when specifying path strings:
 * <ul>
 *   <li>If a map key happens to contain the segment separator ('.'), it must be
 *      escaped using the circumflex character ('^'). So key {@code "my.awkward.map.key"}
 *      should be escaped to {@code "my^.awkward^.map^.key"}.
 *  <li>For a map key with value {@code null}, use this escape sequence: {@code "^0"}. So
 *      {@code "lookups.^0"} represents the {@code null} key in the {@code lookups} map.
 *  <li>If a segment needs to represent a map key whose value is the empty string,
 *      simply make it a zero-length segment: {@code "lookups..name"}. This implies that a
 *      path string that ends with a dot in fact ends with an empty (zero-length) segment.
 *  <li>The escape character ('^') itself <b>may</b> be escaped. Thus, key
 *      {@code "super^awkward"} can be represented either as {@code "super^awkward"} or as
 *      {@code "super^^awkward"}. If the escape character is not followed by a dot or
 *      another escape character, it is just that character. You <b>must</b> escape the
 *      escape character, however, if the <i>entire</i> path segment happens to be the
 *      escape sequence for {@code null} ({@code "^0"}). Thus, in the odd case you have a
 *      key with value {@code "^0"}, escape it to {@code "^^0"}.
 * </ul>
 *
 * <p>You can let the {@link #escape(String) escape} method do the escaping for you. Do
 * not escape path segments when passing them individually (as a {@code String} array) to
 * the constructor. Only escape them when passing a complete path string.
 *
 * @author Ayco Holleman
 */
public final class Path implements Comparable<Path>, Iterable<String>, Emptyable {

  private static final Path EMPTY_PATH = new Path();

  // segment separator
  private static final char SEP = '.';

  // escape character
  private static final char ESC = '^';

  // escape sequence to use for null keys
  private static final String NULL_SEGMENT = "^0";

  /**
   * Returns a new {@code Path} instance for the specified path string.
   *
   * @param path the path string from which to create a {@code Path}
   * @return a new {@code Path} instance for the specified path string
   */
  public static Path from(String path) {
    Check.notNull(path, Tag.PATH);
    if (path.isEmpty()) {
      return EMPTY_PATH;
    }
    return new Path(path);
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i> The array may contain {@code null} values as well as empty strings.
   *
   * @param segments the path segments
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path from(String[] segments) {
    Check.notNull(segments);
    return segments.length == 0 ? EMPTY_PATH : new Path(segments);
  }

  /**
   * Returns an empty {@code Path} instance, consisting of zero path segments.
   *
   * @return an empty {@code Path} instance, consisting of zero path segments
   */
  public static Path empty() {
    return EMPTY_PATH;
  }

  /**
   * Returns a {@code Path} consisting of a single segment. <i>Do not escape the
   * segment.</i>
   *
   * @param segment the one and only segment of the {@code Path}
   * @return a {@code Path} consisting of a single segment
   */
  public static Path of(String segment) {
    return new Path(new String[]{segment});
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i>
   *
   * @param segment0 the 1st segment
   * @param segment1 the 2nd segment
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path of(String segment0, String segment1) {
    return new Path(new String[]{segment0, segment1});
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i>
   *
   * @param segment0 the 1st segment
   * @param segment1 the 2nd segment
   * @param segment2 the 3rd segment
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path of(String segment0, String segment1, String segment2) {
    return new Path(new String[]{segment0, segment1, segment2});
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i>
   *
   * @param segment0 the 1st segment
   * @param segment1 the 2nd segment
   * @param segment2 the 3rd segment
   * @param segment3 the 4th segment
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path of(
        String segment0,
        String segment1,
        String segment2,
        String segment3) {
    return new Path(new String[]{segment0, segment1, segment2, segment3});
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i>
   *
   * @param segment0 the 1st segment
   * @param segment1 the 2nd segment
   * @param segment2 the 3rd segment
   * @param segment3 the 4th segment
   * @param segment4 the 5th segment
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path of(
        String segment0,
        String segment1,
        String segment2,
        String segment3,
        String segment4) {
    return new Path(new String[]{segment0, segment1, segment2, segment3, segment4});
  }

  /**
   * Returns a {@code Path} consisting of the specified segments. <i>Do not escape the
   * segments.</i>
   *
   * @param segments the path segments
   * @return a {@code Path} consisting of the specified segments
   */
  public static Path of(String... segments) {
    return from(segments);
  }

  /**
   * Returns a copy of the specified path.
   *
   * @param other the {@code Path} to copy.
   * @return a copy of the specified path
   */
  public static Path copyOf(Path other) {
    return other == EMPTY_PATH ? other : Check.notNull(other).ok(Path::new);
  }

  /**
   * Escapes the specified path segment. Do not escape path segments when passing them
   * individually to one of the static factory methods. Only use this method to assemble
   * complete path strings from individual path segments. Generally you don't need this
   * method when specifying path strings, unless one or more path segments contain a dot
   * ('.') or the escape character ('^') itself. The argument may be {@code null}, in
   * which case the escape sequence for {@code null} ({@code "^0"}) is returned.
   *
   * @param segment the path segment to escape
   * @return the escaped version of the segment
   */
  public static String escape(String segment) {
    if (segment == null) {
      return NULL_SEGMENT;
    } else if (segment.equals(NULL_SEGMENT)) {
      return ESC + NULL_SEGMENT;
    }
    int x = segment.indexOf(SEP);
    if (x == -1) {
      return segment;
    }
    StringBuilder sb = new StringBuilder(segment.length() + 3)
          .append(segment.substring(0, x))
          .append(ESC)
          .append(SEP);
    for (int i = x + 1; i < segment.length(); ++i) {
      char c = segment.charAt(i);
      switch (c) {
        case SEP -> sb.append(ESC).append(SEP);
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  private final String[] elems;
  private String str; // Caches toString()
  private int hash; // Caches hashCode()

  // Reserved for EMPTY_PATH
  private Path() {
    elems = EMPTY_STRING_ARRAY;
  }

  private Path(String path) {
    elems = parse(str = path);
  }

  private Path(String[] segments) {
    elems = new String[segments.length];
    arraycopy(segments, 0, elems, 0, segments.length);
  }

  private Path(Path other) {
    // Since we are immutable we can happily share state
    this.elems = other.elems;
    this.str = other.str;
    this.hash = other.hash;
  }

  /**
   * Returns the path segment at the specified index. Specify a negative index to retrieve
   * a segment relative to end of the {@code Path} (-1 would return the last path
   * segment).
   *
   * @param index the array index of the path segment
   * @return the path segment at the specified index.
   */
  public String segment(int index) {
    if (index < 0) {
      return Check.that(elems.length + index)
            .is(indexOf(), elems)
            .mapToObj(x -> elems[x]);
    }
    return Check.that(index).is(indexOf(), elems).mapToObj(x -> elems[x]);
  }

  /**
   * Returns a new {@code Path} starting with the segment at the specified array index.
   * Specify a negative index to count back from the last segment of the {@code Path} (-1
   * returns the last path segment).
   *
   * @param offset the index of the first segment of the new {@code Path}
   * @return a new {@code Path} starting with the segment at the specified array index
   */
  public Path subPath(int offset) {
    int from = offset < 0 ? elems.length + offset : offset;
    Check.that(from).is(lt(), elems.length);
    return new Path(copyOfRange(elems, from, elems.length));
  }

  /**
   * Returns a new {@code Path} consisting of {@code length} segments starting with
   * segment {@code offset}. The {@code offset} argument may be negative to specify a
   * segment relative to the end of the {@code Path}. Thus, -1 specifies the last segment
   * of the {@code Path}.
   *
   * @param offset the index of the first segment to extract
   * @param length the number of segments to extract
   * @return a new {@code Path} consisting of {@code len} segments starting with segment
   * {@code from}.
   */
  public Path subPath(int offset, int length) {
    if (offset < 0) {
      offset = elems.length + offset;
    }
    Check.offsetLength(elems.length, offset, length);
    return new Path(copyOfRange(elems, offset, offset + length));
  }

  /**
   * Returns a {@code Path} with all segments of this {@code Path} except the first
   * segment. If the path is empty, this method returns {@code null}. If it consists of a
   * single segment, this method returns {@link #EMPTY_PATH}.
   *
   * @return a {@code Path} with all segments of this {@code Path} except the first
   * segment
   */
  public Path shift() {
    return switch (elems.length) {
      case 0 -> null;
      case 1 -> EMPTY_PATH;
      default -> new Path(copyOfRange(elems, 1, elems.length));
    };
  }

  /**
   * Returns a {@code Path} with all segments of this {@code Path} except the last
   * segment. If the path is empty, this method returns {@code null}. If it consists of a
   * single segment, this method returns {@link #EMPTY_PATH}.
   *
   * @return the parent of this {@code Path}
   */
  public Path parent() {
    return switch (elems.length) {
      case 0 -> null;
      case 1 -> EMPTY_PATH;
      default -> new Path(copyOfRange(elems, 0, elems.length - 1));
    };
  }

  /**
   * Returns a new {@code Path} containing only the segments of this {@code Path} that are
   * not array indices.
   *
   * @return a new {@code Path} without any array indices
   */
  public Path getCanonicalPath() {
    String[] canonical = stream()
          .filter(s -> !s.chars().allMatch(Character::isDigit)
                || new BigInteger(s).intValueExact() < 0)
          .toArray(String[]::new);
    return canonical.length == 0 ? EMPTY_PATH : new Path(canonical);
  }

  /**
   * Returns a new {@code Path} representing the concatenation of this {@code Path} and
   * the specified {@code Path}.
   *
   * @param path the path to append to this {@code Path}
   * @return a new {@code Path} representing the concatenation of this {@code Path} and
   * the specified {@code Path}
   */
  public Path append(String path) {
    Check.notNull(path);
    return append(new Path(parse(path)));
  }

  /**
   * Returns a new {@code Path} consisting of the segments of this {@code Path} plus the
   * segments of the specified {@code Path}.
   *
   * @param other the {@code Path} to append to this {@code Path}.
   * @return a new {@code Path} consisting of the segments of this {@code Path} plus the
   * segments of the specified {@code Path}
   */
  public Path append(Path other) {
    Check.notNull(other);
    return new Path(ArrayMethods.concat(elems, other.elems));
  }

  /**
   * Returns a new {@code Path} with the path segment at the specified array index set to
   * the new value.
   *
   * @param index the array index of the segment to replace
   * @param newValue the new segment
   * @return a new {@code Path} with the path segment at the specified array index set to
   * the new value
   */
  public Path replace(int index, String newValue) {
    Check.on(INDEX, index, Tag.INDEX).is(indexOf(), elems);
    String[] copy = Arrays.copyOf(elems, elems.length);
    copy[index] = newValue;
    return new Path(copy);
  }

  /**
   * Returns a {@code Path} in which the order of the segments is reversed.
   *
   * @return a {@code Path} in which the order of the segments is reversed
   */
  public Path reverse() {
    String[] elems;
    if ((elems = this.elems).length > 1) {
      String[] segments = new String[elems.length];
      int x = elems.length;
      for (int i = 0; i < elems.length; ++i) {
        segments[i] = elems[--x];
      }
      return new Path(segments);
    }
    return this;
  }

  /**
   * Returns an {@code Iterator} over the path segments.
   *
   * @return an {@code Iterator} over the path segments
   */
  @Override
  public Iterator<String> iterator() {
    return new Iterator<>() {
      private int i;

      public boolean hasNext() {
        return i < elems.length;
      }

      public String next() {
        if (i < elems.length) {
          return elems[i++];
        }
        throw new IndexOutOfBoundsException(i);
      }
    };
  }

  /**
   * Returns a {@code Stream} of path segments.
   *
   * @return a {@code Stream} of path segments
   */
  public Stream<String> stream() {
    return Arrays.stream(elems);
  }

  /**
   * Returns the number of segments in this {@code Path}.
   *
   * @return the number of segments in this {@code Path}
   */
  public int size() {
    return elems.length;
  }

  /**
   * Returns {@code true} if this is an empty {@code Path}, consisting of zero segments.
   *
   * @return {@code true} if this is an empty {@code Path}, consisting of zero segments
   */
  @Override
  public boolean isEmpty() {
    return elems.length == 0;
  }

  /**
   * Returns {@code true} if this is a non-empty {@code Path}, consisting only of
   * non-null, non-empty of path segments.
   *
   * @return {@code true} if this is a non-empty {@code Path}, consisting only of
   * non-null, non-empty of path segments
   */
  @Override
  public boolean isDeepNotEmpty() {
    return ObjectMethods.isDeepNotEmpty(elems);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
          || (obj instanceof Path p && Arrays.equals(elems, p.elems));
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = Arrays.deepHashCode(elems);
    }
    return hash;
  }

  @Override
  public int compareTo(Path other) {
    Check.notNull(other);
    return Arrays.compare(elems, other.elems);
  }

  /**
   * Returns this {@code Path} as a string, properly escaped.
   *
   * @return this {@code Path} as a string, properly escaped
   */
  @Override
  public String toString() {
    if (str == null) {
      str = implode(elems, Path::escape, ".", 0, elems.length);
    }
    return str;
  }

  private static String[] parse(String path) {
    ArrayList<String> elems = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    int len = path.length();
    for (int i = 0; i < len; i++) {
      switch (path.charAt(i)) {
        case SEP -> {
          elems.add(sb.toString());
          sb.setLength(0);
        }
        case ESC -> {
          if (i < len - 1) {
            char c = path.charAt(i + 1);
            if (c == SEP || c == ESC) {
              sb.append(c);
              ++i;
            } else if (c == '0'
                  && sb.length() == 0
                  && (i == len - 2 || path.charAt(i + 2) == SEP)) {
              elems.add(null);
              sb.setLength(0);
              i += 2;
            } else {
              sb.append(ESC);
            }
          } else {
            sb.append(ESC);
          }
        }
        default -> sb.append(path.charAt(i));
      }
    }
    if (sb.length() > 0) {
      elems.add(sb.toString());
    } else if (path.charAt(len - 1) == SEP) {
      elems.add(StringMethods.EMPTY_STRING);
    }
    return elems.toArray(String[]::new);
  }

}
