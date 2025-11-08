package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.commands.Command;
import common.dto.ChatMessage;
import common.dto.LobbyUpdateDTO;
import common.dto.ReturnToLobbyDTO;
import common.dto.TextMessage;
import JsonDTO.CaseData;

public class GameSession {
  private static final Logger logger = LoggerFactory.getLogger(GameSession.class);
  private final String sessionId;
  private final GameContextServer gameContext;
  private ClientSession player1;
  private ClientSession player2;
  private final ReentrantLock sessionLock = new ReentrantLock();
  private GameSessionState state;
  private String gameCode;
  private final GameSessionManager sessionManager;
  private final GameServer server;
  private final CaseData caseFile;


  public GameSession(CaseData caseFile, ClientSession hostPlayer, boolean isPublic, String assignedGameCode, GameSessionManager manager, GameServer server) {
    this.sessionId = UUID.randomUUID().toString();
    this.caseFile = Objects.requireNonNull(caseFile, "CaseData object cannot be null"); // MODIFIED
    this.sessionManager = Objects.requireNonNull(manager, "GameSessionManager cannot be null");
    this.server = Objects.requireNonNull(server, "GameServer cannot be null");
    this.state = GameSessionState.LOADING;
    this.player1 = Objects.requireNonNull(hostPlayer, "Host player (player1) cannot be null");
    hostPlayer.setAssociatedGameSession(this);
    if (!isPublic) {
      this.gameCode = Objects.requireNonNull(assignedGameCode, "Private game session must be created with an assigned game code.");
    } else {
      this.gameCode = null;
    }

    // This line now causes the SECOND error, which we will fix next.
    // We pass `this.caseFile` which is a CaseData object.
    this.gameContext = new GameContextServer(this, this.caseFile, hostPlayer.getPlayerId(), null);

    if (!loadCaseDataIntoContext()) {
      this.state = GameSessionState.ERROR;
      log("CRITICAL: Failed to load case data for new session " + sessionId);
      hostPlayer.send(new TextMessage("Error: Failed to initialize the game data for this case. Session cannot start.", true));
    } else {
      this.state = GameSessionState.WAITING_FOR_PLAYERS;
      log("Session created for case '" + this.caseFile.getTitle() + "'. Host: " + hostPlayer.getDisplayId() + ". Waiting for Player 2.");
    }
  }

  private void log(String message) {
    // Formats the message with the session's unique ID for easy tracking in logs
    logger.info("[SESS:{}] {}", this.sessionId.substring(0, 8), message);
  }

  private boolean loadCaseDataIntoContext() {
    log("Loading case data into context...");
    gameContext.resetForNewCaseLoad();
    try {
      if (!extractors.BuildingExtractor.loadBuilding(this.caseFile, this.gameContext)) {
        log("Failed to load building data.");
        return false;
      }
      extractors.GameObjectExtractor.loadObjects(this.caseFile, this.gameContext);
      extractors.SuspectExtractor.loadSuspects(this.caseFile, this.gameContext);
    } catch (Exception e) {
      log("Unexpected error during case data loading: " + e.getMessage());
      server.logError("Stack trace for unexpected loading error in session " + sessionId, e);
      return false;
    }
    gameContext.initializePlayerStartingState();
    log("Case data loaded successfully.");
    return true;
  }

  public boolean addPlayer(ClientSession newPlayer) {
    sessionLock.lock();
    try {
      if (player2 != null) {
        newPlayer.send(new common.dto.JoinGameResponseDTO(false, "Session is already full.", null));
        return false;
      }
      if (this.state != GameSessionState.WAITING_FOR_PLAYERS) {
        newPlayer.send(new common.dto.JoinGameResponseDTO(false, "Session not currently accepting new players.", null));
        return false;
      }
      if (player1 != null && player1.getPlayerId().equals(newPlayer.getPlayerId())) {
        newPlayer.send(new common.dto.JoinGameResponseDTO(false, "You cannot join your own game as the second player.", null));
        return false;
      }
      player2 = newPlayer;
      newPlayer.setAssociatedGameSession(this);
      log("Player 2 (" + player2.getDisplayId() + ") joined session.");
      this.gameContext.setPlayerIds(player1.getPlayerId(), player2.getPlayerId());

      newPlayer.send(new common.dto.JoinGameResponseDTO(true, "Joined game: " + this.caseFile.getTitle() + " with host " + player1.getDisplayId(), this.sessionId));

      startGameSession();
      return true;
    } finally {
      sessionLock.unlock();
    }
  }

