/**
 * A module centered around path-based object access and dynamic invocation. Its
 * centerpiece is the {@link org.klojang.path.PathWalker} class, which can read from
 * and write to a wide varieties of types.
 */
module org.klojang.invoke {

  exports org.klojang.invoke;
  exports org.klojang.path;
  exports org.klojang.path.util;

  requires org.klojang.check;
  requires org.klojang.util;
  requires org.klojang.convert;

}
