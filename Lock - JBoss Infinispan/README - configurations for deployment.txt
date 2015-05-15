Infinispan uses multicasting so no configurations of a HOST are needed.

In class "cz.muni.fi.infinispan.lock.Lock" in variable "FILE_PATH" provide the path to the file being locked.
the file will be created automatically so you don't have to create it on your own.

Start the lock instance on separate machines or in different consoles with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.infinispan.lock.Lock"