## Live Football World Cup Scoreboard

### Requirements

- Java 21
- Maven 3.6+

### How to start:

```mvn clean install```

```mvn clean compile```

### Run tests:

```mvn test```

### Run demo:

``` mvn exec:java -Dexec.mainClass="com.scoreboard.ScoreboardDemo"```

### License

MIT License



Note:
Honestly from the requirements I understood that you only wanted a live scoreboard, so the current implementation 
addresses that: if the match is being concluded it's not gonna appear on the scoreboard anymore.

Now, if I understood wrong and you wanted the summary to print all games (both finished and live) 
then a separate List<Game> would be needed to store finished games and then in the full summary a List<Game>
that would combine both live and finished games and then we could print all of them.
