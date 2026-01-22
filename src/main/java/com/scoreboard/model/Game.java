package com.scoreboard.model;

import java.time.Instant;
import java.util.Objects;

public final class Game {
    private static final int MAX_TEAM_NAME_LENGTH = 50;
    private final Long id;
    private final String homeTeam;
    private final String awayTeam;
    private volatile Score score;
    private final Instant startTime;

    public Game(Long id, String homeTeam, String awayTeam) {
        this.id = Objects.requireNonNull(id, "Game ID cannot be null");
        this.homeTeam = validateAndNormalizeTeamName(homeTeam, "Home");
        this.awayTeam = validateAndNormalizeTeamName(awayTeam, "Away");
        if (this.homeTeam.equalsIgnoreCase(this.awayTeam)) {
            throw new IllegalArgumentException("Home and away teams cannot be the same");
        }
        this.score = Score.initial();
        this.startTime = Instant.now();
    }

    public void updateScore(int homeScore, int awayScore) {
        this.score = new Score(homeScore, awayScore);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public int getTotalScore() {
        return score.total();
    }

    public int getHomeScore() {
        return score.home();
    }

    public int getAwayScore() {
        return score.away();
    }

    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return String.format("%s %d - %s %d", homeTeam, score.home(), awayTeam, score.away());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Game game)) return false;
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private String validateAndNormalizeTeamName(String teamName, String teamType) {
        Objects.requireNonNull(teamName, teamType + " team name cannot be null");

        String trimmed = teamName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(teamType + " team name cannot be empty");
        }
        if (trimmed.length() > MAX_TEAM_NAME_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "%s team name too long (max %d characters, got %d)",
                    teamType, MAX_TEAM_NAME_LENGTH, trimmed.length())
            );
        }

        return trimmed;
    }
}
