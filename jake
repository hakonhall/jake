#!/bin/bash

if test -z "$BASH_VERSION" -o "${BASH_VERSINFO[0]}" -lt 3; then
    echo "Requires bash 3+"
    exit 1
fi

set -e

if ! type dirname &> /dev/null; then
    echo "'dirname' not installed"
    exit 1
fi

# Whether this bash script is sourced or executed as a program, BASH_SOURCE[0]
# holds the absolute path to this file.  Fantastic!  However, that path may be
# a symlink to the actual file (or a symlink to a symlink, and so on), so we
# need to resolve symlinks.
jakepath="${BASH_SOURCE[0]}"

while test -L "$jakepath"; do
    if type realpath &> /dev/null; then
        jakepath=$(realpath "$jakepath")
    elif type readlink &> /dev/null; then
        link=$(readlink "$jakepath")
        if test "${link:0:1}" == /; then
            jakepath="$link"
        else
            jakepath="$(dirname "$jakepath")/$link"
        fi
    else
        cat <<EOF
Failed to find the real location of this script ($jakepath):
It is a symlink, but with neither 'realpath' nor 'readlink' installed the
symlink cannot be read.
EOF
        exit 1
    fi
done

if ! test -f "$jakepath"; then
    echo "BASH_SOURCE[0] points to a non-existing file: '$jakepath'"
    exit 1
fi

dir=$(dirname "$jakepath")
if ! test -d "$dir"/lib || ! test -d "$dir"/src; then
    echo "Does not appear to be a jake directory: '$dir'"
    exit 1
fi

jar="$dir"/jar/no.ion.jake-0.0.1.jar
if ! test -f "$jar"; then
    echo "Run 'make' in '$dir'? jake jar not found:"$'\n'"$jar"
    exit 1
fi

if ! type java &> /dev/null; then
    echo "'java' not installed"
    exit 1
fi

# -Djdk.module.illegalAccess.silent=true

exec java \
     --illegal-access=permit \
     -cp "$jar":\
"$dir"/lib/bundle-plugin.jar:\
"$dir"/lib/abi-check-plugin.jar:\
"$dir"/lib/gson-2.8.5.jar:\
"$dir"/lib/guava-20.0.jar:\
"$dir"/lib/asm-7.0.jar:\
"$dir"/lib/annotations.jar:\
"$dir"/lib/jackson-databind-2.8.11.6.jar:\
"$dir"/lib/jackson-core-2.8.11.jar:\
"$dir"/lib/jackson-annotations-2.8.11.jar \
     no.ion.jake.vespa.Main "$@"
