package com.scoreboard.model;

/**
 * Immutable value object representing a game score.
 */
public record Score(int home, int away) {
    private static final int MAX_REALISTIC_SCORE = 50;

    public Score {
        validateScore(home, "Home");
        validateScore(away, "Away");
    }

    public static Score initial() {
        return new Score(0, 0);
    }

    public int total() {
        return home + away;
    }

    @Override
    public String toString() {
        return home + "-" + away;
    }

    private static void validateScore(int score, String teamType) {
        if (score < 0) {
            throw new IllegalArgumentException(
                    String.format("%s score cannot be negative (got: %d)", teamType, score)
            );
        }
        if (score > MAX_REALISTIC_SCORE) {
            throw new IllegalArgumentException(
                    String.format("%s score exceeds realistic maximum of %d (got: %d)",
                            teamType, MAX_REALISTIC_SCORE, score)
            );
        }
    }
}
