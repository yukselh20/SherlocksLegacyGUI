package singleplayer;

// MODIFIED: Added imports for LocalizedCaseFile and related utilities
import JsonDTO.CaseFile;
import JsonDTO.LocalizedCaseFile;
import common.commands.Command;
import common.commands.SubmitExamAnswerCommand;
import extractors.BuildingExtractor;
import extractors.CaseLoader;
import extractors.GameObjectExtractor;
import extractors.SuspectExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import singleplayer.util.CaseFileUtil;
import singleplayer.util.CommandFactorySinglePlayer;
import singleplayer.util.CommandParserSinglePlayer;

public class SinglePlayerMain {

  // ... (Constants remain the same) ...
  public static final String CASES_DIRECTORY = "cases";
  private static final int MENU_WIDTH = 90;
  private static final String BORDER_CHAR = "═";
  private static final String CORNER_TL = "╔";
  private static final String CORNER_TR = "╗";
  private static final String CORNER_BL = "╚";
  private static final String CORNER_BR = "╝";
  private static final String SIDE_BORDER = "║";
  private static final String T_LEFT = "╠";
  private static final String T_RIGHT = "╣";
  private static final String DIVIDER = T_LEFT + BORDER_CHAR.repeat(MENU_WIDTH) + T_RIGHT;
  private static final String TOP_BORDER = CORNER_TL + BORDER_CHAR.repeat(MENU_WIDTH) + CORNER_TR;
  private static final String BOTTOM_BORDER = CORNER_BL + BORDER_CHAR.repeat(MENU_WIDTH) + CORNER_BR;

  private final GameContextSinglePlayer gameContext;
  private final Scanner scanner;

  public SinglePlayerMain() {
    this.gameContext = new GameContextSinglePlayer();
    this.scanner = new Scanner(System.in);
  }

  public void runGame() {
    while (!gameContext.wantsToExitApplication()) {
      gameContext.resetExitFlags();
      List<CaseFile> availableCases = displayCaseSelectionMenu(); // MODIFIED: Now returns the list
      LocalizedCaseFile selectedCase = selectCase(availableCases); // MODIFIED: Now returns LocalizedCaseFile

      if (selectedCase == null) {
        if (gameContext.wantsToExitApplication()) {
          break;
        }
        continue;
      }
      initializeAndPlayCase(selectedCase); // MODIFIED: Takes LocalizedCaseFile
    }
    System.out.println("\nThank you for playing Single Player. Goodbye!");
  }

  // MODIFIED: This method now detects and displays available languages.
  private List<CaseFile> displayCaseSelectionMenu() {
    List<CaseFile> cases = CaseLoader.loadCases(CASES_DIRECTORY);

    System.out.println("\n" + TOP_BORDER);
    String title = "SELECT A CASE TO INVESTIGATE";
    int padding = (MENU_WIDTH - title.length()) / 2;
    System.out.println(SIDE_BORDER + " ".repeat(Math.max(0, padding)) + title
            + " ".repeat(Math.max(0, MENU_WIDTH - title.length() - padding)) + SIDE_BORDER);
    System.out.println(DIVIDER);

    if (cases.isEmpty()) {
      String noCasesMsg = "No cases found in '" + CASES_DIRECTORY + "'. Use 'add case [path]' or 'quit'.";
      int msgPadding = (MENU_WIDTH - noCasesMsg.length()) / 2;
      System.out.printf("%s %-" + MENU_WIDTH + "s %s%n", SIDE_BORDER, " ".repeat(Math.max(0, msgPadding)) + noCasesMsg, SIDE_BORDER);
    } else {
      for (int i = 0; i < cases.size(); i++) {
        CaseFile currentCase = cases.get(i);
        // NEW: Build a string of available languages
        String languages = currentCase.getLocalizations().values().stream()
                .map(CaseFile.LocalizedData::getLanguageName)
                .collect(Collectors.joining(", "));

        String caseLine = String.format("%d. %s [%s]", i + 1, currentCase.getUniversalTitle(), languages);
        System.out.printf("%s %-" + MENU_WIDTH + "s %s%n", SIDE_BORDER, caseLine, SIDE_BORDER);
      }
    }
    System.out.println(BOTTOM_BORDER);
    return cases;
  }

  // MODIFIED: This method now handles both case and language selection.
  private LocalizedCaseFile selectCase(List<CaseFile> cases) {
    while (true) {
      System.out.print("Enter case number (0 to add case, 'quit' to exit game): ");
      String input = scanner.nextLine().trim();

      if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
        gameContext.handlePlayerExitRequest(null);
        return null;
      }
      if (input.equals("0") || input.toLowerCase().startsWith("add case")) {
        handleAddingCase(input);
        return null;
      }

      try {
        int choice = Integer.parseInt(input);
        if (choice > 0 && choice <= cases.size()) {
          CaseFile chosenCase = cases.get(choice - 1);
          // NEW: After selecting a case, select a language.
          String langCode = selectLanguage(chosenCase);
          if (langCode == null) { // User chose to go back
            return null; // This will refresh the main menu
          }
          // Return the single-language adapter object
          return new LocalizedCaseFile(chosenCase, langCode);
        } else {
          System.out.println("Invalid choice. Not in the list.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number or a command.");
      }
    }
  }

