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
 * Assembles, caches and supplies {@link Setter setters} for classes.
 *
 * @author Ayco Holleman
 */
public final class SetterFactory {

  /**
   * The one and only instance of {@code SetterFactory}.
   */
  public static final SetterFactory INSTANCE = new SetterFactory();

  private final Map<Class<?>, Map<String, Setter>> cache = new HashMap<>();

  private SetterFactory() { }

  /**
   * Returns the public {@link Setter setters} for the specified class. The returned
   * {@code Map} maps property names to {@code Setter} instances. The order of the keys
   * within the map is determined by the order of the methods returned by
   * {@link Class#getMethods()}.
   *
   * @param clazz The class for which to retrieve the public setters
   * @return The public setters of the specified class
   * @throws IllegalAssignmentException If the does not have any public setters
   */
  public Map<String, Setter> getSetters(Class<?> clazz) {
    Map<String, Setter> setters = cache.get(clazz);
    if (setters == null) {
      List<Method> methods = getMethods(clazz);
      Check.that(methods).isNot(empty(), () -> new NoPublicSettersException(clazz));
      Map<String, Setter> map = LinkedHashMap.newLinkedHashMap(methods.size());
      for (Method m : methods) {
        String prop = getPropertyNameFromSetter(m);
        map.put(prop, new Setter(m, prop));
      }
      setters = Map.copyOf(map);
      cache.put(clazz, setters);
    }
    return setters;
  }

  private static List<Method> getMethods(Class<?> beanClass) {
    Method[] methods = beanClass.getMethods();
    List<Method> setters = new ArrayList<>();
    for (Method m : methods) {
      if (!beanClass.isRecord()
            && !isStatic(m.getModifiers())
            && m.getParameterCount() == 1
            && m.getReturnType() == void.class
            && isValidSetterName(m)
      ) {
        setters.add(m);
      }
    }
    return setters;
  }

  private static String getPropertyNameFromSetter(Method m) {
    String n = m.getName();
    if (n.startsWith("set") && isUpperCase(n.charAt(3))) {
      return extractName(n, 3);
    }
    throw notAProperty(m);
  }

  private static String extractName(String n, int from) {
    StringBuilder sb = new StringBuilder(n.length() - 3);
    sb.append(n.substring(from));
    sb.setCharAt(0, toLowerCase(sb.charAt(0)));
    return sb.toString();
  }

  private static IllegalArgumentException notAProperty(Method m) {
    String fmt = "method %s %s(%s) in class %s is not a setter";
    String rt = ClassMethods.simpleClassName(m.getReturnType());
    String clazz = ClassMethods.className(m.getDeclaringClass());
    String params = ArrayMethods.implode(m.getParameterTypes(),
          ClassMethods::simpleClassName);
    String msg = String.format(fmt, rt, m.getName(), params, clazz);
    return new IllegalArgumentException(msg);
  }

  private static boolean isValidSetterName(Method m) {
    String n = m.getName();
    return n.length() > 3 && n.startsWith("set") && isUpperCase(n.charAt(3));
  }

}
