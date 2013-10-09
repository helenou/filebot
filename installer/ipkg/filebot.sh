#!/bin/sh
java -Dunixfs=false -DuseExtendedFileAttributes=false -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Dapplication.deployment=ipkg -Dapplication.analytics=true -Duser.home=/opt/usr/share/filebot/data -Dapplication.dir=/opt/usr/share/filebot/data -Djava.io.tmpdir=/opt/usr/share/filebot/data/temp -Djna.library.path=/opt/usr/share/filebot -Djava.library.path=/opt/usr/share/filebot -Dnet.sourceforge.filebot.AcoustID.fpcalc=fpcalc -jar /opt/usr/share/filebot/FileBot.jar "$@"