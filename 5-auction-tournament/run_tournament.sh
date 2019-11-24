rm -rf tournament/myTournament
rm -rf agents

mkdir agents

echo "Write space separated names of agents that will be enrolled in the tournament"
ls all_agents
read files
for file in $files; do
        cp all_agents/$file.jar agents/$file.jar
done        
echo "Starting tournament"
java -jar ../logist/logist.jar -new myTournament agents
java -jar ../logist/logist.jar -run myTournament config/auction.xml
java -jar ../logist/logist.jar -score myTournament
cat tournament/myTournament/results.txt

python plots.py tournament/myTournament/runner0.log
