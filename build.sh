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

has=$(which java)
if [ "$?" -ne "0" ]; then
  echo "❌ no java, install it"
  exit 1
fi
version=$(java -version)

# TODO: check java version for installed kscript?

kscript --package main.kt
mv main $BINARY_NAME