  // NEW: A dedicated method for language selection.
  private String selectLanguage(CaseFile selectedCase) {
    List<Map.Entry<String, CaseFile.LocalizedData>> languages = new ArrayList<>(selectedCase.getLocalizations().entrySet());
    if (languages.size() == 1) {
      return languages.get(0).getKey(); // Auto-select if there's only one language.
    }

    while (true) {
      System.out.println("\nPlease select a language for '" + selectedCase.getUniversalTitle() + "':");
      for (int i = 0; i < languages.size(); i++) {
        System.out.printf("  %d. %s\n", i + 1, languages.get(i).getValue().getLanguageName());
      }
      System.out.print("Enter language number (or 0 to go back): ");
      String input = scanner.nextLine().trim();

      try {
        int choice = Integer.parseInt(input);
        if (choice == 0) {
          return null; // Go back to case selection
        }
        if (choice > 0 && choice <= languages.size()) {
          return languages.get(choice - 1).getKey(); // Return the chosen language code (e.g., "en")
        } else {
          System.out.println("Invalid choice. Not in the list.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  // MODIFIED: This method now takes a LocalizedCaseFile.
  // The rest of the method body can remain EXACTLY THE SAME!
  private void initializeAndPlayCase(LocalizedCaseFile caseFile) {
    if (caseFile == null) return;

    System.out.println("\nLoading case: " + caseFile.getTitle() + "...");
    gameContext.resetForNewCaseLoad();

    boolean loadingSuccess = true;
    try {
      // This works because LocalizedCaseFile has the same methods as the old CaseFile DTO
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
    System.out.println("\nType 'start case' to begin, or 'exit' to return to case selection.");

    playCurrentCase();
  }

  // ... (handleAddingCase, playCurrentCase, and main methods remain unchanged) ...
  // NOTE: I've copied them below for completeness.

  private void handleAddingCase(String initialInput) {
    // ... this method's content does not need to change ...
    String filePath = "";
    if (initialInput.toLowerCase().startsWith("add case")) {
      if (initialInput.length() > "add case ".length()) {
        filePath = initialInput.substring("add case ".length()).trim();
      }
    }
    if (filePath.isEmpty()) {
      System.out.print("Enter the full file path to the case JSON: ");
      filePath = scanner.nextLine().trim();
      if (filePath.isEmpty()) {
        System.out.println("Add case cancelled: No file path provided.");
        return;
      }
    }
    CaseFileUtil.addCaseFromFile(filePath);
  }

  private void playCurrentCase() {
    // ... this method's content does not need to change ...
    while (!gameContext.wantsToExitToCaseSelection() && !gameContext.wantsToExitApplication()) {
      String prompt = "> ";
      if (gameContext.isAwaitingExamAnswer()) {
        prompt = "";
      } else if (gameContext.isCaseStarted() && gameContext.getCurrentRoomForPlayer(null) != null) {
        prompt = "<" + gameContext.getCurrentRoomForPlayer(null).getName() + "> ";
      }
      if (!prompt.isEmpty()) System.out.print(prompt);
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) continue;
      Command commandToExecute;
      if (gameContext.isAwaitingExamAnswer()) {
        commandToExecute = new SubmitExamAnswerCommand(gameContext.getAwaitingQuestionNumber(), input);
      } else {
        String[] parsedInput = CommandParserSinglePlayer.parseInputSimple(input);
        commandToExecute = CommandFactorySinglePlayer.createCommand(parsedInput);
      }
      if (commandToExecute != null) {
        if (gameContext.getPlayerDetective(null) != null) {
          commandToExecute.setPlayerId(gameContext.getPlayerDetective(null).getPlayerId());
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
    if (gameContext.wantsToExitToCaseSelection()) {
      if (gameContext.isCaseStarted()) {
        gameContext.setCaseStarted(false);
      }
      System.out.println("\nReturning to case selection menu...");
    }
  }

  public static void main(String[] args) {
    SinglePlayerMain game = new SinglePlayerMain();
    try {
      game.runGame();
    } catch (Exception e) {
      System.err.println("\nUNEXPECTED SP_ERROR: An unhandled error occurred in SinglePlayerMain:");
      e.printStackTrace();
      System.err.println("Application will now exit.");
    }
  }
}