package org.klojang.invoke;

/**
 * A {@code BeanValueTransformer} can be "inserted" into a {@link BeanReader} and
 * {@link BeanWriter} to modify the value to be returned to the user by the
 * {@code BeanReader}, or to be written to the bean by the {@code BeanWriter}.
 *
 * @param <T> the type of the bean
 */
@FunctionalInterface
public interface BeanValueTransformer<T> {

  /**
   * The no-op transformer. Returns the property value as-is.
   *
   * @param <T> the type of the bean
   * @return the property value as-is
   */
  static <T> BeanValueTransformer<T> identity() { return (x, y, z) -> z; }

  /**
   * Transforms the value just retrieved from a bean, or about to be set on a bean.
   *
   * @param bean the bean that was read or is about to be written
   * @param propertyName the name of the property that was read or is about to be
   *       written
   * @param propertyValue the original value
   * @return the new value
   */
  Object transform(T bean, String propertyName, Object propertyValue);
}
