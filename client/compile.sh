#!/bin/bash

set -e

echo "Cleaning"

rm -rf bin textglass-reference.jar

if [ "$1" = "clean" ]
then
    rm -rf lib
    exit 0
fi

if [ ! -d lib ]
then
    echo "Downloading libraries"

    mkdir lib

    wget -nv -O "lib/jackson-core-asl-1.9.13.jar" "http://central.maven.org/maven2/org/codehaus/jackson/jackson-core-asl/1.9.13/jackson-core-asl-1.9.13.jar"
    wget -nv -O "lib/jackson-mapper-asl-1.9.13.jar" "http://central.maven.org/maven2/org/codehaus/jackson/jackson-mapper-asl/1.9.13/jackson-mapper-asl-1.9.13.jar"
fi

echo "Compiling"

LIBS=`find lib -type f | xargs echo | sed "s/ /:/g"`

mkdir bin

javac -cp "$LIBS" src/*.java -d bin

if [ "$1" = "dist" ]
then
    echo "Creating distribution"
    jar cf textglass-reference.jar -C bin .
fi
