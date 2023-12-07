package org.klojang.invoke;

import org.klojang.check.Check;
import org.klojang.util.ArrayMethods;
import org.klojang.util.ClassMethods;

import java.lang.reflect.Method;
import java.util.*;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.reflect.Modifier.isStatic;
import static org.klojang.check.CommonChecks.empty;

/**
 * Assembles, caches, and supplies {@link Getter getters} for classes.
 *
 * @author Ayco Holleman
 */
public final class GetterFactory {

  /**
   * The one and only instance of {@code GetterFactory}.
   */
  public static final GetterFactory INSTANCE = new GetterFactory();

  private final Map<Class<?>, Map<String, Getter>> cache = new HashMap<>();

  private static final Set<String> NON_GETTERS = Set.of("getClass",
        "toString",
        "hashCode");

  private GetterFactory() { }

  /**
   * Returns the public {@link Getter getters} for the specified class. The returned
   * {@code Map} maps property names to {@code Getter} instances. The order of the keys
   * within the map is determined by the order of the methods returned by
   * {@link Class#getMethods()}.
   *
   * @param clazz the class for which to retrieve the public getters
   * @param strict if {@code false}, all non-static methods with a zero-length parameter
   * list and a non-{@code void} return type, except {@code getClass()},
   * {@code hashCode()} and {@code toString()}, will be regarded as getters. Otherwise
   * JavaBeans naming conventions will be applied regarding which methods qualify as
   * getters, with the exception that methods returning a {@link Boolean} are allowed to
   * have a name starting with "is". For {@code record} types, getters are collected as
   * though with {@code strict} equal to {@code false}.
   * @return the public getters of the specified class
   * @throws IllegalAssignmentException if the does not have any public getters
   */
  public Map<String, Getter> getGetters(Class<?> clazz, boolean strict) {
    Map<String, Getter> getters = cache.get(clazz);
    if (getters == null) {
      List<Method> methods = getMethods(clazz, strict);
      Check.that(methods).isNot(empty(), () -> new NoPublicGettersException(clazz));
      Map<String, Getter> map = LinkedHashMap.newLinkedHashMap(methods.size());
      for (Method m : methods) {
        String prop = getPropertyNameFromGetter(m, strict);
        map.put(prop, new Getter(m, prop));
      }
      cache.put(clazz, getters = Collections.unmodifiableMap(map));
    }
    return getters;
  }

  private static List<Method> getMethods(Class<?> clazz, boolean strict) {
    Method[] methods = clazz.getMethods();
    List<Method> getters = new ArrayList<>(methods.length - NON_GETTERS.size());
    for (Method m : methods) {
      if (isStatic(m.getModifiers())) {
        continue;
      } else if (m.getParameterCount() != 0) {
        continue;
      } else if (m.getReturnType() == void.class) {
        continue;
      } else if (NON_GETTERS.contains(m.getName())) {
        continue;
      } else if (strict && !clazz.isRecord() && !validGetterName(m)) {
        continue;
      }
      getters.add(m);
    }
    return getters;
  }

  private static String getPropertyNameFromGetter(Method m, boolean strict) {
    if (m.getDeclaringClass().isRecord()) {
      return m.getName();
    }
    String n = m.getName();
    if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
          && n.length() > 2
          && n.startsWith("is")
          && isUpperCase(n.charAt(2))) {
      return extractName(n, 2);
    } else if (n.length() > 3 && n.startsWith("get") && isUpperCase(n.charAt(3))) {
      return extractName(n, 3);
    }
    if (!strict) {
      return n;
    }
    throw notAProperty(m);
  }

  private static String extractName(String n, int from) {
    StringBuilder sb = new StringBuilder(n.length() - 3);
    sb.append(n.substring(from));
    sb.setCharAt(0, toLowerCase(sb.charAt(0)));
    return sb.toString();
  }

  private static boolean validGetterName(Method m) {
    String n = m.getName();
    if (n.length() > 4 && n.startsWith("get") && isUpperCase(n.charAt(3))) {
      return true;
    }
    if (n.length() > 3 && n.startsWith("is") && isUpperCase(n.charAt(2))) {
      return m.getReturnType() == boolean.class
            || m.getReturnType() == Boolean.class;
    }
    return false;
  }

  private static IllegalArgumentException notAProperty(Method m) {
    String fmt = "method %s %s(%s) in class %s is not a getter";
    String rt = ClassMethods.simpleClassName(m.getReturnType());
    String clazz = ClassMethods.className(m.getDeclaringClass());
    String params = ArrayMethods.implode(m.getParameterTypes(),
          ClassMethods::simpleClassName);
    String msg = String.format(fmt, rt, m.getName(), params, clazz);
    return new IllegalArgumentException(msg);
  }

}
