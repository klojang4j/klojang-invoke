package org.klojang.invoke;

import org.klojang.check.Check;
import org.klojang.check.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.klojang.check.CommonChecks.empty;
import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.invoke.IncludeExclude.INCLUDE;
import static org.klojang.invoke.NoSuchPropertyException.noSuchProperty;

/**
 * A dynamic bean writer class. This class uses method handles instead of reflection to
 * write bean properties. However, it still uses reflection to figure out what those
 * properties are in the first place. Therefore, if you use this class from within a Java
 * module you must open the module to the klojang-invoke module. Reflection is used only
 * transiently. No reflection objects are cached. They are disposed of once the required
 * information has been extracted from them.
 *
 * @param <T> The type of the bean
 * @author Ayco Holleman
 */
public final class BeanWriter<T> {

  private final Class<T> beanClass;
  private final BeanValueTransformer<T> transformer;
  private final Map<String, Setter> setters;

  /**
   * Creates a {@code BeanWriter} for the specified class. You can optionally specify an
   * array of properties that you intend to write. If you specify a zero-length array, all
   * properties will be writable. If you intend to use this {@code BeanWriter} to
   * repetitively write just one or two properties on bulky bean types, explicitly
   * specifying the properties you intend to write might make the {@code BeanWriter}
   * slightly more efficient.
   *
   * @param beanClass the bean class
   * @param properties the properties to be written
   */
  public BeanWriter(Class<T> beanClass, String... properties) {
    this(beanClass, INCLUDE, properties);
  }

  /**
   * Creates a {@code BeanWriter} for the specified class. You can optionally specify an
   * array of properties that you intend to write. If you specify a zero-length array all
   * properties will be writable. Input values will first be converted by the specified
   * conversion function before being assigned to properties.
   *
   * @param beanClass the bean class
   * @param transformer a conversion function for input values. The function is passed the
   * bean onto which to set the value; the property to set; and input value. Using these
   * three parameters, the function can compute a new value, which will be the value to
   * which to property is actually set.
   * @param properties the properties you allow to be written
   */
  public BeanWriter(
        Class<T> beanClass,
        BeanValueTransformer<T> transformer,
        String... properties) {
    this(beanClass, transformer, INCLUDE, properties);
  }

  /**
   * Creates a {@code BeanWriter} for the specified class. You can optionally specify an
   * array of properties that you intend or do <i>not</i> intend to write. If you specify
   * a zero-length array all properties will be writable.
   *
   * @param beanClass the bean class
   * @param includeExclude whether to include or exclude the specified properties
   * @param properties the properties to be included/excluded
   */
  public BeanWriter(
        Class<T> beanClass,
        IncludeExclude includeExclude,
        String... properties) {
    this.beanClass = Check.notNull(beanClass, Private.BEAN_CLASS).ok();
    this.transformer = null;
    Check.notNull(includeExclude, Private.INCLUDE_EXCLUDE);
    Check.notNull(properties, Private.PROPERTIES);
    this.setters = getSetters(includeExclude, properties);
  }

  /**
   * Creates a {@code BeanWriter} for the specified class. You can optionally specify an
   * array of properties that you intend or do <i>not</i> intend to write. If you specify
   * a zero-length array all properties will be writable. If you intend to use this
   * {@code BeanWriter} to repetitively write just one or two properties from bulky bean
   * types, explicitly specifying the properties you intend to write might make the
   * {@code BeanWriter} more efficient. Input values will first be converted by the
   * specified conversion function before being assigned to properties.
   *
   * <p><i>Specifying one or more non-existent properties will not cause an
   * exception to be thrown.</i> They will be quietly ignored.
   *
   * @param beanClass the bean class
   * @param transformer A conversion function for input values. The conversion is given
   * the {@link Setter} for the property to be set as the first argument, and the input
   * value as the second argument. The return value should be the actual value to assign
   * to the property. The {@code Setter} should only be used to get the
   * {@link Setter#getProperty() name} and {@link Setter#getParamType() type} of the
   * property to be set. You <i>should not</i> use it to actually
   * {@link Setter#write(Object, Object) write} the property, as this will happen anyhow
   * once the conversion function returns. Unless the conversion fails for extraordinary
   * reasons, it should throw an {@link IllegalAssignmentException} upon failure. You can
   * again use the {@code Setter} to {@link Setter#illegalAssignment(Object) generate} the
   * exception.
   * @param includeExclude whether to include or exclude the specified properties
   * @param properties the properties to be included/excluded
   */
  public BeanWriter(
        Class<T> beanClass,
        BeanValueTransformer<T> transformer,
        IncludeExclude includeExclude,
        String... properties) {
    this.beanClass = Check.notNull(beanClass, Private.BEAN_CLASS).ok();
    this.transformer = Check.notNull(transformer, Private.CONVERTER).ok();
    Check.notNull(includeExclude, Private.INCLUDE_EXCLUDE);
    Check.notNull(properties, Private.PROPERTIES);
    this.setters = getSetters(includeExclude, properties);
  }

