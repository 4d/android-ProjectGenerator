#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"


cd "$DIR"
kscript --package main.kt
mv main androidprojectgenerator.jar

if [ -z "$PJGEN_PATH" ]
then
    if [ -z "$PERFORCE_PATH" ]
    then
        echo "[compile] no PERFORCE_PATH env var defined, try to copy to relative directory"
        mv -f androidprojectgenerator.jar "../../4DComponents/Internal User Components/4D Mobile App/Resources/scripts/"
    else
        echo "[compile] copy to $PERFORCE_PATH"
        mv -f androidprojectgenerator.jar "$PERFORCE_PATH/4eDimension/main/4DComponents/Internal User Components/4D Mobile App/Resources/scripts/"
    fi
else
    echo "[compile] copy to $PJGEN_PATH"
    mv -f androidprojectgenerator.jar "$PJGEN_PATH/"
fi

echo "success"
