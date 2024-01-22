package org.klojang.invoke;

import org.klojang.util.ExceptionMethods;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandles.publicLookup;

/**
 * Represents a getter for a single property.
 *
 * @author Ayco Holleman
 */
public final class Getter {

  private final Class<?> returnType;
  private final MethodHandle mh;
  private final String property;

  Getter(Method method, String property) {
    this.returnType = method.getReturnType();
    this.property = property;
    try {
      this.mh = publicLookup().unreflect(method);
    } catch (IllegalAccessException e) {
      throw ExceptionMethods.uncheck(e);
    }
  }

  Getter(MethodHandle mh, String property, Class<?> returnType) {
    this.mh = mh;
    this.property = property;
    this.returnType = returnType;
  }

  /**
   * Returns the name of the property.
   *
   * @return the name of the property
   */
  public String getProperty() {
    return property;
  }

  /**
   * Returns the type of the property.
   *
   * @return the type of the property
   */
  public Class<?> getReturnType() {
    return returnType;
  }

  /**
   * Reads the value of the property off the specified bean
   *
   * @param bean The bean off which to read the property's value
   * @return the value of the property
   * @throws Throwable The unspecified {@code Throwable} associated with calls to
   * {@link MethodHandle#invoke(Object...) MethodHandle.invoke}.
   */
  public Object read(Object bean) throws Throwable {
    return mh.invoke(bean);
  }

}