  /**
   * Sets the specified property to the specified value. If this {@code BeanWriter} was
   * instantiated with a {@link BeanValueTransformer}, the property is set to the output
   * from the transformer.
   *
   * @param bean The bean instance
   * @param property The property
   * @param value The value to set it to
   * @throws IllegalAssignmentException If the value cannot be cast to the type of the
   * property, or if the value is {@code null} and the property has a primitive type. This
   * is a {@link RuntimeException}, but you might still want to catch it as it can often
   * be handled in a meaningful way.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void write(T bean, String property, Object value) throws Throwable {
    Check.notNull(bean, Tag.BEAN);
    Setter setter = Check.notNull(property, Tag.PROPERTY).ok(setters::get);
    Check.that(setter).is(notNull(), () -> noSuchProperty(bean, property));
    set(bean, setter, value);
  }

  /**
   * Overwrites all properties in the second bean with the values they have in the first
   * bean. This can potentially nullify non-null properties in the target bean.
   *
   * @param fromBean The bean from which to copy the values.
   * @param toBean The bean to which to copy the values.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void copy(T fromBean, T toBean) throws Throwable {
    Check.notNull(fromBean, Private.SOURCE_BEAN);
    Check.notNull(toBean, Private.TARGET_BEAN);
    BeanReader<T> reader = getBeanReader();
    for (Setter setter : setters.values()) {
      set(toBean, setter, reader.read(fromBean, setter.getProperty()));
    }
  }

  /**
   * Copies all non-null properties from the first bean to the second bean. This can
   * potentially overwrite non-null properties in the second bean, but it will never
   * nullify them.
   *
   * @param fromBean The bean from which to copy the values.
   * @param toBean The bean to which to copy the values.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void copyNonNull(T fromBean, T toBean) throws Throwable {
    Check.notNull(fromBean, Private.SOURCE_BEAN);
    Check.notNull(toBean, Private.TARGET_BEAN);
    BeanReader<T> reader = getBeanReader();
    for (Setter setter : setters.values()) {
      Object v = reader.read(fromBean, setter.getProperty());
      if (v != null) {
        set(toBean, setter, v);
      }
    }
  }

  /**
   * Overwrites all properties in the second bean whose value is {@code null} with the
   * values they have in the first bean. Non-null properties in the second bean are left
   * alone.
   *
   * @param fromBean The bean from which to copy the values.
   * @param toBean The bean to which to copy the values.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void enrich(T fromBean, T toBean) throws Throwable {
    Check.notNull(fromBean, Private.SOURCE_BEAN);
    Check.notNull(toBean, Private.TARGET_BEAN);
    BeanReader<T> reader = getBeanReader();
    for (Setter setter : setters.values()) {
      Object v = reader.read(fromBean, setter.getProperty());
      if (v != null && reader.read(toBean, setter.getProperty()) == null) {
        set(toBean, setter, v);
      }
    }
  }

  /**
   * Overwrites all properties in the specified bean with the corresponding values in the
   * specified map. This can potentially nullify non-null values in the target bean.
   *
   * @param fromMap The {@code Map} providing the data for the JavaBean
   * @param toBean The JavaBean to populate
   * @throws IllegalAssignmentException If a value cannot be cast or converted to the type
   * of the destination property, or if the value is {@code null} and the destination
   * property has a primitive type. This is a {@link RuntimeException}, but you might
   * still want to catch it as it can often be handled in a meaningful way.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void copy(Map<String, ?> fromMap, T toBean)
        throws IllegalAssignmentException, Throwable {
    Check.notNull(fromMap, Private.SOURCE_MAP);
    Check.notNull(toBean, Private.TARGET_BEAN);
    for (Map.Entry<String, ?> e : fromMap.entrySet()) {
      if (e.getKey() != null) {
        Setter setter = setters.get(e.getKey());
        if (setter != null) {
          set(toBean, setter, e.getValue());
        }
      }
    }
  }

  /**
   * Copies all non-null values from the specified map to the specified bean. Map keys
   * that do not correspond to bean properties are quietly ignored.
   *
   * @param fromMap The {@code Map} providing the data for the JavaBean
   * @param toBean The JavaBean to populate
   * @throws IllegalAssignmentException If a value cannot be cast or converted to the type
   * of the destination property, or if the value is {@code null} and the destination
   * property has a primitive type. This is a {@link RuntimeException}, but you might
   * still want to catch it as it can often be handled in a meaningful way.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void copyNonNull(Map<String, ?> fromMap, T toBean) throws Throwable {
    Check.notNull(fromMap, Private.SOURCE_MAP);
    Check.notNull(toBean, Private.TARGET_BEAN);
    for (Map.Entry<String, ?> e : fromMap.entrySet()) {
      if (e.getValue() != null && e.getKey() != null) {
        Setter setter = setters.get(e.getKey());
        if (setter != null) {
          set(toBean, setter, e.getValue());
        }
      }
    }
  }

  /**
   * Overwrites all properties in the specified bean whose value is {@code null} with the
   * corresponding values in the specified map. Non-null properties in the target bean are
   * left alone.
   *
   * @param fromMap The {@code Map} providing the data for the JavaBean
   * @param toBean The JavaBean to populate
   * @throws IllegalAssignmentException If a value cannot be cast or converted to the type
   * of the destination property, or if the value is {@code null} and the destination
   * property has a primitive type. This is a {@link RuntimeException}, but you might
   * still want to catch it as it can often be handled in a meaningful way.
   * @throws Throwable The {@code Throwable} thrown from inside the
   * {@code java.lang.invoke} package
   */
  public void enrich(Map<String, ?> fromMap, T toBean)
        throws IllegalAssignmentException, Throwable {
    Check.notNull(fromMap, Private.SOURCE_MAP);
    Check.notNull(toBean, Private.TARGET_BEAN);
    BeanReader<T> reader = getBeanReader();
    for (Map.Entry<String, ?> e : fromMap.entrySet()) {
      if (e.getValue() != null && e.getKey() != null) {
        Setter setter = setters.get(e.getKey());
        if (setter != null && reader.read(toBean, e.getKey()) == null) {
          set(toBean, setter, e.getValue());
        }
      }
    }
  }

