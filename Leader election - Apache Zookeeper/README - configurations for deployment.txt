In class "cz.muni.fi.zookeeper.leaderElection.ElectionCandidate" change the HOST and PORT according to the running instance of the Zookeeper server.

Then start the election candidate with:

mvn exec:java -Dexec.mainClass="cz.muni.fi.zookeeper.leaderElection.ElectionCandidate"