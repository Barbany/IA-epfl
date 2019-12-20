     
echo "Starting tournament"
java -jar ../logist/logist.jar -new myTournament agents
java -jar ../logist/logist.jar -run myTournament config/auction.xml
java -jar ../logist/logist.jar -score myTournament
cat tournament/myTournament/results.txt
