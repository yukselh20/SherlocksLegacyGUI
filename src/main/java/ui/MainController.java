package ui;

import client.GameClient;
import client.GameClientStateListener;
import common.NetworkConstants;
import common.dto.PublicGameInfoDTO;
import common.dto.RoomDescriptionDTO;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import server.ServerMain;
import singleplayer.SinglePlayerMain;
import ui.util.GameOutputParser;
import ui.util.RoomView;
import ui.util.TextAreaOutputStream;
import ui.windows.ChatWindow;
import ui.windows.JournalWindow;
import ui.windows.TasksWindow;
import ui.windows.HelpWindow;

public class MainController implements GameClientStateListener {

    private enum UIState {
        MENU,
        CHOOSING_CASE,
        CHOOSING_LANGUAGE,
        CASE_INVITATION,
        GAME_SINGLE,
        GAME_MULTI
    }

    private enum UIMultiplayerSubState {
        NONE,
        CONNECTING,
        MAIN_MENU,
        HOST_OPTIONS,
        CASE_SELECTION,
        LANGUAGE_SELECTION,
        HOSTING_LOBBY,
        JOIN_OPTIONS,
        PUBLIC_GAMES_LIST,
        PRIVATE_GAME_ENTRY,
        IN_LOBBY,
        IN_GAME,
        DISCONNECTED
    }

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Button tasksButton;
    @FXML
    private Button journalButton;
    @FXML
    private Button chatButton;
    @FXML
    private Button helpButton;
    @FXML
    private Button exitButton;
    @FXML
    private Label unreadChatLabel;
    @FXML
    private StackPane roomPane;
    @FXML
    private VBox rightInfoPanel;
    @FXML
    private TextArea terminalTextArea;
    @FXML
    private TextField terminalInputField;
    @FXML
    private Label statusLabel;
    @FXML
    private SplitPane bottomSplitPane;

    private GameClient gameClient;
    private Thread gameClientThread;
    private JournalWindow journalWindow;
    private ChatWindow chatWindow;
    private TasksWindow tasksWindow;
    private HelpWindow helpWindow;
    private RoomView roomView;
    private int unreadChatCount = 0;

    private VBox mainMenuVBox;
    private List<String> launchArgs;
    private HostServices hostServices;
    private UIState currentState = UIState.MENU;

    private TextAreaOutputStream taos;
    private SinglePlayerMain singlePlayerGame;
    private Thread singlePlayerGameThread;
    private JsonDTO.CaseFile selectedCaseFile; // Temporarily store the case for language selection
    private UIMultiplayerSubState currentMultiplayerSubState = UIMultiplayerSubState.NONE;
    private boolean isSinglePlayer;
    private boolean isHostPlayer;

    @FXML
    public void initialize() {
        terminalTextArea.setEditable(false);
        terminalTextArea.setWrapText(true);
        terminalInputField.setOnAction(event -> handleTerminalInput());
        tasksButton.setOnAction(event -> {
            playSound("click.wav");
            openTasksWindow();
        });
        journalButton.setOnAction(event -> {
            playSound("pageflip.mp3");
            openJournalWindow();
        });
        chatButton.setOnAction(event -> {
            playSound("click.wav");
            openChatWindow();
        });

        helpButton.setOnAction(event -> {
            playSound("click.wav");
            openHelpWindow();
        });

        exitButton.setOnAction(event -> {
            playSound("click.wav");
            sendCommand("exit");
        });

        updateStatus("GUI Ready. Please select a game mode.");
        unreadChatLabel.setVisible(false);
        bottomSplitPane.setDividerPositions(0.7);

        roomView = new RoomView(this);
        roomPane.getChildren().add(roomView);

        // Redirect System.out and System.in
        this.taos = new TextAreaOutputStream(terminalTextArea);
        GameOutputParser parser = new GameOutputParser(this);
        taos.setParser(parser);
        System.setOut(new PrintStream(taos, true));

        createMainMenu();
        setupButtonIcons();
        updateUIVisibility();
    }

