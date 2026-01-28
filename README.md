## Live Football World Cup Scoreboard

Simple in-memory scoreboard library for managing Football World Cup games.

### Features
-  Start new games with automatic ID generation and 0-0 initial score

- Update scores with validation (non-negative, realistic limits)

- Finish games idempotently

- Team deduplication prevents same team in multiple simultaneous games

- Generate summaries ordered by total score (desc) then start time (desc)

- Thread-safe operations using concurrent data structures

- Immutable return values to prevent external state corruption

- Comprehensive validation with clear, detailed error messages

- Modern Java 21 features (records, pattern matching, Optional, virtual threads)

### Requirements

- Java 21
- Maven 3.6+
- JUnit 5.10+

### How to start:

```mvn clean install```

```mvn clean compile```

### Run tests:

```mvn test```

### Run demo:

``` mvn exec:java -Dexec.mainClass="com.scoreboard.ScoreboardDemo"```

### Usage

Basic Example:

```java
import com.scoreboard.Scoreboard;
import com.scoreboard.model.Game;

public class Example {
    public static void main(String[] args) {
        Scoreboard scoreboard = new Scoreboard();

        // Start a game
        Game game = scoreboard.startGame("Spain", "Brazil");
        System.out.println(game); // Spain 0 - Brazil 0

        // Update score
        scoreboard.updateScore(game.getId(), 2, 1);
        System.out.println(game); // Spain 2 - Brazil 1

        // Get summary
        List<Game> summary = scoreboard.getSummary();
        summary.forEach(System.out::println);

        // Finish game
        boolean finished = scoreboard.finishGame(game.getId());
        System.out.println("Game finished: " + finished); // true
    }
}
```

### Design Decisions

1. **Thread Safety**: 

   Use `ConcurrentHashMap` and `AtomicLong` for lock-free concurrency.

    *Rationale*: Allows high-performance concurrent reads while ensuring consistency for writes. Avoids explicit synchronization overhead.
   
    *Trade-off*: Slightly more memory usage than simple `HashMap`, but significantly better scalability.


2. **Score as Value Object**

   Encapsulate score validation in an immutable `Score` record.

    *Rationale*: Follows Domain-Driven Design principles. Centralizes validation logic and prevents invalid state propagation.
   
    *Benefit*: `Game` class stays focused on game orchestration, not score validation rules. The record provides automatic equals/hashCode and immutability.


3. **Idempotent Operations**
   `finishGame()` returns boolean and doesn't throw on missing game.

   *Rationale*: Network retries and distributed systems benefit from idempotency. Matches `HTTP DELETE` semantics.


4. **Immutable Returns**

   *Decision*: Return `List.copyOf()` and unmodifiable collections.
   
   *Rationale*: Prevents external modification of internal state. Follows principle of least privilege.
   
   *Cost*: Small memory overhead for defensive copies, but ensures correctness.


5. **Optional**

   Decision: Provide `Optional<Game> findGame()`

   Rationale: Makes null handling explicit in type system. Guides users toward null-safe code.


6. **Start Time Tracking**
   Decision: Capture `Instant.now()` when game starts.
   
   *Rationale*: Required for summary tie-breaking. Uses system clock for simplicity.
   

7. **Validation Strategy**

   *Decision*: Fail fast with proper exception types at domain boundaries. Validation in domain objects (`Game`, `Score`), not service layer (`Scoreboard`).

   *Rationale*: Prevents invalid state from entering the system. Clear error messages aid debugging.

   *Exception handling:* `NullPointerException` for null inputs (standard Java behavior via Objects.requireNonNull)

   `IllegalArgumentException` for invalid but non-null inputs (blank strings, negative scores)

   `GameNotFoundException` for operations on non-existent games


8. **Team Deduplication**

   **Decision**: Track active teams using `ConcurrentHashMap.newKeySet()` for thread-safe active team tracking to prevent same team in multiple concurrent games.

   **Rationale**: Realistic real life example - a team cannot play in two games simultaneously.

   **Trade-off**: Slightly more memory and complexity, but enforces important domain constraint.


### Architecture

1. **Service Layer (Scoreboard)**: Manages collections, IDs, concurrency
2. **Domain Layer (Game, Score)**: Business logic and validation
3. **Exception Layer (GameNotFoundException, TeamAlreadyPlayingException)**: Domain-specific errors

### License

MIT License
