In the file "application.conf" in resources change the "hostname" to the address of the machine
the member is running on (e.g "127.0.0.1").

Then set "seed-nodes" with addresses of all nodes participating in the leader election.
(e.g ["akka.tcp://ClusterSystem@127.0.0.1:2551", "akka.tcp://ClusterSystem@127.0.0.1:2552"])
	  
Run one instance as:
/activator "runMain sample.cluster.simple.SimpleClusterApp 2551"

and another one:
/activator "runMain sample.cluster.simple.SimpleClusterApp 2552"