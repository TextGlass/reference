TextGlass Reference Client
==========================

What is this?
-------------

This is the reference client for the TextGlass project.

This reference client is the starting point for:

 * Creating, testing, and validating a TextGlass domain.
 * Creating a TextGlass client for a specific platform or language.


Specification
-------------

https://github.com/TextGlass/reference/wiki/Domain-Specification


Source
------

https://github.com/TextGlass/reference/tree/master/client/src

The source is broken up into several components:

 * src/Main.java

   This is the driver for the reference client. It contains logic to parse
   the command line and the test file.

 * src/TextGlassClient.java

   This is the actual TextGlass client. It is initialized with a pattern file
   and an attribute file (or multiple). Once initialized, it can then classify
   text and return back attribute maps.

 * src/JsonFile.java

   This parses a JSON file into something the TextGlassClient can handle.

 * src/Pattern.java

   This is a pattern object.

 * src/Attributes.java

   This is the pattern attributes object.

 * src/Transformer\*.java

   These are transformers.


Compile
-------

Note: this step requires "javac" (JDK)

```
./compile.sh
```

To clean:

```
./compile.sh clean
```


Run
---

Note: this step requires "java" (JRE)

```
./run.sh -p [pattern file] -a [attribute file] -t [test file]
```

For help:

```
./run.sh -h
```

To run the reference_a domain:

```
./run.sh \
  -p ../domains/a/patterns.json \
  -a ../domains/a/attributes.json \
  -t ../domains/a/test.json
```

You can also use the test harness:

```
./test.sh ../domains/a
```

