package yauhenipo.parser;

import yauhenipo.parser.driver.Browser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class RunTestNGResultsParserToXls {

    private static final ReportPage reportPage = new ReportPage();
    private static boolean isWindowRun = false;
    private static ExcelGenerator excelGenerator = new ExcelGenerator();
    private static final String EXCEL_EXTENSION = "xlsx";

    public static void main(String[] args) {
        String reportTestNGPath = null;
        try {
            if (args.length == 0) {
                isWindowRun = true;
                JFileChooser jFileChooser = viewFileChooser();
                File file = jFileChooser.getSelectedFile();
                //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
                //закомментить
                reportTestNGPath = file.getAbsolutePath();
                //раскомментить
//                reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\emailable-report.html";
            } else {
                log.debug(String.format("args values: %s", Arrays.asList(args).toString()));
                reportTestNGPath = getDecodeAbsolutePath(args[0]);
            }

            log.info(String.format(">>>>>>   Report file path:   <<<<<<\n%s", reportTestNGPath));

            try {
                saveRemoteBasicReportFile(reportTestNGPath, getGenerateReportFile(getFileName(reportTestNGPath)).getAbsolutePath());
            } catch (Exception e) {
                String msg = String.format("ERROR of saving TestNG report:\nPATH:\n%s\nERROR:\n%s", reportTestNGPath, e.getMessage());
                log.error(msg);
                viewAlert("File generation will continue after closing the window.\n" + msg);
            }
        } catch (Exception e) {
            log.error(e);
            viewAlert(Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
            e.printStackTrace();
        }
        File generateFile = getGenerateExcelReportFile(reportTestNGPath);
        openDesktopFile(generateFile);
    }

    public static File getGenerateExcelReportFile(String reportTestNGPath) {
        File generateFile = null;
        String message = null;
        try {
            Browser.getInstance();
            Browser.openUrl(reportTestNGPath);
            List<String> failedTestsNames = reportPage.getFailedTestsNames();
            List<String> failedTestsStacktrace = reportPage.getFailedTestsStacktraces();
            generateFile = getGenerateReportFile(reportTestNGPath, failedTestsNames.size(), failedTestsStacktrace.size(), EXCEL_EXTENSION);
            fetchReportExcelSheet(failedTestsNames, failedTestsStacktrace);
            Map<String, List<String>> reportSummary = SummaryReport.groupingTestsFailed(failedTestsNames, failedTestsStacktrace);
            fetchSummaryExcelSheet(reportSummary);
            excelGenerator.createFile(generateFile);
            message = String.format("Excel file PATH:\n%s", generateFile.getPath());
        } catch (Exception e) {
            message = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
            log.error(e);
            e.printStackTrace();
        } finally {
            log.info(message);
            Browser.getInstance().exit();
            viewAlert(message);
        }
        return generateFile;
    }

    private static void openDesktopFile(File generateFile) {
        if (isWindowRun && Objects.requireNonNull(generateFile).exists()) {
            try {
                Desktop.getDesktop().open(generateFile);
            } catch (IOException e) {
                log.error(e);
                viewAlert(String.format("Open desktop file: %s", e.getMessage()));
            }
        }
    }

    private static void saveRemoteBasicReportFile(String originalFilePath, String copyFilePath) throws IOException {
        if (!originalFilePath.contains(":")) {
            FileUtils.copyFile(new File(originalFilePath), new File(copyFilePath));
        }
    }

    private static JFileChooser viewFileChooser() {
        JFileChooser jFileChooser = new JFileChooser() {

            @Override
            protected JDialog createDialog(Component parent)
                    throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                // config here as needed - just to see a difference
                dialog.setLocationByPlatform(true);
                // might help - can't know because I can't reproduce the problem
                dialog.setAlwaysOnTop(true);
                return dialog;
            }
        };
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getSourcePath()));
        jFileChooser.showOpenDialog(null);
        return jFileChooser;
    }

    static void viewAlert(String message) {
        if (isWindowRun) {
            JOptionPane.showMessageDialog(null, message);
        }
    }

    public static String getDecodeAbsolutePath(String sourcePath) {
        return decode(new File(sourcePath).getAbsolutePath());
    }

    private static String getSourcePath() {
        return getDecodeAbsolutePath(RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    public static String decode(String path) {
        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            viewAlert(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static void fetchReportExcelSheet(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        excelGenerator.writeFileSheet("report", failedTestsNames, failedTestsStacktrace);
    }

    private static void fetchSummaryExcelSheet(Map<String, List<String>> mapTests) {
        Map<String, List<String>> sortedMapTests = mapTests.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));
        List<String> failedMethods = new ArrayList<>(sortedMapTests.keySet());
        List<String> countFailed = new ArrayList<>();
        List<String> columnTestsCells = new ArrayList<>();
        for (String failedMethod : failedMethods) {
            String columnTestsCell = "";
            countFailed.add(String.valueOf(sortedMapTests.get(failedMethod).size()));
            List<String> testsList = sortedMapTests.get(failedMethod);
            for (String test : testsList) {
                columnTestsCell = columnTestsCell.concat(test).concat("\r\n");
            }
            columnTestsCells.add(columnTestsCell);
        }
        excelGenerator.writeFileSheet("summary", countFailed, failedMethods, columnTestsCells);
    }

    private static String getGenerateReportFileName(String reportTestNGPath, int failedTestsNamesCount, int failedTestsStacktraceCount, String extension) {
        return String.format("%s_%dTests_%dStacktrace.%s",
                FilenameUtils.removeExtension(getFileName(reportTestNGPath)),
                failedTestsNamesCount,
                failedTestsStacktraceCount,
                extension);
    }

    private static String getFileName(String file) {
        return FilenameUtils.getName(file);
    }

    private static File getGenerateReportFile(String generateFileName) {
        return new File(getParentFilePath() + File.separator + generateFileName);
    }

    private static File getGenerateReportFile(String reportTestNGPath, int failedTestsNamesCount, int failedTestsStacktraceCount, String extension) {
        return new File(getParentFilePath() + File.separator + getGenerateReportFileName(reportTestNGPath, failedTestsNamesCount, failedTestsStacktraceCount, extension));
    }

    private static String getParentFilePath() {
        return new File(getSourcePath()).getParent();
    }
}
