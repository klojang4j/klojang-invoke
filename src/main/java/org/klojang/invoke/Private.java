package org.klojang.invoke;

import org.klojang.util.InvokeException;

import static org.klojang.util.ClassMethods.simpleClassName;
import static org.klojang.util.ExceptionMethods.getRootCause;

final class Private {

  static final String INCLUDE_EXCLUDE = "includeExclude";
  static final String PROPERTIES = "properties";
  static final String BEAN_CLASS = "beanClass";
  static final String SOURCE_BEAN = "source bean";
  static final String TARGET_BEAN = "target bean";
  static final String SOURCE_MAP = "source map";
  static final String CONVERTER = "converter";

  static InvokeException wrap(Throwable t, Object bean, Getter getter) {
    String msg = String.format("Error while reading %s.%s: %s",
        simpleClassName(bean),
        getter.getProperty(),
        getRootCause(t));
    return new InvokeException(msg);
  }

}
