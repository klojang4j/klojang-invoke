package org.klojang.invoke;

import org.klojang.check.Check;
import org.klojang.util.InvokeException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Character.*;
import static java.lang.invoke.MethodType.methodType;
import static org.klojang.check.CommonChecks.keyIn;

/**
 * A {@code Builder} class for {@link BeanReader} instances. Use this class if the
 * bean class resides in a Java 9+ module that does not allow reflective access to
 * its classes.
 *
 * @param <T>
 */
public final class BeanReaderBuilder<T> {

  private final Class<T> beanClass;

  private final Map<String, Getter> getters = new HashMap<>();

  BeanReaderBuilder(Class<T> beanClass) {
    this.beanClass = beanClass;
  }

  /**
   * Registers the specified properties as properties of type {@code int}. The
   * properties must correspond to public getters on the bean class for which to
   * create the {@link BeanReader}.
   *
   * @param properties the property names
   * @return this instance
   */
  public BeanReaderBuilder<T> withInt(String... properties) {
    return with(int.class, properties);
  }

  /**
   * Registers the specified properties as properties of type {@code String}. The
   * properties must correspond to public getters on the bean class for which to
   * create the {@link BeanReader}.
   *
   * @param properties the property names
   * @return this instance
   */
  public BeanReaderBuilder<T> withString(String... properties) {
    return with(String.class, properties);
  }

  /**
   * Registers the specified properties as properties of the specified type. The
   * properties must correspond to public getters on the bean class for which to
   * create the {@link BeanReader}. If the bean class is a {@code record} type, there
   * is no difference between calling this method and calling
   * {@link #withGetter(Class, String...) withGetter()}.
   *
   * @param type the type of the properties
   * @param properties the property names
   * @return this instance
   */
  public BeanReaderBuilder<T> with(Class<?> type, String... properties) {
    Check.notNull(type, "type");
    Check.notNull(properties, "properties");
    for (String prop : properties) {
      Check.on(InvokeException::new, prop)
          .isNot(keyIn(), getters, "duplicate property: \"${arg}\"");
      String method = getMethodNameFromProperty(prop, type);
      Getter getter = getGetter(prop, method, type);
      getters.put(prop, getter);
    }
    return this;
  }

  /**
   * Registers the specified names as getters with the specified return type.
   * Especially meant to register getters with names that do not conform to the
   * JavaBeans naming conventions. The names are supposed to be the complete method
   * name of a public getter on the bean class. For example: "getLastName". If the
   * bean class is a {@code record} type, there is no difference between calling this
   * method and calling {@link #with(Class, String...) with()}.
   *
   * @param returnType the return type of the specified getters
   * @param names the names of the getters
   * @return this instance
   */
  public BeanReaderBuilder<T> withGetter(Class<?> returnType, String... names) {
    Check.notNull(returnType, "return type");
    Check.notNull(names, "names");
    for (String method : names) {
      String prop = getPropertyFromMethodName(method, returnType);
      Check.on(InvokeException::new, prop)
          .isNot(keyIn(), getters, "duplicate property: \"${arg}\"");
      Getter getter = getGetter(prop, method, returnType);
      getters.put(prop, getter);
    }
    return this;
  }

  /**
   * Returns a new {@code BeanReader} for instances of type {@code T}.
   *
   * @return a new {@code BeanReader} for instances of type {@code T}
   */
  public BeanReader<T> build() {
    return new BeanReader<>(beanClass, Map.copyOf(getters));
  }

  private Getter getGetter(String property, String method, Class<?> type) {
    try {
      MethodHandle mh = MethodHandles.publicLookup()
          .findVirtual(beanClass, method, methodType(type));
      return new Getter(mh, property, type);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new InvokeException(e.toString());
    }
  }

  private String getPropertyFromMethodName(String method, Class<?> type) {
    if (beanClass.isRecord()) {
      return method;
    }
    if ((type == boolean.class || type == Boolean.class)
        && method.length() > 2
        && method.startsWith("is")
        && isUpperCase(method.charAt(2))) {
      return extractName(method, 2);
    } else if (method.length() > 3
        && method.startsWith("get")
        && isUpperCase(method.charAt(3))) {
      return extractName(method, 3);
    }
    return method;
  }

  private String getMethodNameFromProperty(String prop, Class<?> type) {
    if (beanClass.isRecord()) {
      return prop;
    }
    String methodName;
    if (type == boolean.class || type == Boolean.class) {
      if (prop.length() > 1) {
        methodName = "is" + toUpperCase(prop.charAt(0)) + prop.substring(1);
      } else {
        methodName = "is" + toUpperCase(prop.charAt(0));
      }
    } else if (prop.length() > 1) {
      methodName = "get" + toUpperCase(prop.charAt(0)) + prop.substring(1);
    } else {
      methodName = "get" + toUpperCase(prop.charAt(0));
    }
    return methodName;
  }

  private static String extractName(String n, int from) {
    StringBuilder sb = new StringBuilder(n.length() - 3);
    sb.append(n.substring(from));
    sb.setCharAt(0, toLowerCase(sb.charAt(0)));
    return sb.toString();
  }

}