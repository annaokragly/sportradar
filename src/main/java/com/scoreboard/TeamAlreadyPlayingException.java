package com.scoreboard;

/**
 * Exception thrown when attempting to start a game with a team
 * that is already playing in another ongoing game.
 */
public class TeamAlreadyPlayingException extends RuntimeException {
    private final String teamName;

    public TeamAlreadyPlayingException(String teamName) {
        super(String.format("Team '%s' is already playing in another ongoing game", teamName));
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }
}
