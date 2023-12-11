package org.klojang.invoke;

/**
 * A function that can be applied to transform a value just retrieved from a bean, or
 * about to be set on a bean. A {@code BeanValueTransformer} can be "inserted" into a
 * {@link BeanReader} and {@link BeanWriter} to modify the value that was read, or is
 * about to be written, respectively.
 *
 * @param <T> the type of the bean
 */
public interface BeanValueTransformer<T> {

  /**
   * The no-op transformer. Returns the property value as-is.
   *
   * @param <T> the type of the bean
   * @return the property value as-is
   */
  @SuppressWarnings("unchecked")
  static <T> BeanValueTransformer<T> identity() {
    return (BeanValueTransformer<T>) Private.IDENTIFY_TRANSFORMER;
  }

  /**
   * Transforms the value just retrieved from a bean, or to be set on a bean.
   *
   * @param bean the bean that was read or is about to be written
   * @param propertyName the property that was read or is about to be written
   * @param propertyValue the value to be transformed
   * @return the new value
   */
  Object transform(T bean, String propertyName, Object propertyValue);
}