  /**
   * Returns the type of the objects this {@code BeanWriter} can write to.
   *
   * @return The type of the objects this {@code BeanWriter} can write to
   */
  public Class<T> getBeanClass() {
    return beanClass;
  }

  /**
   * Returns {@code true} if the specified string represents a property that can be
   * written by this {@code BeanWriter}. Note that this check is already done by the
   * {@link #write(Object, String, Object)} method before it will actually attempt to set
   * the property. Only perform this check if there is a considerable chance that the
   * provided string is <i>not</i> a writable property.
   *
   * @param property the string to be tested
   * @return {@code true} if the specified string represents a property that can be
   * written by this {@code BeanWriter}
   */
  public boolean canWrite(String property) {
    return setters.containsKey(property);
  }

  /**
   * Returns the properties that this {@code BeanWriter} will write. That will be all
   * write-accessible properties minus the properties excluded through the constructor (if
   * any).
   *
   * @return the properties that this {@code BeanWriter} will write
   */
  public Set<String> getWritableProperties() {
    return setters.keySet();
  }

  /**
   * Returns the {@link Setter setters} used by the {@code BeanWriter} to write bean
   * properties. The returned {@code Map} maps the name of a property to the
   * {@code Setter} used to write it.
   *
   * @return The {@link Setter setters} used by the {@code BeanWriter} to write bean
   * properties.
   */
  public Map<String, Setter> getIncludedSetters() {
    return setters;
  }

  private Map<String, Setter> getSetters(IncludeExclude ie, String[] props) {
    Map<String, Setter> m = SetterFactory.INSTANCE.getSetters(beanClass);
    if (props.length == 0) {
      return m;
    }
    Map<String, Setter> tmp;
    if (ie.isExclude()) {
      Set<String> propSet = Set.of(props);
      tmp = LinkedHashMap.newLinkedHashMap(m.size() - props.length);
      m.forEach((x, y) -> { if (!propSet.contains(x)) tmp.put(x, y); });
    } else {
      tmp = LinkedHashMap.newLinkedHashMap(props.length);
      for (String prop : props) {
        if (m.containsKey(prop)) {
          tmp.put(prop, m.get(prop));
        }
      }
    }
    Check.that(tmp).isNot(empty(), () -> new NoPublicSettersException(beanClass));
    return tmp;
  }

  private void set(T bean, Setter setter, Object value) throws Throwable {
    if (transformer != null) {
      value = transformer.transform(bean, setter.getProperty(), value);
    }
    setter.write(bean, value);
  }

  private BeanReader<T> beanReader;

  private BeanReader<T> getBeanReader() {
    if (beanReader == null) {
      beanReader = new BeanReader<>(beanClass,
            setters.keySet().toArray(String[]::new));
    }
    return beanReader;
  }

}
