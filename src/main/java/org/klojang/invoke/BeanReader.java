package org.klojang.invoke;

import org.klojang.check.Check;
import org.klojang.check.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Map.Entry;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.invoke.IncludeExclude.INCLUDE;
import static org.klojang.invoke.NoSuchPropertyException.noSuchProperty;

/**
 * A dynamic bean reader class. This class uses method handles instead of reflection to
 * read bean properties. However, it still uses reflection to figure out what those
 * properties are in the first place. Therefore, if you use a {@code BeanReader} to read
 * beans residing in a Java 9+ module, that module must be "open" to reflective access.
 * Reflection is used only transiently. No reflection objects are cached. They are
 * disposed of once the required information has been extracted from them.
 *
 * <p>If you prefer, you can use the {@link BeanReaderBuilder} class to configure
 * {@link BeanReader} instances in a completely reflection-free manner. You obtain a
 * {@code BeanReaderBuilder} via {@link #forClass(Class) BeanReader.forClass()}.
 *
 * @param <T> The type of the bean
 * @author Ayco Holleman
 */
public final class BeanReader<T> {

  /**
   * Returns a {@code Builder} for {@code BeanReader} instances. The builder will produce
   * a 100% reflection-free {@code BeanReader}, which you may find desirable when writing
   * Java 9+ modules.
   *
   * @param beanClass the class for which to create a {@code BeanReader} (may be a
   *       {@code record} type)
   * @param <T> the type of the objects to be read
   * @return a {@code Builder} for {@code BeanReader} instances
   */
  public static <T> BeanReaderBuilder<T> forClass(Class<T> beanClass) {
    return new BeanReaderBuilder<>(beanClass);
  }