    private void showCaseInvitation(String invitationText, boolean isHost) {
        VBox invitationBox = new VBox(20);
        invitationBox.setAlignment(Pos.CENTER);
        invitationBox.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLabel = new Label("Case Invitation");
        titleLabel.setStyle("-fx-font-size: 24; -fx-text-fill: #d4af37;");

        TextArea invitationTextArea = new TextArea(invitationText);
        invitationTextArea.setEditable(false);
        invitationTextArea.setWrapText(true);
        invitationTextArea.setStyle("-fx-control-inner-background: #0a0a0a; -fx-text-fill: #00ff00; -fx-font-family: 'Courier New';");
        invitationTextArea.setPrefWidth(600);
        invitationTextArea.setPrefHeight(400);

        Button startButton = new Button("Start Case");
        startButton.setOnAction(event -> handleStartCase());

        invitationBox.getChildren().addAll(titleLabel, invitationTextArea, startButton);

        if (!isHost) {
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            invitationBox.getChildren().add(cancelButton);
        }

        Platform.runLater(() -> {
            roomPane.getChildren().clear();
            roomPane.getChildren().add(invitationBox);
            currentState = UIState.CASE_INVITATION;
            updateUIVisibility();
        });
    }

    private void handleStartCase() {
        playSound("click.wav");
        if (isSinglePlayer) {
            // Run in a background thread to avoid freezing the UI
            new Thread(() -> {
                singlePlayerGame.processCommand("start case");
                Platform.runLater(() -> {
                    currentState = UIState.GAME_SINGLE;
                    updateUIVisibility();
                });
            }).start();
        } else {
            sendCommand("start case");
            // The UI will be updated by the server's response
        }
    }

