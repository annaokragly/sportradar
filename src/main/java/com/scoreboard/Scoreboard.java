package com.scoreboard;

import com.scoreboard.model.Game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe Live Football World Cup Score Board.
 */
public class Scoreboard {
    private final Map<Long, Game> games;
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Scoreboard() {
        this.games = new ConcurrentHashMap<>();
    }

    public Game startGame(String homeTeam, String awayTeam) {
        Long gameId = idGenerator.getAndIncrement();
        Game game = new Game(gameId, homeTeam, awayTeam);

        Game existing = games.putIfAbsent(gameId, game);
        if (existing != null) {
            throw new IllegalStateException("Game ID collision detected: " + gameId);
        }

        return game;
    }

    public boolean finishGame(Long gameId) {
        Objects.requireNonNull(gameId, "Game ID cannot be null");
        return games.remove(gameId) != null;
    }

    public void updateScore(Long gameId, int homeScore, int awayScore) {
        Objects.requireNonNull(gameId, "Game ID cannot be null");

        Game game = games.computeIfPresent(gameId, (key, existingGame) -> {
            existingGame.updateScore(homeScore, awayScore);
            return existingGame;
        });

        if (game == null) {
            throw new GameNotFoundException(gameId);
        }
    }

    public List<Game> getSummary() {
        return games.values().stream()
                .sorted(this::compareForSummary)
                .toList();
    }

    public Optional<Game> findGame(Long gameId) {
        Objects.requireNonNull(gameId, "Game ID cannot be null");
        return Optional.ofNullable(games.get(gameId));
    }

    public List<Game> getAllGames() {
        return List.copyOf(games.values());
    }

    public int getGameCount() {
        return games.size();
    }

    public void clear() {
        games.clear();
    }

    private int compareForSummary(Game g1, Game g2) {
        int totalScoreCompare = Integer.compare(g2.getTotalScore(), g1.getTotalScore());
        if (totalScoreCompare != 0) {
            return totalScoreCompare;
        }
        return g2.getStartTime().compareTo(g1.getStartTime());
    }
}