  private void startGameSession() {
    setSessionState(GameSessionState.IN_LOBBY_AWAITING_START);
    log("Session is now " + this.state + ". Players: " + player1.getDisplayId() + ", " + player2.getDisplayId());
    LobbyUpdateDTO gameReadyMsg = new LobbyUpdateDTO(
            "Both players are in the lobby.",
            new ArrayList<>(getPlayerDisplayIds()),
            new ArrayList<>(getPlayerActualIds()),
            (player1 != null ? player1.getPlayerId() : null),
            true,
            caseFile.getInvitation());
    broadcast(gameReadyMsg, null);
    broadcast(new TextMessage("--- Case Invitation ---\n" + caseFile.getInvitation() + "\n\nHost (" + player1.getDisplayId() + ") should type 'start case' to begin.", false), null);
  }


  public void handlePlayerDisconnect(ClientSession disconnectedClient) {
    sessionLock.lock();
    try {
      String leavingPlayerId = disconnectedClient.getPlayerId();
      String leavingPlayerDisplayId = disconnectedClient.getDisplayId();
      boolean wasP1 = player1 != null && player1.getPlayerId().equals(leavingPlayerId);
      boolean wasP2 = player2 != null && player2.getPlayerId().equals(leavingPlayerId);

      if (!wasP1 && !wasP2) return;

      log("Player " + leavingPlayerDisplayId + " (ID: " + leavingPlayerId + ") has disconnected.");
      disconnectedClient.setAssociatedGameSession(null);

      if (wasP1) { // HOST DISCONNECTED
        log("Host has disconnected. Ending session " + sessionId);
        if (player2 != null) {
          player2.send(new TextMessage("The host (" + leavingPlayerDisplayId + ") has disconnected. The session has ended.", false));
          player2.send(new ReturnToLobbyDTO("Returning to main menu as host has left."));
          player2.setAssociatedGameSession(null);
        }
        sessionManager.endSession(this.sessionId, "Host disconnected.");
      } else if (wasP2) { // GUEST DISCONNECTED
        log("Guest has disconnected. Session " + sessionId + " continues for host.");
        this.player2 = null;
        if (gameContext != null) {
          gameContext.setPlayerIds(player1.getPlayerId(), null);
        }
        if (this.state == GameSessionState.ACTIVE && player1 != null) {
          player1.send(new TextMessage(leavingPlayerDisplayId + " has left the game. You may continue your investigation solo.", false));
        } else if (this.state == GameSessionState.IN_LOBBY_AWAITING_START && player1 != null) {
          this.state = GameSessionState.WAITING_FOR_PLAYERS;
          player1.send(new TextMessage(leavingPlayerDisplayId + " has left the lobby. Waiting for a new player...", false));
          log("Session " + sessionId + " is now back to WAITING_FOR_PLAYERS.");
        }
      }
    } finally {
      sessionLock.unlock();
    }
  }


