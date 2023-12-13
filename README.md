# Klojang Invoke

_Klojang Invoke_ is a Java module focused on path-based object access and dynamic
invocation. Its central classes are the
[Path](https://klojang4j.github.io/klojang-invoke/api/org.klojang.invoke/org/klojang/path/Path.html)
class and the
[PathWalker](https://klojang4j.github.io/klojang-invoke/api/org.klojang.invoke/org/klojang/path/PathWalker.html)
class. The
`Path` class captures a path through an object graph. For example
"employee.address.city". The `PathWalker` class lets you read from and write to
a wide variety of types using Path objects.

_Klojang Invoke_ is mainly intended as a supporting library for
[Klojang Templates](https://github.com/klojang4j/klojang-templates), but can be used and
useful separately from it.

## Getting Started

To use _Klojang Invoke_, add the following dependency to your Maven POM file:

```xml

<dependency>
    <groupId>org.klojang</groupId>
    <artifactId>klojang-invoke</artifactId>
    <version>2.0.2</version>
</dependency>
```

or Gradle build script:

```
implementation group: 'org.klojang', name: 'klojang-invoke', version: '2.0.2'
```

## Documentation

The **Javadocs** for _Klojang Invoke_ can be
found **[here](https://klojang4j.github.io/klojang-invoke/api)**.

The latest **test coverage report** can be
found **[here](https://klojang4j.github.io/klojang-invoke/coverage)**.

The latest **OWASP vulnerabilities report** can be
found 
**[here](https://klojang4j.github.io/klojang-invoke/vulnerabilities/dependency-check-report.html)**.
