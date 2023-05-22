#!/bin/bash

BINARY_NAME=androidprojectgenerator.jar

has=$(which kscript)
if [ "$?" -ne "0" ]; then
  echo "no kscript, install it"
  # brew install kscripting/tap/kscript
  echo "with sdkman: sdk install kscript 3.0.2"
  exit 1
fi

version=$(kscript  --version 2>&1 | grep "Version" | sed 's/.*: //')
if [[ "$version" = v3* ]]
then
	echo "✅ kscript $version"
else
	echo "❌ kscript $version. Need v3"
  echo "with sdkman: sdk install kscript 3.0.2"
  echo "and maybe: sdk use kscript 3.0.2"
  
  exit 1
fi

has=$(which kotlin)
if [ "$?" -ne "0" ]; then
  echo "❌ no kotlin, install it."
  echo "with sdkman: sdk install kotlin"
  exit 1
fi
version=$(kotlin -version)
# TODO: check kotlin version for installed kscript?

if [ -z "$JAVA_HOME" ]; then
  if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
fi

has=$(which java)
if [ "$?" -ne "0" ]; then
  echo "❌ no java, install it"
  exit 2
fi
version=$(java -version)
echo "ℹ️ java 11 required"
# TODO: check java version for installed kscript?

version=$(gradle -v 2>&1 | grep "Gradle " | sed 's/.* //')

if [[ "$version" = "6.8"* ]]
then
	echo "✅ gradle $version"
else
	echo "❌ gradle $version. Need v6.8.3"
  echo "with sdkman: sdk install gradle 6.8.3"
  echo "or brew ..."
  
  #exit 1
fi

kscript --clear-cache
kscript  --package main.kt
if [ -f "main" ]; then
  mv main "$BINARY_NAME"
else
  echo "❌ no binary produced"
  exit 3
fi