    public void setLaunchArgs(List<String> args) {
        this.launchArgs = args;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupButtonIcons() {
        setButtonIcon(tasksButton, "/icons/tasks.png");
        setButtonIcon(journalButton, "/icons/journal.png");
        setButtonIcon(chatButton, "/icons/chat.png");
    }

    private void setButtonIcon(Button button, String iconPath) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitHeight(20);
            iconView.setFitWidth(20);
            button.setGraphic(iconView);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath);
        }
    }

    private void playSound(String soundFile) {
        try {
            String soundPath = getClass().getResource("/sounds/" + soundFile).toExternalForm();
            Media sound = new Media(soundPath);
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play sound: " + soundFile);
        }
    }

    private void createMainMenu() {
        mainMenuVBox = new VBox(15);
        mainMenuVBox.setAlignment(Pos.CENTER);
        mainMenuVBox.getStyleClass().add("main-menu-container");

        Button singlePlayerButton = new Button("Single Player");
        singlePlayerButton.getStyleClass().add("main-menu-button");
        singlePlayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("1");
        });

        Button multiplayerButton = new Button("Multiplayer (Join/Host)");
        multiplayerButton.getStyleClass().add("main-menu-button");
        multiplayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("2");
        });

        Button startServerButton = new Button("Start Server Only");
        startServerButton.getStyleClass().add("main-menu-button");
        startServerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("3");
        });

        Button quitButton = new Button("Quit");
        quitButton.getStyleClass().add("main-menu-button");
        quitButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("4");
        });

        mainMenuVBox
                .getChildren()
                .addAll(singlePlayerButton, multiplayerButton, startServerButton, quitButton);
    }

    private void handleMainMenuInput(String input) {
        switch (input) {
            case "1":
                startSinglePlayer();
                break;
            case "2":
                startMultiplayer();
                break;
            case "3":
                startServer();
                break;
            case "4":
                shutdown();
                break;
            default:
                terminalTextArea.appendText("Invalid selection. Please enter a number from 1 to 4.\n");
                break;
        }
    }

    private void updateUIVisibility() {
        Platform.runLater(() -> {
            Node currentView = roomPane.getChildren().isEmpty() ? null : roomPane.getChildren().get(0);
            Node nextView = null;

            switch (currentState) {
                case CASE_INVITATION:
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(isHostPlayer); // Only host can exit at this stage
                    rightInfoPanel.setVisible(false);
                    return; // Return early to prevent view transition logic from running
                case MENU:
                    nextView = mainMenuVBox;
                    terminalTextArea.clear();
                    terminalTextArea.appendText("Welcome to Detective Game! Please select a mode to begin.\n");
                    terminalTextArea.appendText("\n--- Main Menu ---\n");
                    terminalTextArea.appendText("1. Single Player\n");
                    terminalTextArea.appendText("2. Multiplayer (Join/Host)\n");
                    terminalTextArea.appendText("3. Start Server Only\n");
                    terminalTextArea.appendText("4. Quit\n");
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    break;
                case GAME_SINGLE:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    chatButton.setVisible(false);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
                case GAME_MULTI:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    chatButton.setVisible(true);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
            }

            if (currentView != nextView) {
                final Node viewToDisplay = nextView;
                FadeTransition ft = new FadeTransition(Duration.millis(500), currentView);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.setOnFinished(event -> {
                    roomPane.getChildren().clear();
                    roomPane.getChildren().add(viewToDisplay);
                    FadeTransition ft2 = new FadeTransition(Duration.millis(500), viewToDisplay);
                    ft2.setFromValue(0.0);
                    ft2.setToValue(1.0);
                    ft2.play();
                });
                ft.play();
            }
        });
    }

    private void startSinglePlayer() {
        isSinglePlayer = true;
        isHostPlayer = true;
        updateStatus("Starting Single Player...");
        currentState = UIState.CHOOSING_CASE;
        singlePlayerGame = new SinglePlayerMain();
        showSinglePlayerCaseSelection();
    }

    private void showSinglePlayerCaseSelection() {
        List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
        VBox caseSelectionBox = new VBox(15);
        caseSelectionBox.setAlignment(Pos.CENTER);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Case ---\n");
        for (int i = 0; i < cases.size(); i++) {
            JsonDTO.CaseFile caseFile = cases.get(i);
            terminalTextArea.appendText((i + 1) + ". " + caseFile.getUniversalTitle() + "\n");
            Button caseButton = new Button(caseFile.getUniversalTitle());
            caseButton.setOnAction(event -> showSinglePlayerLanguageSelection(caseFile));
            caseSelectionBox.getChildren().add(caseButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("---------------------\n");

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(event -> {
            currentState = UIState.MENU;
            updateUIVisibility();
        });
        caseSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(caseSelectionBox);
    }

    public void showCaseSelectionMenu() {
        Platform.runLater(() -> {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
        });
    }

    private void handleCaseSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.MENU;
            updateUIVisibility();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
            if (choice > 0 && choice <= cases.size()) {
                showSinglePlayerLanguageSelection(cases.get(choice - 1));
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid case number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void showSinglePlayerLanguageSelection(JsonDTO.CaseFile caseFile) {
        this.selectedCaseFile = caseFile; // Store the selected case
        currentState = UIState.CHOOSING_LANGUAGE;
        VBox langSelectionBox = new VBox(15);
        langSelectionBox.setAlignment(Pos.CENTER);
        List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
        java.util.Collections.sort(langCodes);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
        for (int i = 0; i < langCodes.size(); i++) {
            String langCode = langCodes.get(i);
            String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
            terminalTextArea.appendText((i + 1) + ". " + langName + "\n");
            Button langButton = new Button(langName);
            langButton.setOnAction(event -> {
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(caseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            });
            langSelectionBox.getChildren().add(langButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("-------------------------------------\n");
        Button backButton = new Button("Back to Case Selection");
        backButton.setOnAction(event -> {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
        });
        langSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(langSelectionBox);
    }

    private void handleLanguageSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<String> langCodes = new java.util.ArrayList<>(selectedCaseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);

            if (choice > 0 && choice <= langCodes.size()) {
                String langCode = langCodes.get(choice - 1);
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(selectedCaseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid language number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void startMultiplayer() {
        isSinglePlayer = false;
        isHostPlayer = false; // Guest by default, updated by server
        updateStatus("Starting Multiplayer Client...");
        String host = getLaunchArg(0, NetworkConstants.DEFAULT_HOST);
        int port = getLaunchArg(1, NetworkConstants.DEFAULT_PORT);

        gameClient = new GameClient(host, port, this.taos);
        gameClient.setListener(this);

        gameClientThread = new Thread(() -> {
            try {
                gameClient.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                currentState = UIState.MENU;
                updateUIVisibility();
            }
        }, "GameClient-Thread");
        gameClientThread.setDaemon(true);
        gameClientThread.start();
        currentState = UIState.GAME_MULTI;
        // Don't call updateUIVisibility here, the listener will do it.
    }

    private void startServer() {
        updateStatus("Starting Game Server...");
        Thread serverThread = new Thread(() -> {
            try {
                ServerMain.main(launchArgs.toArray(new String[0]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        terminalTextArea.appendText("Server started in background. You can now start a multiplayer client.\n");
    }

    private String getLaunchArg(int index, String defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            return launchArgs.get(index);
        }
        return defaultValue;
    }

    private int getLaunchArg(int index, int defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            try {
                return Integer.parseInt(launchArgs.get(index));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    public void shutdown() {
        System.out.println("\nShutting down application...");
        if (gameClient != null) {
            gameClient.stopClient();
        }
        if (gameClientThread != null && gameClientThread.isAlive()) {
            try {
                gameClientThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleTerminalInput() {
        String input = terminalInputField.getText().trim();
        if (!input.isEmpty()) {
            if (currentState == UIState.GAME_SINGLE) {
                singlePlayerGame.processCommand(input);
            } else if (currentState == UIState.GAME_MULTI && gameClient != null) {
                if (currentMultiplayerSubState == UIMultiplayerSubState.MAIN_MENU && input.equals("3")) {
                    gameClient.stopClient();
                    if (gameClientThread != null) {
                        gameClientThread.interrupt(); // Interrupt the thread to unblock it
                    }
                } else {
                    gameClient.enqueueUserInput(input);
                }
            } else if (currentState == UIState.MENU) {
                handleMainMenuInput(input);
            } else if (currentState == UIState.CHOOSING_CASE) {
                handleCaseSelectionInput(input);
            } else if (currentState == UIState.CHOOSING_LANGUAGE) {
                handleLanguageSelectionInput(input);
            } else if (currentState == UIState.CASE_INVITATION && !isSinglePlayer) {
                gameClient.enqueueUserInput(input);
            }
            terminalInputField.clear();
        }
    }

    private void openTasksWindow() {
        if (tasksWindow == null) {
            tasksWindow = new TasksWindow();
        }
        tasksWindow.show();
    }

    private void openJournalWindow() {
        if (journalWindow == null) {
            journalWindow = new JournalWindow(this);
        }
        journalWindow.show();
    }

    private void openChatWindow() {
        if (chatWindow == null) {
            chatWindow = new ChatWindow(this);
        }
        chatWindow.show();
        unreadChatCount = 0;
        updateUnreadChatLabel();
    }

    private void openHelpWindow() {
        if (helpWindow == null) {
            helpWindow = new HelpWindow();
        }
        helpWindow.show();
    }

    public void incrementUnreadChat() {
        unreadChatCount++;
        updateUnreadChatLabel();
    }

    private void updateUnreadChatLabel() {
        if (unreadChatCount > 0) {
            unreadChatLabel.setText(String.valueOf(unreadChatCount));
            unreadChatLabel.setVisible(true);
        } else {
            unreadChatLabel.setVisible(false);
        }
    }

    public void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    public TextArea getTerminalTextArea() {
        return terminalTextArea;
    }

    public StackPane getRoomPane() {
        return roomPane;
    }

    public VBox getRightInfoPanel() {
        return rightInfoPanel;
    }

    public void sendCommand(String command) {
        if ((currentState == UIState.GAME_MULTI || (currentState == UIState.CASE_INVITATION && !isSinglePlayer)) && gameClient != null) {
            gameClient.enqueueUserInput(command);
        } else if (currentState == UIState.GAME_SINGLE && singlePlayerGame != null) {
            singlePlayerGame.processCommand(command);
        }
    }

    public GameClient getGameClient() {
        return gameClient;
    }

    public void updateRoomView(RoomDescriptionDTO roomDescription) {
        if (roomView != null && roomDescription != null) {
            Platform.runLater(() -> {
                roomView.loadRoom(roomDescription);
                updateRightPanel(roomDescription);
                updateStatus("Current room: " + roomDescription.getName());
            });
        }
    }

    private void updateRightPanel(RoomDescriptionDTO roomDescription) {
        // This can be expanded later.
    }

    public void showRoomResponse(String targetName, String response) {
        if (roomView != null) {
            Platform.runLater(() -> {
                roomView.showResponseBubble(targetName, response);
            });
        }
    }

    public void addJournalEntry(String entry) {
        if (journalWindow != null) {
            Platform.runLater(() -> {
                journalWindow.addEntry(entry);
            });
        }
    }

    public void addChatMessage(String sender, String message) {
        if (chatWindow != null) {
            Platform.runLater(() -> {
                chatWindow.addChatMessage(sender, message);
            });
        }
    }

    @Override
    public void onDisconnected() {
        currentMultiplayerSubState = UIMultiplayerSubState.DISCONNECTED;
        Platform.runLater(() -> {
            VBox disconnectedBox = new VBox(15);
            disconnectedBox.setAlignment(Pos.CENTER);
            Label label = new Label("Disconnected from server.");
            Button reconnectButton = new Button("Reconnect");
            reconnectButton.setOnAction(event -> sendCommand("connect"));
            disconnectedBox.getChildren().addAll(label, reconnectButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(disconnectedBox);
        });
    }

    @Override
    public void onConnecting() {
        currentMultiplayerSubState = UIMultiplayerSubState.CONNECTING;
        Platform.runLater(() -> {
            VBox connectingBox = new VBox(15);
            connectingBox.setAlignment(Pos.CENTER);
            Label label = new Label("Connecting to server...");
            connectingBox.getChildren().add(label);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(connectingBox);
        });
    }

    @Override
    public void onConnected() {
        // This will shortly be followed by onMainMenu
    }

    @Override
    public void onMainMenu() {
        currentMultiplayerSubState = UIMultiplayerSubState.MAIN_MENU;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Multiplayer Menu ---\n");
            terminalTextArea.appendText("1. Host Game\n");
            terminalTextArea.appendText("2. Join Game\n");
            terminalTextArea.appendText("3. Back to Main Menu\n");
            terminalTextArea.appendText("----------------------\n");
            VBox menuBox = new VBox(15);
            menuBox.setAlignment(Pos.CENTER);
            Button hostButton = new Button("Host Game");
            hostButton.setOnAction(event -> sendCommand("1"));
            Button joinButton = new Button("Join Game");
            joinButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back to Main Menu");
            backButton.setOnAction(event -> {
                gameClient.stopClient();
                if (gameClientThread != null) {
                    gameClientThread.interrupt();
                }
            });
            menuBox.getChildren().addAll(hostButton, joinButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(menuBox);
        });
    }

    @Override
    public void onHostGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.HOST_OPTIONS;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Host Game Options ---\n");
            terminalTextArea.appendText("1. Host Public Game\n");
            terminalTextArea.appendText("2. Host Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-------------------------\n");
            VBox hostOptionsBox = new VBox(15);
            hostOptionsBox.setAlignment(Pos.CENTER);
            Button publicButton = new Button("Host Public Game");
            publicButton.setOnAction(event -> sendCommand("1"));
            Button privateButton = new Button("Host Private Game");
            privateButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("3"));
            hostOptionsBox.getChildren().addAll(publicButton, privateButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(hostOptionsBox);
        });
    }

    @Override
    public void onCaseSelection(List<JsonDTO.CaseFile> cases) {
        currentMultiplayerSubState = UIMultiplayerSubState.CASE_SELECTION;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Case ---\n");
            VBox caseSelectionBox = new VBox(15);
            caseSelectionBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < cases.size(); i++) {
                final int caseNum = i + 1;
                String caseTitle = cases.get(i).getUniversalTitle();
                terminalTextArea.appendText(caseNum + ". " + caseTitle + "\n");
                Button caseButton = new Button(caseTitle);
                caseButton.setOnAction(event -> sendCommand(String.valueOf(caseNum)));
                caseSelectionBox.getChildren().add(caseButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("---------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            caseSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(caseSelectionBox);
        });
    }

    @Override
    public void onLanguageSelection(JsonDTO.CaseFile caseFile) {
        currentMultiplayerSubState = UIMultiplayerSubState.LANGUAGE_SELECTION;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
            VBox langSelectionBox = new VBox(15);
            langSelectionBox.setAlignment(Pos.CENTER);
            List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);
            for (int i = 0; i < langCodes.size(); i++) {
                final int langNum = i + 1;
                String langCode = langCodes.get(i);
                String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
                terminalTextArea.appendText(langNum + ". " + langName + "\n");
                Button langButton = new Button(langName);
                langButton.setOnAction(event -> sendCommand(String.valueOf(langNum)));
                langSelectionBox.getChildren().add(langButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("-------------------------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            langSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(langSelectionBox);
        });
    }

    @Override
    public void onHostingLobby(String gameCode) {
        currentMultiplayerSubState = UIMultiplayerSubState.HOSTING_LOBBY;
        Platform.runLater(() -> {
            VBox lobbyBox = new VBox(15);
            lobbyBox.setAlignment(Pos.CENTER);
            Label label = new Label("Waiting for another player to join...");
            if (gameCode != null) {
                Label codeLabel = new Label("Private Game Code: " + gameCode);
                lobbyBox.getChildren().add(codeLabel);
            }
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            lobbyBox.getChildren().addAll(label, cancelButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(lobbyBox);
        });
    }

    @Override
    public void onJoinGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.JOIN_OPTIONS;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Join Game Options ---\n");
            terminalTextArea.appendText("1. Join Public Game\n");
            terminalTextArea.appendText("2. Join Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-----------------------\n");
            VBox joinOptionsBox = new VBox(15);
            joinOptionsBox.setAlignment(Pos.CENTER);
            Button publicButton = new Button("Join Public Game");
            publicButton.setOnAction(event -> sendCommand("1"));
            Button privateButton = new Button("Join Private Game");
            privateButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("3"));
            joinOptionsBox.getChildren().addAll(publicButton, privateButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(joinOptionsBox);
        });
    }

    @Override
    public void onPublicGamesList(List<PublicGameInfoDTO> games) {
        currentMultiplayerSubState = UIMultiplayerSubState.PUBLIC_GAMES_LIST;
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Public Games ---\n");
            VBox gamesBox = new VBox(15);
            gamesBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < games.size(); i++) {
                final int gameNum = i + 1;
                PublicGameInfoDTO game = games.get(i);
                String gameInfo = game.getCaseTitle() + " hosted by " + game.getHostPlayerDisplayId();
                terminalTextArea.appendText(gameNum + ". " + gameInfo + "\n");
                Button gameButton = new Button(gameInfo);
                gameButton.setOnAction(event -> sendCommand(String.valueOf(gameNum)));
                gamesBox.getChildren().add(gameButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("--------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            gamesBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(gamesBox);
        });
    }

    @Override
    public void onPrivateGameEntry() {
        currentMultiplayerSubState = UIMultiplayerSubState.PRIVATE_GAME_ENTRY;
        Platform.runLater(() -> {
            VBox privateGameBox = new VBox(15);
            privateGameBox.setAlignment(Pos.CENTER);
            Label label = new Label("Enter Private Game Code:");
            TextField codeField = new TextField();
            codeField.setOnAction(event -> sendCommand(codeField.getText()));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("cancel"));
            privateGameBox.getChildren().addAll(label, codeField, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(privateGameBox);
        });
    }

    @Override
    public void onLobby() {
        currentMultiplayerSubState = UIMultiplayerSubState.IN_LOBBY;
    }

    @Override
    public void onEnterGame(RoomDescriptionDTO initialRoom) {
        isSinglePlayer = false;
        currentMultiplayerSubState = UIMultiplayerSubState.IN_GAME;
        currentState = UIState.GAME_MULTI;
        Platform.runLater(() -> {
            roomPane.getChildren().clear();
            roomPane.getChildren().add(roomView);
            tasksButton.setVisible(true);
            journalButton.setVisible(true);
            chatButton.setVisible(true);
            helpButton.setVisible(true);
            exitButton.setVisible(true);
            rightInfoPanel.setVisible(true);
            updateRoomView(initialRoom);
        });
    }

    @Override
    public void onUpdateRoom(RoomDescriptionDTO newRoom) {
        updateRoomView(newRoom);
    }

    @Override
    public void onReceiveCaseInvitation(String invitation, boolean isHost) {
        this.isHostPlayer = isHost;
        showCaseInvitation(invitation, isHost);
    }
}
