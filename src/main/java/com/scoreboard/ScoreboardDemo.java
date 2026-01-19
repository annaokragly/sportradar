package com.scoreboard;

import com.scoreboard.model.Game;

import java.util.List;

public class ScoreboardDemo {

    public static void main(String[] args) {
        Scoreboard scoreboard = new Scoreboard();

        System.out.println("═══════════════════════════════════════");
        System.out.println("LIVE FOOTBALL WORLD CUP SCOREBOARD DEMO");
        System.out.println("═══════════════════════════════════════\n");

        System.out.println("Starting games...\n");

        Game game1 = scoreboard.startGame("Mexico", "Canada");
        Game game2 = scoreboard.startGame("Spain", "Brazil");
        Game game3 = scoreboard.startGame("Germany", "France");
        Game game4 = scoreboard.startGame("Uruguay", "Italy");
        Game game5 = scoreboard.startGame("Argentina", "Australia");

        printAllGames(scoreboard);



        System.out.println("\nUpdating scores...\n");

        updateAndPrint(scoreboard, game1, 0, 5);
        updateAndPrint(scoreboard, game2, 10, 2);
        updateAndPrint(scoreboard, game3, 2, 2);
        updateAndPrint(scoreboard, game4, 6, 6);
        updateAndPrint(scoreboard, game5, 1, 2);

        System.out.println("\nFinishing game: " + game5.getHomeTeam()+  " " + game5.getHomeScore() + " - " + game5.getAwayScore() + " " + game5.getAwayTeam() + "\n");

        scoreboard.finishGame(game5.getId());

        printSummary(scoreboard);
    }

    private static void printAllGames(Scoreboard scoreboard) {
        System.out.println("Live games:\n");
        List<Game> games = scoreboard.getAllGames();
        for (Game game : games) {
            System.out.printf("%s %d - %d %s\n",
                game.getHomeTeam(),
                game.getHomeScore(),
                game.getAwayScore(),
                game.getAwayTeam());
        }
        System.out.println();
    }

    private static void updateAndPrint(Scoreboard scoreboard, Game game, int homeScore, int awayScore) {
        scoreboard.updateScore(game.getId(), homeScore, awayScore);
        System.out.printf("Updated: %s %d - %d %s\n",
            game.getHomeTeam(),
            game.getHomeScore(),
            game.getAwayScore(),
            game.getAwayTeam());
    }

    private static void printSummary(Scoreboard scoreboard) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("          LIVE GAMES SUMMARY");
        System.out.println("═══════════════════════════════════════\n");

        List<Game> summary = scoreboard.getSummary();
        if (summary.isEmpty()) {
            System.out.println("  No games on the scoreboard.");
        } else {
            for (int i = 0; i < summary.size(); i++) {
                Game game = summary.get(i);
                System.out.printf("%2d. %-15s %2d - %2d %-15s\n",
                        i + 1,
                        game.getHomeTeam(),
                        game.getHomeScore(),
                        game.getAwayScore(),
                        game.getAwayTeam()
                );
            }
        }

        System.out.println("\n");
    }
}