  private final Class<T> beanClass;
  private final Map<String, Getter> getters;
  private final BeanValueTransformer<T> transformer;

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class (may be a {@code record} type)
   * @param properties the properties to be included/excluded
   * @see #getReadableProperties()
   */
  public BeanReader(Class<T> beanClass, String... properties) {
    this(beanClass, BeanValueTransformer.identity(), properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties to be <i>included</i> is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class (may be a {@code record} type)
   * @param includeExclude whether to include or exclude the specified properties
   * @param properties the properties to be included/excluded
   * @see #getReadableProperties()
   * @see #readAllProperties(Object)
   */
  public BeanReader(Class<T> beanClass,
        IncludeExclude includeExclude,
        String... properties) {
    this(beanClass, BeanValueTransformer.identity(), includeExclude, properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties to be <i>included</i> is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class (may be a {@code record} type)
   * @param strictNaming if {@code false}, all methods with a zero-length parameter
   *       list and a non-{@code void} return type, except {@code getClass()},
   *       {@code hashCode()} and {@code toString()}, will be regarded as getters.
   *       Otherwise JavaBeans naming conventions will be applied regarding which methods
   *       qualify as getters. By way of exception, methods returning a {@link Boolean}
   *       are allowed to have a name starting with "is" (just like methods returning a
   *       {@code boolean}). The {@code strictNaming} parameter is tacitly ignored for
   *       {@code record} classes. Records are always processed as though
   *       {@code strictNaming} were {@code false}.
   * @param includeExclude whether to include or exclude the subsequently specified
   *       properties
   * @param properties the properties to be included/excluded
   */
  public BeanReader(Class<T> beanClass,
        boolean strictNaming,
        IncludeExclude includeExclude,
        String... properties) {
    this(beanClass,
          strictNaming,
          BeanValueTransformer.identity(),
          includeExclude,
          properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class (may be a {@code record} type)
   * @param transformer a conversion function for bean values. The function is
   *       passed the bean from which the value was retrieved, the property that was read,
   *       and the value of the property. Using these three parameters, the function can
   *       compute a new value, which will be the value that is <i>actually</i> returned
   *       from {@link #read(Object, String) BeanReader.read()}.
   * @param properties the properties to be included
   */
  public BeanReader(Class<T> beanClass,
        BeanValueTransformer<T> transformer,
        String... properties) {
    this(beanClass, transformer, INCLUDE, properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties to be <i>included</i> is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class (may be a {@code record} type)
   * @param transformer a conversion function for bean values. The function is
   *       passed the bean from which the value was retrieved, the property that was read,
   *       and the value of the property. Using these three parameters, the function can
   *       compute a new value, which will be the value that is <i>actually</i> returned
   *       from {@link #read(Object, String) BeanReader.read()}.
   * @param includeExclude whether to include or exclude the subsequently specified
   *       properties
   * @param properties the properties to be included
   */
  public BeanReader(Class<T> beanClass,
        BeanValueTransformer<T> transformer,
        IncludeExclude includeExclude,
        String... properties) {
    this(beanClass, true, transformer, includeExclude, properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified class. You can optionally specify an
   * array of properties that you intend to read. If you specify a zero-length array, all
   * properties will be readable. If you intend to use this {@code BeanReader} to
   * repetitively read just one or two properties from bulky bean types, explicitly
   * specifying the properties you intend to read might make the {@code BeanReader}
   * slightly more efficient. <i>It is not an error to specify non-existent
   * properties.</i> They will be tacitly ignored. A side effect of specifying one or more
   * properties to be <i>included</i> is that it forces the values returned from
   * {@link #readAllProperties(Object) readAllProperties()} to be in the order in which
   * you specify the properties.
   *
   * @param beanClass the bean class
   * @param strictNaming if {@code false}, all methods with a zero-length parameter
   *       list and a non-{@code void} return type, except {@code getClass()},
   *       {@code hashCode()} and {@code toString()}, will be regarded as getters.
   *       Otherwise JavaBeans naming conventions will be applied regarding which methods
   *       qualify as getters. By way of exception, methods returning a {@link Boolean}
   *       are allowed to have a name starting with "is" (just like methods returning a
   *       {@code boolean}). The {@code strictNaming} parameter is tacitly ignored for
   *       {@code record} classes. Records are always processed as though
   *       {@code strictNaming} were {@code false}.
   * @param transformer a conversion function for bean values. The function is
   *       passed the bean from which the value was retrieved, the property that was read,
   *       and the value of the property. Using these three parameters, the function can
   *       compute a new value, which will be the value that is <i>actually</i> returned
   *       from {@link #read(Object, String) BeanReader.read()}.
   * @param includeExclude whether to include or exclude the subsequently specified
   *       properties
   * @param properties the properties to be included/excluded
   */
  public BeanReader(Class<T> beanClass,
        boolean strictNaming,
        BeanValueTransformer<T> transformer,
        IncludeExclude includeExclude,
        String... properties) {
    Check.notNull(beanClass, Private.BEAN_CLASS);
    Check.notNull(transformer, Private.TRANSFORMER);
    Check.notNull(includeExclude, Private.INCLUDE_EXCLUDE);
    Check.that(properties, Private.PROPERTIES).is(deepNotNull());
    this.beanClass = beanClass;
    this.transformer = transformer;
    this.getters = getGetters(strictNaming, includeExclude, properties);
  }

  BeanReader(Class<T> beanClass,
        Map<String, Getter> getters,
        BeanValueTransformer<T> transformer) {
    this.beanClass = beanClass;
    this.getters = getters;
    this.transformer = transformer;
  }

  /**
   * Returns the value of the specified property on the specified bean. If the property
   * does not exist a {@link NoSuchPropertyException} is thrown. If this
   * {@code BeanReader} was instantiated with a {@link BeanValueTransformer}, the output
   * from the transformer is returned.
   *
   * @param bean the bean instance
   * @param property The property
   * @param <U> the type of the return value
   * @return its value
   * @throws NoSuchPropertyException if the specified property does not exist
   */
  @SuppressWarnings("unchecked")
  public <U> U read(T bean, String property) throws NoSuchPropertyException {
    Check.notNull(bean, Tag.BEAN);
    Check.notNull(property, Tag.PROPERTY);
    Getter getter = getters.get(property);
    Check.that(getter).is(notNull(), () -> noSuchProperty(bean, property));
    return (U) read(bean, getter);
  }

  /**
   * Returns the values of all readable properties in the specified JavaBean. The values
   * will be returned in the same order as the property names returned by
   * {@link #getReadableProperties()}.
   *
   * @param bean the bean from which to read the values
   * @return the values of all readable properties in the specified JavaBean
   */
  public List<Object> readAllProperties(T bean) {
    List<Object> values = new ArrayList<>(getters.size());
    getters.forEach((k, v) -> values.add(read(bean, v)));
    return values;
  }

  /**
   * Returns the class of the objects this {@code BeanReader} can read.
   *
   * @return the class of the objects {@code BeanReader} can read
   */
  public Class<T> getBeanClass() {
    return beanClass;
  }

  /**
   * Returns {@code true} if the specified string represents a property that can be read
   * by this {@code BeanReader}. Note that this check is already done by the
   * {@link #read(Object, String) read} method before it will actually attempt to read
   * from the provided bean. Only perform this check if there is a considerable chance
   * that the provided string is <i>not</i> a readable property.
   *
   * @param property The string to be tested
   * @return {@code true} if the specified string represents a property that can be read
   *       by this {@code BeanReader}
   * @see #getReadableProperties()
   */
  public boolean canRead(String property) {
    return getters.containsKey(property);
  }

  /**
   * Returns the properties that this {@code BeanReader} can read. If one or more
   * properties were excluded through the constructor, they will not be contained in the
   * returned {@code Set}. If no properties were excluded, or they were excluded via
   * {@link IncludeExclude#EXCLUDE}, the properties will be returned in arbitrary order.
   * If you specified one or more properties to be <i>included</i> (via
   * {@link IncludeExclude#INCLUDE}), the properties will be returned in the order in
   * which you specified those properties.
   *
   * @return the properties that this {@code BeanReader} will read
   */
  public Set<String> getReadableProperties() {
    return getters.keySet();
  }

  /**
   * Returns the {@link Getter getters} used by the {@code BeanReader} to read bean
   * properties. The returned {@code Map} maps the name of a property to the
   * {@code Getter} used to read it.
   *
   * @return all getters used to read bean properties.
   */
  public Map<String, Getter> getIncludedGetters() {
    return getters;
  }

  private Object read(T bean, Getter getter) {
    Object val;
    try {
      val = getter.read(bean);
    } catch (Throwable exc) {
      throw Private.wrap(exc, bean, getter);
    }
    if (transformer != null) {
      val = transformer.transform(bean, getter.getProperty(), val);
    }
    return val;
  }


  private Map<String, Getter> getGetters(boolean strict,
        IncludeExclude ie,
        String[] properties) {
    Map<String, Getter> getters = GetterFactory.INSTANCE.getGetters(beanClass, strict);
    if (properties.length == 0) {
      return getters;
    }
    Set<String> props = Set.of(properties);
    Predicate<Entry<String, Getter>> filter = ie.isExclude()
          ? e -> !props.contains(e.getKey())
          : e -> props.contains(e.getKey());
    Entry[] entries = getters.entrySet().stream().filter(filter).toArray(Entry[]::new);
    Check.that(entries).isNot(empty(), () -> new NoPublicGettersException(beanClass));
    return Map.ofEntries(entries);
  }

}
