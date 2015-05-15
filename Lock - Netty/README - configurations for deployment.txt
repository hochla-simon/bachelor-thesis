On the participants:

In class "cz.muni.fi.netty.lock.participant.Participant" specify the "HOST" of the server
on which the Coordinator instance is running on. You can either choose localhost (127.0.0.1)
or the IP address of the remote machine.

In class "cz.muni.fi.netty.lock.main.LockFileDemo" in variable "FILE_PATH" provide the path to the file being locked.
The file will be created automatically so you don't have to create it on your own.


On the coordinator:

In class "cz.muni.fi.netty.lock.main.Main" in variable SITES_COUNT specify the count of participants.  


First start the coordinator with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.lock.main.Main" -Dexec.args="coordinator"

Then start the given number of participant instances on separate machines with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.lock.main.Main" -Dexec.args="participant"