  public void playerRequestsExit(String playerId) {
    sessionLock.lock();
    try {
      ClientSession exitingPlayer = getClientSessionById(playerId);
      if (exitingPlayer == null) {
        log("PlayerRequestsExit called for a player ID not in this session: " + playerId);
        return; // Player not found or already gone.
      }

      log("Player " + exitingPlayer.getDisplayId() + " has requested to exit the game.");

      boolean wasHost = (player1 != null && player1.getPlayerId().equals(playerId));

      // SCENARIO 1: The Host is exiting. The session must end for everyone.
      if (wasHost) {
        log("Host is exiting. The session will be terminated.");
        if (player2 != null) {
          player2.send(new TextMessage("The host has ended the game session.", false));
          player2.send(new ReturnToLobbyDTO("Returning to main menu."));
          player2.setAssociatedGameSession(null);
        }
        player1.send(new ReturnToLobbyDTO("You have left the game."));
        player1.setAssociatedGameSession(null);
        sessionManager.endSession(this.sessionId, "Host exited the game.");
      }
      // SCENARIO 2: The Guest is exiting. The host can continue.
      else {
        log("Guest (" + exitingPlayer.getDisplayId() + ") is exiting. Host will remain.");
        exitingPlayer.send(new ReturnToLobbyDTO("You have left the game."));
        exitingPlayer.setAssociatedGameSession(null);
        this.player2 = null;
        if (gameContext != null) {
          gameContext.setPlayerIds(player1.getPlayerId(), null);
        }
        if (player1 != null) {
          player1.send(new TextMessage(exitingPlayer.getDisplayId() + " has left the lobby.", false));
          if (this.state == GameSessionState.ACTIVE) {
            player1.send(new TextMessage("You may continue your investigation solo.", false));
          } else if (this.state == GameSessionState.IN_LOBBY_AWAITING_START) {
            setSessionState(GameSessionState.WAITING_FOR_PLAYERS);
            log("Session is now back to WAITING_FOR_PLAYERS.");
            player1.send(new TextMessage("Waiting for a new player...", false));
            if (this.gameCode == null) { // This session is public
              sessionManager.relistPublicLobby(this);
            }
          }
        }
      }
    } finally {
      sessionLock.unlock();
    }
  }

  public void processCommand(Command command, String playerId) {
    sessionLock.lock();
    try {
      command.setPlayerId(playerId);
      boolean commandAllowed = false;
      if (this.state == GameSessionState.ACTIVE) {
        commandAllowed = true;
      } else if (this.state == GameSessionState.WAITING_FOR_PLAYERS || this.state == GameSessionState.IN_LOBBY_AWAITING_START) {
        // Whitelist of commands allowed in any lobby state
        if (command instanceof common.commands.StartCaseCommand
                || command instanceof common.commands.RequestStartCaseCommand
                || command instanceof common.commands.ExitCommand
                || command instanceof common.commands.CancelLobbyCommand) { // Our new command is now included
          commandAllowed = true;
        }
      }

      if (commandAllowed) {
        gameContext.executeCommand(command);
      } else {
        ClientSession sender = getClientSessionById(playerId);
        if (sender != null) {
          sender.send(new TextMessage("Command not allowed in current session state: " + this.state, true));
        }
      }
    } finally {
      sessionLock.unlock();
    }
  }

  public void processChatMessage(ChatMessage chatMessage) {
    sessionLock.lock();
    try {
      if (this.state == GameSessionState.ACTIVE || this.state == GameSessionState.IN_LOBBY_AWAITING_START) {
        broadcast(chatMessage, null);
      }
    } finally {
      sessionLock.unlock();
    }
  }

  public void broadcast(Serializable dto, String excludePlayerId) {
    if (player1 != null && (excludePlayerId == null || !player1.getPlayerId().equals(excludePlayerId))) {
      player1.send(dto);
    }
    if (player2 != null && (excludePlayerId == null || !player2.getPlayerId().equals(excludePlayerId))) {
      player2.send(dto);
    }
  }

  public void endSession(String reason) {
    this.state = GameSessionState.ENDED_ABANDONED; // Or other end state
    // Notify players, clean up, etc.
  }

  public void notifyNameChangeToManagerIfHost(String updatedPlayerId, String newDisplayName) {
    sessionLock.lock();
    try {
      if (player1 != null && player1.getPlayerId().equals(updatedPlayerId) && sessionManager != null && gameCode == null) {
        sessionManager.updatePublicGameHostName(this.sessionId, newDisplayName);
      }
    } finally {
      sessionLock.unlock();
    }
  }

