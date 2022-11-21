package org.klojang.path;

import org.klojang.invoke.BeanReader;
import org.klojang.invoke.NoPublicGettersException;
import org.klojang.invoke.NoSuchPropertyException;

import static org.klojang.util.ObjectMethods.isEmpty;
import static org.klojang.path.PathWalkerException.*;

@SuppressWarnings({"rawtypes", "unchecked"})
final class BeanSegmentReader extends SegmentReader<Object> {

  BeanSegmentReader(boolean suppressExceptions, KeyDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Object read(Object bean, Path path, int segment) {
    String property = path.segment(segment);
    if (isEmpty(property)) {
      return deadEnd(emptySegment(path, segment));
    }
    BeanReader reader;
    try {
      reader = new BeanReader(bean.getClass());
    } catch (NoPublicGettersException e) {
      return deadEnd(terminalValue(path, segment, bean.getClass()));
    }
    try {
      Object val = reader.read(bean, path.segment(segment));
      return new ObjectReader(se, kd).read(val, path, ++segment);
    } catch (NoSuchPropertyException e) {
      return deadEnd(noSuchProperty(path, segment, bean.getClass()));
    }
  }

}
