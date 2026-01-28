package com.scoreboard;

import com.scoreboard.model.Game;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scoreboard")
class ScoreboardTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
    }

    @Nested
    @DisplayName("starting games")
    class StartGameTests {

        @Test
        @DisplayName("should start a new game with initial score 0-0")
        void startsGame() {
            Game game = scoreboard.startGame("Spain", "Brazil");

            assertAll("Game initialization",
                    () -> assertNotNull(game.getId(), "Game should have an ID"),
                    () -> assertEquals("Spain", game.getHomeTeam(), "Home team name"),
                    () -> assertEquals("Brazil", game.getAwayTeam(), "Away team name"),
                    () -> assertEquals(0, game.getHomeScore(), "Initial home score"),
                    () -> assertEquals(0, game.getAwayScore(), "Initial away score"),
                    () -> assertNotNull(game.getStartTime(), "Start time recorded")
            );
        }

        @Test
        @DisplayName("should reject null team names")
        void rejectsNullTeamNames() {
            assertAll("Null team names",
                    () -> assertThrows(NullPointerException.class,
                            () -> scoreboard.startGame(null, "Brazil")),
                    () -> assertThrows(NullPointerException.class,
                            () -> scoreboard.startGame("Spain", null))
            );
        }

        @ParameterizedTest(name = "team name = ''{0}''")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("should reject blank team names")
        void rejectsBlankTeamNames(String invalidName) {
            assertAll("Blank team names",
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> scoreboard.startGame(invalidName, "Brazil")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> scoreboard.startGame("Spain", invalidName))
            );
        }

        @Test
        @DisplayName("should reject identical team names")
        void rejectsIdenticalTeamNames() {
            assertThrows(IllegalArgumentException.class,
                    () -> scoreboard.startGame("Spain", "Spain"));
        }
    }

    @Nested
    @DisplayName("updating scores")
    class UpdateScoreTests {

        @Test
        @DisplayName("should update game score successfully")
        void updatesScoreSuccessfully() {
            Game game = scoreboard.startGame("Spain", "Brazil");

            scoreboard.updateScore(game.getId(), 2, 1);

            assertAll("Score update",
                    () -> assertEquals(2, game.getHomeScore()),
                    () -> assertEquals(1, game.getAwayScore()),
                    () -> assertEquals(3, game.getTotalScore())
            );
        }

        @ParameterizedTest
        @CsvSource({
                "-1, 0",
                "0, -1",
                "-1, -1"
        })
        @DisplayName("should reject negative scores")
        void rejectsNegativeScores(int homeScore, int awayScore) {
            Game game = scoreboard.startGame("Spain", "Brazil");

            assertThrows(IllegalArgumentException.class,
                    () -> scoreboard.updateScore(game.getId(), homeScore, awayScore));
        }

        @Test
        @DisplayName("should throw GameNotFoundException for non-existent game")
        void throwsExceptionForNonExistentGame() {
            GameNotFoundException exception = assertThrows(GameNotFoundException.class,
                    () -> scoreboard.updateScore(999L, 1, 1));

            assertTrue(exception.getMessage().contains("999"));
        }
    }

    @Nested
    @DisplayName("finishing games")
    class FinishGameTests {

        @Test
        @DisplayName("should finish game and remove from scoreboard")
        void finishesGameAndRemovesFromScoreboard() {
            Game game = scoreboard.startGame("Spain", "Brazil");
            Long gameId = game.getId();

            boolean result = scoreboard.finishGame(gameId);

            assertAll("Game finish",
                    () -> assertTrue(result, "Should return true on successful finish"),
                    () -> assertEquals(0, scoreboard.getGameCount(), "Game should be removed"),
                    () -> assertTrue(scoreboard.findGame(gameId).isEmpty(), "Game should not be findable")
            );
        }

        @Test
        @DisplayName("should return false for non-existent game")
        void returnsFalseForNonExistentGame() {
            assertFalse(scoreboard.finishGame(999L));
        }

        @Test
        @DisplayName("should be idempotent - finishing same game multiple times")
        void isIdempotent() {
            Game game = scoreboard.startGame("Spain", "Brazil");
            Long gameId = game.getId();

            boolean firstFinish = scoreboard.finishGame(gameId);
            boolean secondFinish = scoreboard.finishGame(gameId);
            boolean thirdFinish = scoreboard.finishGame(gameId);

            assertAll("Idempotency",
                    () -> assertTrue(firstFinish, "First finish should succeed"),
                    () -> assertFalse(secondFinish, "Second finish should fail"),
                    () -> assertFalse(thirdFinish, "Third finish should fail"),
                    () -> assertEquals(0, scoreboard.getGameCount(), "Game count should be 0")
            );
        }
    }

    @Nested
    @DisplayName("summary generation")
    class SummaryTests {

        @Test
        @DisplayName("should return empty list when no games exist")
        void returnsEmptyListForNoGames() {
            List<Game> summary = scoreboard.getSummary();

            assertNotNull(summary);
            assertTrue(summary.isEmpty());
        }

        @Test
        @DisplayName("should order games by total score descending")
        void ordersGamesByTotalScore() {
            Game game1 = scoreboard.startGame("Mexico", "Canada");
            Game game2 = scoreboard.startGame("Spain", "Brazil");
            Game game3 = scoreboard.startGame("Germany", "France");
            Game game4 = scoreboard.startGame("Uruguay", "Italy");
            Game game5 = scoreboard.startGame("Argentina", "Australia");

            scoreboard.updateScore(game1.getId(), 0, 5);   // Total: 5
            scoreboard.updateScore(game2.getId(), 10, 2);  // Total: 12
            scoreboard.updateScore(game3.getId(), 2, 2);   // Total: 4
            scoreboard.updateScore(game4.getId(), 6, 6);   // Total: 12
            scoreboard.updateScore(game5.getId(), 3, 1);   // Total: 4

            List<Game> summary = scoreboard.getSummary();

            assertEquals(5, summary.size());
            // Verify descending order
            for (int i = 0; i < summary.size() - 1; i++) {
                assertTrue(summary.get(i).getTotalScore() >= summary.get(i + 1).getTotalScore(),
                        "Scores should be in descending order");
            }
        }

        @Test
        @DisplayName("should order games with same score by start time (most recent first)")
        void ordersGamesByStartTimeWhenScoresEqual() throws InterruptedException {
            Game game1 = scoreboard.startGame("Germany", "France");
            scoreboard.updateScore(game1.getId(), 2, 2);

            Thread.sleep(50); // Ensure different start times

            Game game2 = scoreboard.startGame("Argentina", "Australia");
            scoreboard.updateScore(game2.getId(), 3, 1);

            List<Game> summary = scoreboard.getSummary();

            assertAll("Equal score ordering",
                    () -> assertEquals(2, summary.size()),
                    () -> assertEquals(4, summary.get(0).getTotalScore()),
                    () -> assertEquals(4, summary.get(1).getTotalScore()),
                    () -> assertEquals("Argentina", summary.get(0).getHomeTeam(),
                            "Most recent game should be first"),
                    () -> assertEquals("Germany", summary.get(1).getHomeTeam())
            );
        }

        @Test
        @DisplayName("should match requirements example")
        void matchesRequirementExample() {
            Game mexico = scoreboard.startGame("Mexico", "Canada");
            Game spain = scoreboard.startGame("Spain", "Brazil");
            Game germany = scoreboard.startGame("Germany", "France");
            Game uruguay = scoreboard.startGame("Uruguay", "Italy");
            Game argentina = scoreboard.startGame("Argentina", "Australia");

            scoreboard.updateScore(mexico.getId(), 0, 5);
            scoreboard.updateScore(spain.getId(), 10, 2);
            scoreboard.updateScore(germany.getId(), 2, 2);
            scoreboard.updateScore(uruguay.getId(), 6, 6);
            scoreboard.updateScore(argentina.getId(), 3, 1);

            List<Game> summary = scoreboard.getSummary();

            assertAll("Requirements example",
                    () -> assertEquals(5, summary.size()),
                    () -> assertEquals("Uruguay", summary.get(0).getHomeTeam()),
                    () -> assertEquals("Spain", summary.get(1).getHomeTeam()),
                    () -> assertEquals("Mexico", summary.get(2).getHomeTeam()),
                    () -> assertEquals("Argentina", summary.get(3).getHomeTeam()),
                    () -> assertEquals("Germany", summary.get(4).getHomeTeam())
            );
        }
    }

    @Nested
    @DisplayName("team deduplication")
    class TeamDeduplicationTests {

        @Test
        @DisplayName("should prevent same team in multiple games")
        void preventsSameTeamInMultipleGames() {
            scoreboard.startGame("Spain", "Brazil");

            // Try to start another game with Spain
            TeamAlreadyPlayingException exception = assertThrows(
                    TeamAlreadyPlayingException.class,
                    () -> scoreboard.startGame("Spain", "Germany")
            );

            assertTrue(exception.getMessage().contains("Spain"));
            assertEquals("Spain", exception.getTeamName());
        }

        @Test
        @DisplayName("should prevent both teams if either is playing")
        void preventsBothTeamsIfEitherIsPlaying() {
            scoreboard.startGame("Spain", "Brazil");

            assertAll("Both teams should be blocked",
                    () -> assertThrows(TeamAlreadyPlayingException.class,
                            () -> scoreboard.startGame("Spain", "Germany")),
                    () -> assertThrows(TeamAlreadyPlayingException.class,
                            () -> scoreboard.startGame("Germany", "Brazil"))
            );
        }

        @Test
        @DisplayName("should allow team to play again after game finishes")
        void allowsTeamToPlayAgainAfterFinish() {
            Game game = scoreboard.startGame("Spain", "Brazil");

            scoreboard.finishGame(game.getId());

            // Now Spain can play again
            assertDoesNotThrow(() ->
                    scoreboard.startGame("Spain", "Germany")
            );
        }

        @Test
        @DisplayName("should handle team names case-sensitively")
        void handlesTeamNamesCaseSensitively() {
            scoreboard.startGame("Spain", "Brazil");

            // "spain" (lowercase) is different from "Spain"
            assertDoesNotThrow(() ->
                    scoreboard.startGame("spain", "Germany")
            );
        }

        @Test
        @DisplayName("should track active teams correctly")
        void tracksActiveTeamsCorrectly() {
            assertTrue(scoreboard.getActiveTeams().isEmpty());

            Game game1 = scoreboard.startGame("Spain", "Brazil");
            assertEquals(2, scoreboard.getActiveTeams().size());
            assertTrue(scoreboard.isTeamPlaying("Spain"));
            assertTrue(scoreboard.isTeamPlaying("Brazil"));

            Game game2 = scoreboard.startGame("Germany", "France");
            assertEquals(4, scoreboard.getActiveTeams().size());

            scoreboard.finishGame(game1.getId());
            assertEquals(2, scoreboard.getActiveTeams().size());
            assertFalse(scoreboard.isTeamPlaying("Spain"));
            assertFalse(scoreboard.isTeamPlaying("Brazil"));
            assertTrue(scoreboard.isTeamPlaying("Germany"));
        }

        @Test
        @DisplayName("should handle concurrent attempts to use same team")
        void handlesConcurrentAttemptsToUseSameTeam() throws InterruptedException {
            int numThreads = 10;
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(numThreads);

            try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
                for (int i = 0; i < numThreads; i++) {
                    final int threadNum = i;
                    executor.submit(() -> {
                        try {
                            // All threads try to start game with "Spain"
                            scoreboard.startGame("Spain", "Country" + threadNum);
                            successCount.incrementAndGet();
                        } catch (TeamAlreadyPlayingException e) {
                            failureCount.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(5, TimeUnit.SECONDS));
            }

            // Only 1 should succeed, rest should fail
            assertEquals(1, successCount.get(),
                    "Only one thread should successfully start game with Spain");
            assertEquals(9, failureCount.get(),
                    "Nine threads should fail due to team already playing");
        }
    }

    @Nested
    @DisplayName("thread safety")
    class ConcurrencyTests {

        @Test
        @DisplayName("should handle concurrent game creation safely")
        void handlesConcurrentGameCreation() throws InterruptedException {
            int numThreads = 10;
            int gamesPerThread = 10;
            CountDownLatch latch = new CountDownLatch(numThreads * gamesPerThread);
            AtomicInteger successCount = new AtomicInteger(0);

            try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
                for (int i = 0; i < numThreads; i++) {
                    int threadNum = i;
                    executor.submit(() -> {
                        for (int j = 0; j < gamesPerThread; j++) {
                            try {
                                scoreboard.startGame(
                                        "Team" + threadNum + "A" + j,
                                        "Team" + threadNum + "B" + j
                                );
                                successCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
                }

                assertTrue(latch.await(5, TimeUnit.SECONDS),
                        "All concurrent operations should complete");
                assertEquals(100, successCount.get(),
                        "All 100 games should be created successfully");
            }
        }

        @Test
        @DisplayName("should handle concurrent score updates")
        void handlesConcurrentScoreUpdates() throws InterruptedException {
            Game game = scoreboard.startGame("Spain", "Brazil");
            int numThreads = 10;
            CountDownLatch latch = new CountDownLatch(numThreads);

            try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
                for (int i = 0; i < numThreads; i++) {
                    final int score = i;
                    executor.submit(() -> {
                        try {
                            scoreboard.updateScore(game.getId(), score, score);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(5, TimeUnit.SECONDS));

                // Last write wins - score should be from one of the updates
                int finalScore = game.getHomeScore();
                assertTrue(finalScore >= 0 && finalScore < numThreads,
                        "Final score should be from one of the concurrent updates");
            }
        }
    }

    @Nested
    @DisplayName("integration scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("should handle complete workflow")
        void handlesCompleteWorkflow() {
            // Start multiple games
            Game game1 = scoreboard.startGame("Mexico", "Canada");
            Game game2 = scoreboard.startGame("Spain", "Brazil");
            Game game3 = scoreboard.startGame("Germany", "France");

            assertEquals(3, scoreboard.getGameCount());

            // Update their scores
            scoreboard.updateScore(game1.getId(), 0, 5);
            scoreboard.updateScore(game2.getId(), 10, 2);
            scoreboard.updateScore(game3.getId(), 2, 2);

            // Verify summary ordering
            List<Game> summary = scoreboard.getSummary();
            assertEquals("Spain", summary.get(0).getHomeTeam());
            assertEquals("Mexico", summary.get(1).getHomeTeam());
            assertEquals("Germany", summary.get(2).getHomeTeam());

            // Finish one game
            assertTrue(scoreboard.finishGame(game3.getId()));
            assertEquals(2, scoreboard.getGameCount());

            // Verify finished game removed from summary
            summary = scoreboard.getSummary();
            assertEquals(2, summary.size());
            assertFalse(summary.stream()
                    .anyMatch(g -> g.getHomeTeam().equals("Germany")));
        }
    }
}
