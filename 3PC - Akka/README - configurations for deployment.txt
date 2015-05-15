On a participant:

In class "cz.muni.fi.akka.threephasecommit.main.Main" change the variable "TRANSACTION_DECISION" for TransactionDecision.commit
if you want the participant to commit or TransactionDecision.abort for aborting.

In class cz.muni.fi.akka.threephasecommit.main.LockFileDemo change the FILE_PATH to the path of the file being locked.
This file will be created automatically so you don't have to create it on your own.

In the file "common.conf" in resources change the "hostname" to the address of the machine
the participant is running on (e.g "147.251.53.90".


On the coordinator:

In class "cz.muni.fi.akka.threephasecommit.main.Main" set the variable "PARTICIPANT_NAMES" to an array of 'p's with indexes.
(Arrays.asList("p1", "p2"))

In the file "coordinator.conf" in resources set the participant paths according to "PARTICIPANT_NAMES" 
and assign the IP addresses of the machines they will be running on.

In the file "common.conf" in resources change the "hostname" to the address of the machine
the participant is running on (e.g "147.251.53.90".


First start the participants with:

/activator "run-main cz.muni.fi.akka.threephasecommit.main.Main participant"		

and then the coordinator:

/activator "run-main cz.muni.fi.akka.threephasecommit.main.Main coordinator"		