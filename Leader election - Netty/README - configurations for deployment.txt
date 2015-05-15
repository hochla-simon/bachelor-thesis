On the participants:

In the class "cz.muni.fi.netty.leaderelection.electioncandidate.ElectionCandidate" specify the "HOST" of the server on which the Coordinator instance is running on.
You can either choose localhost (127.0.0.1) or the IP address of the remote machine.


On the coordinator:

In class cz.muni.fi.netty.leaderelection.main.Demo in variable SITES_COUNT specify the number of participants.


First start the coordinator with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.leaderelection.main.Demo" -Dexec.args="coordinator"

Then start the given number of participants on separate machines or in different consoles with:
mvn exec:java -Dexec.mainClass="cz.muni.fi.netty.leaderelection.main.Demo" -Dexec.args="participant"