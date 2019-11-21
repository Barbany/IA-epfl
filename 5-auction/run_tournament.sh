rm -rf tournament/myTournament
echo "Starting tournament"
java -jar ../logist/logist.jar -new myTournament agents
java -jar ../logist/logist.jar -run myTournament config/auction.xml
java -jar ../logist/logist.jar -score myTournament
less tournament/myTournament/results.txt
