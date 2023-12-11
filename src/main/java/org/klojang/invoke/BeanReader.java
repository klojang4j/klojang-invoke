package org.klojang.invoke;

import org.klojang.check.Check;
import org.klojang.check.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
   * Returns a {@code Builder} for {@code BeanReader} instances. Note that the specified
   * type may just as well be a {@code record} or {@code enum} type.
   *
   * @param beanClass the class for which to create a {@code BeanReader}
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
   * Creates a {@code BeanReader} for the specified properties of the specified class. You
   * can optionally specify an array of properties that you intend to read. If you specify
   * a zero-length array all properties will be readable. JavaBeans naming conventions
   * will be applied regarding which methods qualify as getters.
   *
   * @param beanClass the bean class
   * @param properties the properties to be included/excluded
   */
  public BeanReader(Class<T> beanClass, String... properties) {
    this(beanClass, true, INCLUDE, properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified properties of the specified class. You
   * can optionally specify an array of properties that you intend to read. If you specify
   * a zero-length array, all properties will be readable. JavaBeans naming conventions
   * will be applied regarding which methods qualify as getters.
   *
   * @param beanClass the bean class
   * @param includeExclude whether to include or exclude the specified properties
   * @param properties the properties to be included/excluded
   */
  public BeanReader(Class<T> beanClass,
        IncludeExclude includeExclude,
        String... properties) {
    this(beanClass, true, includeExclude, properties);
  }

  /**
   * Creates a {@code BeanReader} for the specified properties of the specified class. You
   * can optionally specify an array of properties that you intend to read. If you specify
   * a zero-length array, all properties will be readable. If you intend to use this
   * {@code BeanReader} to repetitively read just one or two properties from bulky bean
   * types, explicitly specifying the properties you intend to read might make the
   * {@code BeanReader} slightly more efficient.
   *
   * <p>Specifying non-existent properties will not cause an exception to be thrown.
   * They will be tacitly ignored.
   *
   * @param beanClass the bean class
   * @param strictNaming if {@code false}, all methods with a zero-length parameter list
   * and a non-{@code void} return type, except {@code getClass()}, {@code hashCode()} and
   * {@code toString()}, will be regarded as getters. Otherwise JavaBeans naming
   * conventions will be applied regarding which methods qualify as getters. By way of
   * exception, methods returning a {@link Boolean} are allowed to have a name starting
   * with "is" (just like methods returning a {@code boolean}). The {@code strictNaming}
   * parameter is quietly ignored for {@code record} classes. Records are always processed
   * as though {@code strictNaming} were {@code false}.
   * @param includeExclude whether to include or exclude the subsequently specified
   * properties
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
   * Creates a {@code BeanReader} for the specified properties of the specified class. You
   * can optionally specify an array of properties that you intend to read. If you specify
   * a zero-length array, all properties will be readable. If you intend to use this
   * {@code BeanReader} to repetitively read just one or two properties from bulky bean
   * types, explicitly specifying the properties you intend to read might make the
   * {@code BeanReader} slightly more efficient.
   *
   * <p>Specifying non-existent properties will not cause an exception to be thrown.
   * They will be tacitly ignored.
   *
   * @param beanClass the bean class
   * @param strictNaming if {@code false}, all methods with a zero-length parameter list
   * and a non-{@code void} return type, except {@code getClass()}, {@code hashCode()} and
   * {@code toString()}, will be regarded as getters. Otherwise JavaBeans naming
   * conventions will be applied regarding which methods qualify as getters. By way of
   * exception, methods returning a {@link Boolean} are allowed to have a name starting
   * with "is" (just like methods returning a {@code boolean}). The {@code strictNaming}
   * parameter is quietly ignored for {@code record} classes. Records are always processed
   * as though {@code strictNaming} were {@code false}.
   * @param transformer a conversion function for bean values. The function is passed the
   * bean from which the value was retrieved; the property that was read; and the value of
   * the property. Using these three parameters, the function can compute a new value,
   * which will be the value that is actually returned
   * @param includeExclude whether to include or exclude the subsequently specified
   * properties
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
    Object val;
    try {
      val = getter.read(bean);
    } catch (Throwable exc) {
      throw Private.wrap(exc, bean, getter);
    }
    if (transformer != null) {
      val = transformer.transform(bean, property, val);
    }
    return (U) val;
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
   * by this {@code BeanReader}
   * @see #getReadableProperties()
   */
  public boolean canRead(String property) {
    return getters.containsKey(property);
  }

  /**
   * Returns the properties that this {@code BeanReader} will read. That will be all
   * read-accessible properties minus the properties excluded through the constructor (if
   * any).
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

  private Map<String, Getter> getGetters(boolean strict,
        IncludeExclude ie,
        String[] props) {
    Map<String, Getter> m = GetterFactory.INSTANCE.getGetters(beanClass, strict);
    if (props.length == 0) {
      return m;
    }
    Map<String, Getter> tmp;
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
    Check.that(tmp).isNot(empty(), () -> new NoPublicGettersException(beanClass));
    return tmp;
  }

}
