package com.scoreboard;

import com.scoreboard.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scoreboard Tests")
class ScoreboardTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard();
    }

    @Test
    @DisplayName("Should start a new game with initial score 0-0")
    void testStartGame() {
        Game game = scoreboard.startGame("Spain", "Brazil");

        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals("Spain", game.getHomeTeam());
        assertEquals("Brazil", game.getAwayTeam());
        assertEquals(0, game.getHomeScore());
        assertEquals(0, game.getAwayScore());
        assertNotNull(game.getStartTime());
    }

    @Test
    @DisplayName("Should throw exception when starting game with empty home team")
    void testStartGameEmptyHomeTeam() {
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame("", "Brazil"));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame("   ", "Brazil"));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame(null, "Brazil"));
    }

    @Test
    @DisplayName("Should throw exception when starting game with empty away team")
    void testStartGameEmptyAwayTeam() {
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame("Spain", ""));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame("Spain", "   "));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.startGame("Spain", null));
    }

    @Test
    @DisplayName("Should update game score successfully")
    void testUpdateScore() {
        Game game = scoreboard.startGame("Spain", "Brazil");

        scoreboard.updateScore(game.getId(), 2, 1);

        assertEquals(2, game.getHomeScore());
        assertEquals(1, game.getAwayScore());
        assertEquals(3, game.getTotalScore());
    }

    @Test
    @DisplayName("Should throw exception when updating with negative scores")
    void testUpdateScoreNegative() {
        Game game = scoreboard.startGame("Spain", "Brazil");

        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.updateScore(game.getId(), -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.updateScore(game.getId(), 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> scoreboard.updateScore(game.getId(), -1, -1));
    }

    @Test
    @DisplayName("Should finish game and remove from scoreboard")
    void testFinishGame() {
        Game game = scoreboard.startGame("Spain", "Brazil");
        Long gameId = game.getId();

        assertEquals(1, scoreboard.getGameCount());

        scoreboard.finishGame(gameId);

        assertEquals(0, scoreboard.getGameCount());
        assertNull(scoreboard.getGame(gameId));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent game")
    void testUpdateScoreNonExistentGame() {
        assertThrows(NoSuchElementException.class,
                () -> scoreboard.updateScore(999L, 1, 1));
    }

    @Test
    @DisplayName("Should throw exception when finishing non-existent game")
    void testFinishNonExistentGame() {
        assertThrows(NoSuchElementException.class,
                () -> scoreboard.finishGame(999L));
    }

    @Test
    @DisplayName("Should return summary ordered by total score descending")
    void testGetSummaryOrderedByTotalScore() {
        Game game1 = scoreboard.startGame("Mexico", "Canada");
        Game game2 = scoreboard.startGame("Spain", "Brazil");
        Game game3 = scoreboard.startGame("Germany", "France");
        Game game4 = scoreboard.startGame("Uruguay", "Italy");
        Game game5 = scoreboard.startGame("Argentina", "Australia");

        scoreboard.updateScore(game1.getId(), 0, 5);
        scoreboard.updateScore(game2.getId(), 10, 2);
        scoreboard.updateScore(game3.getId(), 2, 2);
        scoreboard.updateScore(game4.getId(), 6, 6);
        scoreboard.updateScore(game5.getId(), 3, 1);

        List<Game> summary = scoreboard.getSummary();

        assertEquals(5, summary.size());

        // Check if ordering by total score
        assertTrue(summary.get(0).getTotalScore() >= summary.get(1).getTotalScore());
        assertTrue(summary.get(1).getTotalScore() >= summary.get(2).getTotalScore());
        assertTrue(summary.get(2).getTotalScore() >= summary.get(3).getTotalScore());
        assertTrue(summary.get(3).getTotalScore() >= summary.get(4).getTotalScore());
    }

    @Test
    @DisplayName("Should order games with same total score by start time (most recent first)")
    void testGetSummaryOrderedByStartTime() throws InterruptedException {
        Game game1 = scoreboard.startGame("Germany", "France");
        scoreboard.updateScore(game1.getId(), 2, 2);

        Thread.sleep(10); // Set different start times

        Game game2 = scoreboard.startGame("Argentina", "Australia");
        scoreboard.updateScore(game2.getId(), 3, 1);

        List<Game> summary = scoreboard.getSummary();

        assertEquals(2, summary.size());
        assertEquals(4, summary.get(0).getTotalScore());
        assertEquals(4, summary.get(1).getTotalScore());

        // game2 started later, it should go first
        assertEquals("Argentina", summary.get(0).getHomeTeam());
        assertEquals("Germany", summary.get(1).getHomeTeam());
    }

    @Test
    @DisplayName("Should return empty summary when no games exist")
    void testGetSummaryEmpty() {
        List<Game> summary = scoreboard.getSummary();

        assertNotNull(summary);
        assertTrue(summary.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple games correctly")
    void testMultipleGames() {
        scoreboard.startGame("Mexico", "Canada");
        scoreboard.startGame("Spain", "Brazil");
        scoreboard.startGame("Germany", "France");

        assertEquals(3, scoreboard.getGameCount());
        assertEquals(3, scoreboard.getAllGames().size());
    }

    @Test
    @DisplayName("Should get a game by its ID")
    void testGetGame() {
        Game game = scoreboard.startGame("Spain", "Brazil");

        Game retrieved = scoreboard.getGame(game.getId());

        assertNotNull(retrieved);
        assertEquals(game.getId(), retrieved.getId());
        assertEquals(game.getHomeTeam(), retrieved.getHomeTeam());
        assertEquals(game.getAwayTeam(), retrieved.getAwayTeam());
    }

    @Test
    @DisplayName("Integration test - complete flow")
    void testFullScoreboardFlow() {
        // Start multiple games
        Game game1 = scoreboard.startGame("Mexico", "Canada");
        Game game2 = scoreboard.startGame("Spain", "Brazil");
        Game game3 = scoreboard.startGame("Germany", "France");

        assertEquals(3, scoreboard.getGameCount());

        // Update their scores
        scoreboard.updateScore(game1.getId(), 0, 5);
        scoreboard.updateScore(game2.getId(), 10, 2);
        scoreboard.updateScore(game3.getId(), 2, 2);

        // Verify if summary ordering works
        List<Game> summary = scoreboard.getSummary();
        assertEquals("Spain", summary.get(0).getHomeTeam());
        assertEquals("Mexico", summary.get(1).getHomeTeam());
        assertEquals("Germany", summary.get(2).getHomeTeam());

        // Finish one chosen game
        scoreboard.finishGame(game3.getId());
        assertEquals(2, scoreboard.getGameCount());

        // Verify if finished game is removed from summary
        summary = scoreboard.getSummary();
        assertEquals(2, summary.size());
        assertFalse(summary.stream()
                .anyMatch(g -> g.getHomeTeam().equals("Germany")));
    }
}
