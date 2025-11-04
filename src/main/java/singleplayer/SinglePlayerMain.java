package singleplayer;

import JsonDTO.CaseFile;
import JsonDTO.LocalizedCaseFile;
import common.commands.Command;
import common.commands.SubmitExamAnswerCommand;
import extractors.BuildingExtractor;
import extractors.CaseLoader;
import extractors.GameObjectExtractor;
import extractors.SuspectExtractor;
import java.util.List;
import java.util.stream.Collectors;
import singleplayer.util.CommandFactorySinglePlayer;
import singleplayer.util.CommandParserSinglePlayer;

public class SinglePlayerMain {

    public static final String CASES_DIRECTORY = "cases";

    private final GameContextSinglePlayer gameContext;

    public SinglePlayerMain() {
        this.gameContext = new GameContextSinglePlayer();
    }

    public List<CaseFile> getAvailableCases() {
        return CaseLoader.loadCases(CASES_DIRECTORY);
    }

    public LocalizedCaseFile selectCaseAndLanguage(CaseFile caseFile, String languageCode) {
        if (caseFile == null || languageCode == null) {
            return null;
        }
        return new LocalizedCaseFile(caseFile, languageCode);
    }

    public void initializeCase(LocalizedCaseFile caseFile) {
        if (caseFile == null) return;

        System.out.println("\nLoading case: " + caseFile.getTitle() + "...");
        gameContext.resetForNewCaseLoad();

        boolean loadingSuccess = true;
        try {
            if (!BuildingExtractor.loadBuilding(caseFile, gameContext)) {
                System.out.println("Error: Failed to load building from case file.");
                loadingSuccess = false;
            } else {
                GameObjectExtractor.loadObjects(caseFile, gameContext);
                SuspectExtractor.loadSuspects(caseFile, gameContext);
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_LOAD_ERROR for '" + caseFile.getTitle() + "': " + e.getMessage());
            e.printStackTrace();
            loadingSuccess = false;
        }

        if (!loadingSuccess) {
            System.out.println("Failed to load case '" + caseFile.getTitle() + "' completely. Returning to case selection.");
            return;
        }

        gameContext.initializeNewCase(caseFile, caseFile.getStartingRoom());

        System.out.println("\n--- Case Invitation ---");
        System.out.println(caseFile.getInvitation());
        System.out.println("\nType 'start case' to begin.");
    }

    public void processCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        String trimmedInput = input.trim();
        Command commandToExecute;
        if (gameContext.isAwaitingExamAnswer()) {
            commandToExecute = new SubmitExamAnswerCommand(gameContext.getAwaitingQuestionNumber(), trimmedInput);
        } else {
            String[] parsedInput = CommandParserSinglePlayer.parseInputSimple(trimmedInput);
            commandToExecute = CommandFactorySinglePlayer.createCommand(parsedInput);
        }

        if (commandToExecute != null) {
            if (gameContext.getPlayerDetective(null) != null) {
                commandToExecute.setPlayerId(game.getPlayerDetective(null).getPlayerId());
                commandToExecute.execute(gameContext);
            } else {
                System.out.println("SP_ERROR: Player detective not initialized. Cannot execute command.");
            }
        } else {
            if (!gameContext.isAwaitingExamAnswer()) {
                System.out.println("Unknown command. Type 'help' for available commands.");
            } else {
                System.out.println("Please type your answer for the question.");
            }
        }
    }

    public GameContextSinglePlayer getGameContext() {
        return this.gameContext;
    }
}
