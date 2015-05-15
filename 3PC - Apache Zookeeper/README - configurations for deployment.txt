On a participant:

In class "cz.muni.fi.zookeeper.threephasecommit.main.Main" change the variable "TRANSACTION_DECISION" for TransactionDecision.commit
if you want the participant to commit or TransactionDecision.abort for aborting.

Next, change the HOST and PORT according to the running instance of the Zookeeper server.

In class cz.muni.fi.zookeeper.threephasecommit.main.LockFileDemo change the FILE_PATH to the path of the file being locked.
This file will be created automatically so you don't have to create it on your own.



On the coordinator:

In class "cz.muni.fi.zookeeper.threephasecommit.main.Main" change the HOST and PORT according to the running instance of the Zookeeper server.

Set the number of the participanting sites in variable "SITES_COUNT".



First start the coordinator with:

mvn exec:java -Dexec.mainClass="cz.muni.fi.zookeeper.threephasecommit.main.Main" -Dexec.args="coordinator"


and then the participants on separate machines:

mvn exec:java -Dexec.mainClass="cz.muni.fi.zookeeper.threephasecommit.main.Main" -Dexec.args="participant"
