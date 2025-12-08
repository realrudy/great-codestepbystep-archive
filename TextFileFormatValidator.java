import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TextFileFormatValidator {
    private static final String[] REQUIRED_SECTIONS = {
        "URL",
        "PROBLEM_NAME",
        "LANGUAGE_TYPE",
        "DESCRIPTION",
        "TESTS_START",
        "TESTS_END",
        "SOLUTION"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter folder path to scan: ");
        String folderPath = scanner.nextLine();
        scanner.close();

        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.out.println("Error: Invalid folder path");
            return;
        }

        List<File> txtFiles = getTxtFiles(folder);
        System.out.println("\nFound " + txtFiles.size() + " text files.\n");

        int validCount = 0;
        for (File file : txtFiles) {
            ValidationResult result = validateFile(file);
            if (result.isValid) {
                System.out.println("✓ " + file.getName());
                validCount++;
            } else {
                System.out.println("✗ " + file.getName());
                System.out.println("  Issues: " + result.issues);
                System.out.println();
            }
        }

        System.out.println("\n" + validCount + "/" + txtFiles.size() + " files are properly formatted");
    }

    private static List<File> getTxtFiles(File folder) {
        List<File> txtFiles = new ArrayList<>();
        try {
            Files.walk(Paths.get(folder.getAbsolutePath()))
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(path -> txtFiles.add(path.toFile()));
        } catch (IOException e) {
            System.out.println("Error reading folder: " + e.getMessage());
        }
        return txtFiles;
    }

    private static ValidationResult validateFile(File file) {
        ValidationResult result = new ValidationResult();
        try {
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");

            int sectionIndex = 0;
            boolean hasUrl = false;
            boolean hasName = false;
            boolean hasLanguageType = false;
            boolean inTests = false;
            boolean hasTests = false;
            boolean hasSolution = false;
            int separatorCount = 0;

            for (String line : lines) {
                line = line.trim();

                if (line.isEmpty()) continue;

                // Check for URL (should be first non-empty line)
                if (!hasUrl && line.startsWith("http")) {
                    hasUrl = true;
                    continue;
                }

                // Check for Problem Name
                if (hasUrl && !hasName && !line.contains("Language") && !line.contains("LANGUAGE")) {
                    hasName = true;
                    continue;
                }

                // Check for Language/Type
                if (hasName && !hasLanguageType && line.contains("Language") && line.contains(":")) {
                    hasLanguageType = true;
                    continue;
                }

                // Check for separators ~~
                if (line.equals("~~")) {
                    separatorCount++;
                    if (separatorCount == 1) {
                        inTests = true;
                    } else if (separatorCount == 2) {
                        inTests = false;
                        hasSolution = true;
                    }
                    continue;
                }

                // Track if we have test content
                if (inTests && line.startsWith("TEST #")) {
                    hasTests = true;
                }

                // Check for solution code
                if (hasSolution && (line.contains("public ") || line.contains("private "))) {
                    hasSolution = true;
                }
            }

            // Validate all required sections exist
            if (!hasUrl) {
                result.issues = "Missing URL section; ";
            }
            if (!hasName) {
                result.issues += "Missing problem name; ";
            }
            if (!hasLanguageType) {
                result.issues += "Missing Language/Type section; ";
            }
            if (separatorCount < 2) {
                result.issues += "Missing ~~ separators; ";
            }
            if (!hasTests) {
                result.issues += "Missing test cases; ";
            }
            if (!hasSolution) {
                result.issues += "Missing solution code; ";
            }

            result.isValid = result.issues.isEmpty();

        } catch (IOException e) {
            result.issues = "Error reading file: " + e.getMessage();
            result.isValid = false;
        }

        return result;
    }

    static class ValidationResult {
        boolean isValid = true;
        String issues = "";
    }
}
