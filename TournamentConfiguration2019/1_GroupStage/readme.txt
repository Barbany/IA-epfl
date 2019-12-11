
Put the jar file of your agent in ./agents

Create a tournament:

java -jar './logist/logist.jar' -new 'tour' './agents'

Run the tournament:

java -jar './logist/logist.jar' -run 'tour' './config/auction.xml'

Save the results:

java -jar './logist/logist.jar' -score 'tour'

COMMENT FOR WINDOWS USERS: REMOVE THE SINGLE QUOTES (') FROM THE COMMANDS
