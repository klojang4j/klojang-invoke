package org.klojang.invoke;

import org.klojang.util.InvokeException;

import static org.klojang.util.ClassMethods.simpleClassName;
import static org.klojang.util.ExceptionMethods.getRootCause;

final class Private {

  static InvokeException wrap(Throwable t, Object bean, Getter getter) {
    String msg = String.format("Error while reading %s.%s: %s",
        simpleClassName(bean),
        getter.getProperty(),
        getRootCause(t));
    return new InvokeException(msg);
  }

}
