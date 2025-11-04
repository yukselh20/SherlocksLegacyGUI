package common.commands;

import common.interfaces.GameActionContext;
import java.io.Serial;

public class RequestInitiateExamCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;

  public RequestInitiateExamCommand() {
    super(true);
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    context.processRequestInitiateExam(getPlayerId());
  }

  @Override
  public String getDescription() {
    return "Requests the host to initiate the final exam.";
  }
}