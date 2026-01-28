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
    private final Set<String> activeTeams;

    public Scoreboard() {
        this.games = new ConcurrentHashMap<>();
        this.activeTeams = ConcurrentHashMap.newKeySet();
    }

    public Game startGame(String homeTeam, String awayTeam) {
        Long gameId = idGenerator.getAndIncrement();
        Game game = new Game(gameId, homeTeam, awayTeam);

        String normalizedHome = game.getHomeTeam();
        String normalizedAway = game.getAwayTeam();

        if (activeTeams.contains(normalizedHome)) {
            throw new TeamAlreadyPlayingException(normalizedHome);
        }

        if (activeTeams.contains(normalizedAway)) {
            throw new TeamAlreadyPlayingException(normalizedAway);
        }

        activeTeams.add(normalizedHome);
        activeTeams.add(normalizedAway);

        Game existing = games.putIfAbsent(gameId, game);
        if (existing != null) {

            activeTeams.remove(normalizedHome);
            activeTeams.remove(normalizedAway);
            throw new IllegalStateException("Game ID collision detected: " + gameId);
        }

        return game;
    }

    public boolean finishGame(Long gameId) {
        Objects.requireNonNull(gameId, "Game ID cannot be null");

        Game game = games.remove(gameId);

        if (game != null) {
            activeTeams.remove(game.getHomeTeam());
            activeTeams.remove(game.getAwayTeam());

            return true;
        }

        return false;
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

    public boolean isTeamPlaying(String teamName) {
        Objects.requireNonNull(teamName, "Team name cannot be null");
        return activeTeams.contains(teamName);
    }

    public Set<String> getActiveTeams() {
        return Set.copyOf(activeTeams);
    }

    public List<Game> getAllGames() {
        return List.copyOf(games.values());
    }

    public int getGameCount() {
        return games.size();
    }

    private int compareForSummary(Game g1, Game g2) {
        int totalScoreCompare = Integer.compare(g2.getTotalScore(), g1.getTotalScore());
        if (totalScoreCompare != 0) {
            return totalScoreCompare;
        }
        return g2.getStartTime().compareTo(g1.getStartTime());
    }
}
