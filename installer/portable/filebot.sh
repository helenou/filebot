#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
dir_bin="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# force JVM language and encoding settings
export LANG=en_US.utf8

java -Xmx256m -Dunixfs=false -DuseExtendedFileAttributes=false -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Dapplication.deployment=portable "-Dapplication.dir=$dir_bin" "-Djava.io.tmpdir=$dir_bin/temp" "-Duser.home=$dir_bin" "-Djna.library.path=$dir_bin" "-Djava.library.path=$dir_bin" -Djava.util.prefs.PreferencesFactory=net.sourceforge.tuned.prefs.FilePreferencesFactory "-Dnet.sourceforge.tuned.prefs.file=$dir_bin/prefs.properties" -jar "$dir_bin/FileBot.jar" "$@"