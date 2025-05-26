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
   * Take note: this method returns the first __full__ path that caused a new entry to be created in the
   * parent node's children map. Say what? Take for example:
   *
   * person.address.street
   * person.address.city
   *
   * When creating a node for the person segment, that node will have the entire person.address.street as its
   * path (and segment index 0, which points to the person segment). When creating a node for the address
   * segment, that node will again have person.address.street as its path (and segment index 1, which points
   * to the address segment). This is simply because, when building the tree, person.address.street happened
   * to be processed before person.address.city. The address node will have two child nodes. One will again
   * have person.address.street as its path (but now with segment index 2), and the other person.address.city
   * (also with segment index 2). So the city node will have as its parent a node with path
   * person.address.street.
   *
   * Why does this not matter? Because this path will only be reported to the user if something went wrong
   * while reading the address segment. With the PathTreeWalker class you can't really say whether at that
   * point you were retrieving the value for street or for city. So we just pick one so that the error
   * reporting looks the same as with the PathWalker class. The reported error will be valid, although it
   * would also have been valid if we had picked person.address.city.
   *
   * Note that if this is a leaf node, the returned path no longer is an arbitrary path. It is guaranteed to
   * be the exact path to that node.
   */
  Path path() {
    return path;
  }

  @Override
  public String toString() {
    return "SegmentNode{path=" + path + ",index=" + index + "}";
  }

  private void addPath(Path path, int index) {
    if (index < path.size()) {
      SegmentNode node = children.computeIfAbsent(path.segment(index), _ -> new SegmentNode(path, index));
      node.addPath(path, index + 1);
    }
  }

}
