On a participant:

In the class "cz.muni.fi.netty.threephasecommit.participant.Participant" specify the "HOST" of the server on which the Coordinator instance is running on.
You can either choose localhost (127.0.0.1) or the IP address of a remote machine.

In class "cz.muni.fi.netty.threephasecommit.main.Main" change the variable "TRANSACTION_DECISION" for TransactionDecision.commit
if you want the participant to commit or TransactionDecision.abort for aborting.

In class cz.muni.fi.netty.threephasecommit.main.LockFileDemo change the FILE_PATH to the path of the file being locked.
This file will be created automatically so you don't have to create it on your own.


On the coordinator:

In the class "cz.muni.fi.netty.threephasecommit.main.Main" set the number 
of the participanting sites in variable "SITES_COUNT".


First start the coordinator with:

mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.threephasecommit.main.Main" -Dexec.args="coordinator"

and then the participants on separate machines:

mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.threephasecommit.main.Main" -Dexec.args="participant"
