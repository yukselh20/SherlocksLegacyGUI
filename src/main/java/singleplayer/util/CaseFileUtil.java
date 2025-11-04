package singleplayer.util;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import singleplayer.SinglePlayerMain;

public class CaseFileUtil {

  private CaseFileUtil() {}

  public static void addCaseFromFile(String filePath) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      File sourceFile = new File(filePath);
      if (!sourceFile.exists()) {
        System.out.println("ADD_CASE_ERROR: Source file not found: " + filePath);
        return;
      }
      if (!sourceFile.isFile()) {
        System.out.println("ADD_CASE_ERROR: Source path is not a regular file: " + filePath);
        return;
      }
      if (!filePath.toLowerCase().endsWith(".json")) {
        System.out.println("ADD_CASE_ERROR: Source file must be a .json file.");
        return;
      }

      CaseFile newCaseContent;
      try {
        newCaseContent = mapper.readValue(sourceFile, CaseFile.class);
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        System.out.println("ADD_CASE_ERROR: Could not parse the JSON in '" + sourceFile.getName() + "'. Ensure it's valid JSON.");
        return;
      }

      // MODIFIED: Use getUniversalTitle() for validation
      if (newCaseContent.getUniversalTitle() == null || newCaseContent.getUniversalTitle().trim().isEmpty()) {
        System.out.println("ADD_CASE_ERROR: The case file '" + sourceFile.getName() + "' is missing a valid universal_title.");
        return;
      }

      // MODIFIED: Use getUniversalTitle() for duplicate checking
      List<CaseFile> existingCases = loadExistingCasesFromCasesDir();
      for (CaseFile existingCase : existingCases) {
        if (existingCase.getUniversalTitle().equalsIgnoreCase(newCaseContent.getUniversalTitle())) {
          System.out.println(
                  "ADD_CASE_ERROR: A case titled '"
                          + newCaseContent.getUniversalTitle()
                          + "' already exists in the '"
                          + SinglePlayerMain.CASES_DIRECTORY
                          + "' directory.");
          return;
        }
      }

      File targetCasesFolder = new File(SinglePlayerMain.CASES_DIRECTORY);
      if (!targetCasesFolder.exists()) {
        if (!targetCasesFolder.mkdirs()) {
          System.out.println("ADD_CASE_ERROR: Could not create target 'cases' directory at: " + targetCasesFolder.getAbsolutePath());
          return;
        }
        System.out.println("ADD_CASE_INFO: Created 'cases' directory: " + targetCasesFolder.getAbsolutePath());
      }
      if (!targetCasesFolder.isDirectory()) {
        System.out.println("ADD_CASE_ERROR: Target path '" + targetCasesFolder.getAbsolutePath() + "' is not a directory.");
        return;
      }

      String originalFileName = sourceFile.getName();
      File destinationFile = new File(targetCasesFolder, originalFileName);
      int counter = 1;
      while (destinationFile.exists()) {
        String namePart = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extPart = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String newFileName = namePart + "_" + counter + extPart;
        destinationFile = new File(targetCasesFolder, newFileName);
        counter++;
      }

      Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

      // MODIFIED: Use getUniversalTitle() in the success message
      System.out.println("ADD_CASE_SUCCESS: Case '" + newCaseContent.getUniversalTitle() + "' added.");
      System.out.println(
              "                  Saved as: "
                      + destinationFile.getName()
                      + " in '"
                      + targetCasesFolder.getName()
                      + "' directory.");

    } catch (IOException e) {
      System.out.println("ADD_CASE_IO_ERROR: An error occurred during file operation: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("ADD_CASE_UNEXPECTED_ERROR: An unexpected error occurred: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static List<CaseFile> loadExistingCasesFromCasesDir() {
    ObjectMapper mapper = new ObjectMapper();
    File folder = new File(SinglePlayerMain.CASES_DIRECTORY);
    List<CaseFile> cases = new ArrayList<>();

    if (folder.exists() && folder.isDirectory()) {
      File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
      if (files != null) {
        for (File file : files) {
          try {
            CaseFile caseFile = mapper.readValue(file, CaseFile.class);
            // MODIFIED: Use getUniversalTitle() to validate if a case is usable
            if (caseFile.getUniversalTitle() != null && !caseFile.getUniversalTitle().trim().isEmpty()) {
              cases.add(caseFile);
            } else {
              System.out.println("LOAD_CASES_WARN: Skipping case file '" + file.getName() + "' (missing universal_title).");
            }
          } catch (Exception e) {
            System.out.println(
                    "LOAD_CASES_ERROR: Error parsing existing case file '"
                            + file.getName()
                            + "': "
                            + e.getMessage());
          }
        }
      }
    }
    return cases;
  }
}