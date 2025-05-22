package org.klojang.path;

import org.klojang.check.extra.Result;
import org.klojang.invoke.BeanReader;
import org.klojang.invoke.NoPublicGettersException;
import org.klojang.invoke.NoSuchPropertyException;
import org.klojang.util.Path;

import static org.klojang.path.DeadEndException.*;
import static org.klojang.util.ObjectMethods.isEmpty;

@SuppressWarnings({"rawtypes", "unchecked"})
final class BeanSegmentReader extends SegmentReader<Object> {

  BeanSegmentReader(boolean suppressExceptions, PathSegmentDeserializer keyDeserializer) {
    super(suppressExceptions, keyDeserializer);
  }

  @Override
  Result<Object> read(Object bean, SegmentNode node) {
    String property = node.segment();
    if (isEmpty(property)) {
      return deadEnd(emptySegment(node.getFirstFullPath(), node.segmentIndex()));
    }
    BeanReader reader;
    try {
      reader = new BeanReader(bean.getClass());
    } catch (NoPublicGettersException e) {
      return deadEnd(terminalValue(node.getFirstFullPath(), node.segmentIndex(), bean.getClass()));
    }
    try {
      return Result.of(reader.read(bean, property));
    } catch (NoSuchPropertyException e) {
      return deadEnd(noSuchProperty(node.getFirstFullPath(), node.segmentIndex(), bean.getClass()));
    }
  }

  @Override
  Result<Object> read(Object bean, Path path, int segment) {
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
      Object val = reader.read(bean, property);
      return new ObjectReader(se, kd).read(val, path, ++segment);
    } catch (NoSuchPropertyException e) {
      return deadEnd(noSuchProperty(path, segment, bean.getClass()));
    }
  }

}
