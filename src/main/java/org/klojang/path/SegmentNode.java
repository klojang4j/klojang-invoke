package org.klojang.path;

import org.klojang.util.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SegmentNode {

  static SegmentNode buildTree(List<Path> paths) {
    SegmentNode root = new SegmentNode(null, -1);
    paths.forEach(p -> root.addPath(p, 0));
    return root;
  }


  private final Path path;
  private final int index;
  private final Map<String, SegmentNode> children;

  SegmentNode(Path path, int index) {
    this.path = path;
    this.index = index;
    this.children = new HashMap<>();
  }

  String segment() {
    return path.segment(index);
  }

  /*
   * We don't mean here the first segment of any of the paths passed to PathTreeWalker, but the "virtual" node
   * created by buildTree()
   */
  boolean isRootNode() {
    return path == null;
  }

  boolean isLeaf() {
    return index == path.size() - 1;
  }

  Map<String, SegmentNode> children() {
    return children;
  }

  int segmentIndex() {
    return index;
  }

  /*
   * Returns the first path that caused a new entry to be created in the parent node's children map. Take for
   * example:
   *
   * person.address.street
   * person.address.city
   *
   * When creating a node for the address segment, that node will have "person.address.street" as its path
   * (and segment index 1, which points to the "address" segment). That's simply because, when building the
   * tree, "person.address.street" was encountered before "person.address.city". The address node will have
   * two child nodes. One will again have "person.address.street" as its path (and segment index 2), and the
   * other "person.address.city" (also with segment index 2). So the "person.address.city" node will have as
   * its parent a node with path "person.address.street".
   *
   * Why does this not matter? Because this method will only be called if something went wrong while reading
   * the address segment. With the PathTreeWalker class you can't really say whether you were retrieving the
   * value for "person.address.street" or for "person.address.city" when something already went wrong while
   * reading the preceding segments. So we just pick one so that the error reporting looks the same as with
   * the PathWalker class.
   */
  Path getArbitraryFullPath() {
    return path;
  }

  Path toPath() {
    if (segmentIndex() == path.size() - 1) {
      return path;
    }
    return path.subPath(0, index + 1);
  }

  @Override
  public String toString() {
    return toPath().toString();
  }

  private void addPath(Path path, int index) {
    if (index < path.size()) {
      SegmentNode node = children.computeIfAbsent(path.segment(index), _ -> new SegmentNode(path, index));
      node.addPath(path, index + 1);
    }
  }

}
