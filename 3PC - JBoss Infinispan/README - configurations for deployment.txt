Infinispan uses multicasting so no configurations of a HOST are needed.

On a participant:

In class "cz.muni.fi.infinispan.threephasecommit.main.Main" change the variable "TRANSACTION_DECISION" for TransactionDecision.commit
if you want the participant to commit or TransactionDecision.abort for aborting.

In class cz.muni.fi.infinispan.threephasecommit.main.LockFileDemo change the FILE_PATH to the path of the file being locked.
This file will be created automatically so you don't have to create it on your own.


First start the participants on separate machines with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.infinispan.threephasecommit.main.Main" -Dexec.args="participant"

and then the coordinator:
mvn exec:java -Dexec.mainClass="cz.muni.fi.infinispan.threephasecommit.main.Main" -Dexec.args="coordinator"