  // --- Getters and Helpers ---
  public String getSessionId() { return sessionId; }
  public String getCaseTitle() {
    return caseFile.getTitle();
  }
  public String getGameCode() { return gameCode; }
  public GameSessionState getState() { return state; }
  public void setSessionState(GameSessionState state) { this.state = state; }
  public GameContextServer getGameContext() { return gameContext; }
  public GameServer getServer() { return server; }
  public ClientSession getPlayer1() { return player1; }
  public boolean isFull() { return player1 != null && player2 != null; }
  public List<String> getPlayerDisplayIds() {
    List<String> ids = new ArrayList<>();
    if (player1 != null) ids.add(player1.getDisplayId());
    if (player2 != null) ids.add(player2.getDisplayId());
    return ids;
  }
  public List<String> getPlayerActualIds() {
    List<String> ids = new ArrayList<>();
    if (player1 != null) ids.add(player1.getPlayerId());
    if (player2 != null) ids.add(player2.getPlayerId());
    return ids;
  }
  public ClientSession getClientSessionById(String playerId) {
    if (playerId == null) return null;
    if (player1 != null && player1.getPlayerId().equals(playerId)) return player1;
    if (player2 != null && player2.getPlayerId().equals(playerId)) return player2;
    return null;
  }
  public ClientSession getOtherPlayer(String currentPlayerId) {
    if (currentPlayerId == null) return null;
    if (player1 != null && player1.getPlayerId().equals(currentPlayerId)) return player2;
    if (player2 != null && player2.getPlayerId().equals(currentPlayerId)) return player1;
    return null;
  }



public void playerCancelsLobby(String playerId) {
  sessionLock.lock();
  try {
      // First, verify this command is valid for the current state.
      if (this.state != GameSessionState.WAITING_FOR_PLAYERS
              && this.state != GameSessionState.IN_LOBBY_AWAITING_START) {
          ClientSession player = getClientSessionById(playerId);
          if (player != null) {
              player.send(new TextMessage("Error: The 'cancel' command can only be used in a pre-game lobby.", true));
          }
          return;
      }

      ClientSession cancellingPlayer = getClientSessionById(playerId);
      if (cancellingPlayer == null) {
          return; // Player already gone.
      }

      boolean isHost = player1 != null && player1.equals(cancellingPlayer);

      // --- SCENARIO 1: HOST is cancelling. ---
      // The entire session ends for everyone.
      if (isHost) {
          log("Host is cancelling the lobby. Session will be terminated.");

          // Notify the guest (if they exist) that the session is over.
          if (player2 != null) {
              player2.send(new TextMessage("The host has cancelled the lobby.", false));
              player2.send(new ReturnToLobbyDTO("Returning to main menu."));
              player2.setAssociatedGameSession(null);
          }

          // Notify the host themselves.
          player1.send(new ReturnToLobbyDTO("You have left the lobby."));
          player1.setAssociatedGameSession(null);

          // Tell the manager to destroy this session object.
          sessionManager.endSession(this.sessionId, "Lobby cancelled by host.");

      }
      // --- SCENARIO 2: GUEST is cancelling. ---
      // The host remains and the lobby becomes available again.
      else {
          log("Guest (" + cancellingPlayer.getDisplayId() + ") is leaving the lobby. Host will remain.");

          // Notify the guest they are returning to the menu.
          cancellingPlayer.send(new ReturnToLobbyDTO("You have left the lobby."));
          cancellingPlayer.setAssociatedGameSession(null);

          // Remove the guest from the session state.
          this.player2 = null;
          if (gameContext != null) {
              gameContext.setPlayerIds(player1.getPlayerId(), null);
          }

          // Notify the host and revert the session state.
          if (player1 != null) {
              setSessionState(GameSessionState.WAITING_FOR_PLAYERS);
              log("Session is now back to WAITING_FOR_PLAYERS.");
              player1.send(new TextMessage(cancellingPlayer.getDisplayId() + " has left the lobby. Waiting for a new player...", false));
              
              // If it was a public game, re-list it in the manager.
              if (this.gameCode == null) {
                  sessionManager.relistPublicLobby(this);
              }
          }
      }
  } finally {
      sessionLock.unlock();
  }
}


}
