In class "cz.muni.fi.zookeeper.lock.main.Main" change the HOST and PORT according to the running instance of the Zookeeper server.

In class cz.muni.fi.zookeeper.lock.main.LockFileDemo change the FILE_PATH to the path of the file to be locked.
This file will be created automatically so you don't have to create it on your own.

Then start the election candidate with:

mvn exec:java -Dexec.mainClass="cz.muni.fi.zookeeper.lock.main.Main"