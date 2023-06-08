/**
 * <i>Klojang Invoke</i> is a module focused on path-based object access and dynamic
 * invocation. Its central classes are the {@link org.klojang.path.Path Path} class and
 * the {@link org.klojang.path.PathWalker PathWalker} class. The {@code Path} class
 * captures a path through an object graph. For example {@code employee.address.city}.
 * The {@code PathWalker} class lets you read from and write to a wide variety of types
 * using {@code path} objects.
 */
module org.klojang.invoke {

  exports org.klojang.invoke;
  exports org.klojang.path;
  exports org.klojang.path.util;

  requires org.klojang.check;
  requires org.klojang.util;
  requires org.klojang.convert;

}
