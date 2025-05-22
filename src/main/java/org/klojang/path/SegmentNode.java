package org.klojang.path;

import org.klojang.util.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.reverse;

final class SegmentNode {

  static SegmentNode buildTree(List<Path> paths) {
    SegmentNode root = new SegmentNode(null, null);
    paths.forEach(root::addPath);
    return root;
  }

  private final String segment;
  private final SegmentNode parent;
  private final Map<String, SegmentNode> children = new HashMap<>();

  SegmentNode(String segment, SegmentNode parent) {
    this.segment = segment;
    this.parent = parent;
  }

  String segment() {
    return segment;
  }

  boolean isRoot() {
    return parent == null;
  }

  boolean isLeaf() {
    return children.isEmpty();
  }

  Map<String, SegmentNode> children() {
    return children;
  }

  int segmentIndex() {
    int idx = 0;
    SegmentNode current = this;
    while (current.parent != null) {
      ++idx;
      current = current.parent;
    }
    return idx;
  }

  Path getFirstFullPath() {
    List<String> segments = new ArrayList<>();
    SegmentNode current = this;
    while (current.parent != null) {
      segments.add(current.segment);
      current = current.parent;
    }
    reverse(segments);
    current = this;
    while (!current.children.isEmpty()) {
      current = current.children.values().iterator().next();
      segments.add(current.segment);
    }
    return Path.ofSegments(segments);
  }

  Path toPath() {
    List<String> segments = new ArrayList<>();
    SegmentNode current = this;
    while (current.parent != null) {
      segments.add(current.segment);
      current = current.parent;
    }
    reverse(segments);
    return Path.ofSegments(segments);
  }

  @Override
  public String toString() {
    return toPath().toString();
  }

  private void addPath(Path path) {
    if (!path.isEmpty()) {
      children.computeIfAbsent(path.segment(0), k -> new SegmentNode(k, this)).addPath(path.shift());
    }
  }

}
