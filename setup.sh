#!/bin/sh

source "$HOME/.sdkman/bin/sdkman-init.sh"

has=$(which sdk)
status=$? # seem to not working for sdk in script

if [ "$status" -ne "0" ]; then
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

sdk version

has=$(which kscript)
if [ "$?" -ne "0" ]; then
  sdk install kscript 3.0.2
fi

version=$(kscript  --version 2>&1 | grep "Version" | sed 's/.*: //')
if [[ "$version" = v3* ]]
then
	echo "✅ kscript $version"
else
	echo "❌ kscript $version. Need v3"
  sdk install kscript 3.0.2
  sdk use kscript 3.0.2
fi

java=$(sdk list java | grep 11.0 | grep "open" | sed 's/.*| //')
echo "java $java"
sdk install java $java
sdk use java $java
