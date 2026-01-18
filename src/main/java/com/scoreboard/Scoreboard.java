package com.scoreboard;

import com.scoreboard.model.Game;

import java.util.*;

public class Scoreboard {
    private final Map<Long, Game> games;
    private long nextId;

    public Scoreboard() {
        this.games = new HashMap<>();
        this.nextId = 1;
    }

    public Game startGame(String homeTeam, String awayTeam) {
        Long id = nextId++;
        Game game = new Game(id, homeTeam, awayTeam);
        games.put(id, game);
        return game;
    }

    public void finishGame(Long id) {
        Game game = games.remove(id);
        if (game == null) {
            throw new NoSuchElementException("Cannot find game with id: " + id);
        }
    }

    public void updateScore(Long id, int homeScore, int awayScore) {
        Game game = games.get(id);
        if (game == null) {
            throw new NoSuchElementException("Cannot find game with id: " + id);
        }
        game.updateScore(homeScore, awayScore);
    }

    public List<Game> getSummary() {
        List<Game> summary = new ArrayList<>(games.values());

        summary.sort((game1, game2) -> {
            int totalCompare = Integer.compare(
                    game2.getTotalScore(),
                    game1.getTotalScore()
            );

            if (totalCompare != 0) {
                return totalCompare;
            }

            return game2.getStartTime().compareTo(game1.getStartTime());
        });

        return summary;
    }

    public Game getGame(Long id) {
        return games.get(id);
    }

    public List<Game> getAllGames() {
        return new ArrayList<>(games.values());
    }

    public int getGameCount() {
        return games.size();
    }
}
