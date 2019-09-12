package yauhenipo.parser;

import org.apache.commons.lang3.exception.ExceptionUtils;
import yauhenipo.parser.driver.Browser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class RunTestNGResultsParserToXls {

    private static final ReportPage reportPage = new ReportPage();
    private static boolean isWindowRun = false;
    private static ExcelGenerator excelGenerator = new ExcelGenerator();
    private static final String EXCEL_EXTENSION = ".xlsx";

    public static void main(String[] args) throws IOException {
        String reportTestNGPath;
        try {
            if (args.length == 0) {
                isWindowRun = true;
                JFileChooser jFileChooser = viewFileChooser();
                File file = jFileChooser.getSelectedFile();
                reportTestNGPath = file.getAbsolutePath();
//                reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\emailable-report.html";
            } else {
                log.debug(String.format("Args values:\n%s", Arrays.toString(args)));
                reportTestNGPath = getDecodeAbsolutePath(args[0]);
            }

            log.info(String.format(">>>>>>   Report file path:   <<<<<<\n%s", reportTestNGPath));

            try {
                saveRemoteBasicReportFile(reportTestNGPath, getGenerateReportFile(getFileName(reportTestNGPath)).getAbsolutePath());
            } catch (Exception e) {
                String msg = String.format("ERROR of saving TestNG report:\nPATH:\n%s\nERROR:\n%s", reportTestNGPath, ExceptionUtils.getStackTrace(e));
                log.error(msg);
                viewAlert("File generation will continue after closing the window.\n" + msg);
                throw e;
            }
        } catch (Exception e) {
            String msg = ExceptionUtils.getStackTrace(e);
            log.error(msg);
            viewAlert(msg);
            throw e;
        }
        File generateFile = getGenerateExcelReportFile(reportTestNGPath);
        openDesktopFile(generateFile);
    }

    public static File getGenerateExcelReportFile(String reportTestNGPath) throws IOException {
        return getGenerateExcelReportFile(reportTestNGPath, getGenerateReportFile(reportTestNGPath, EXCEL_EXTENSION));
    }

    public static File getGenerateExcelReportFile(String reportTestNGPath, String generateReportPath, String generateReportName) throws IOException {
        return getGenerateExcelReportFile(reportTestNGPath, new File(
                Paths.get(getDecodeAbsolutePath(generateReportPath), generateReportName + EXCEL_EXTENSION).toString()));
    }

    public static File getGenerateExcelReportFile(String reportTestNGPath, File generateFile) throws IOException {
        String message = null;
        try {
            Browser.getInstance();
            Browser.openUrl(reportTestNGPath);
            List<String> failedTestsNames = reportPage.getFailedTestsNames();
            List<String> failedTestsStacktrace = reportPage.getFailedTestsStacktrace();
            fetchReportExcelSheet(failedTestsNames, failedTestsStacktrace);
            Map<String, List<String>> reportSummary = SummaryReport.groupingTestsFailed(failedTestsNames, failedTestsStacktrace);
            fetchSummaryExcelSheet(reportSummary);
            excelGenerator.createFile(generateFile);
            message = String.format("Excel file PATH:\n%s", generateFile.getPath());
            log.info(message);
        } catch (FileNotFoundException fileNotFoundException) {
            message = ExceptionUtils.getStackTrace(fileNotFoundException);
        } catch (Exception e) {
            message = ExceptionUtils.getStackTrace(e);
            log.error(message);
            throw e;
        } finally {
            Browser.getInstance().exit();
            viewAlert(message);
        }
        return generateFile;
    }

    private static void fetchReportExcelSheet(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        excelGenerator.writeDataToExcelSheet("report", failedTestsNames, failedTestsStacktrace);
    }

    private static void fetchSummaryExcelSheet(Map<String, List<String>> mapTests) {
        Map<String, List<String>> sortedMapTests = getSortingSummaryMapByTestsFailed(mapTests);
        List<String> failedMethods = new ArrayList<>(sortedMapTests.keySet());
        List<String> countFailed = new ArrayList<>();
        List<String> columnTestsCells = new ArrayList<>();
        for (String failedMethod : failedMethods) {
            countFailed.add(String.valueOf(sortedMapTests.get(failedMethod).size()));
            List<String> testsList = sortedMapTests.get(failedMethod);
            columnTestsCells.add(String.join("\r\n", testsList));
        }
        excelGenerator.writeDataToExcelSheet("summary", countFailed, failedMethods, columnTestsCells);
    }

    private static Map<String, List<String>> getSortingSummaryMapByTestsFailed(Map<String, List<String>> mapTests) {
        return mapTests.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));
    }

    private static void openDesktopFile(File generateFile) {
        if (isWindowRun && Objects.requireNonNull(generateFile).exists()) {
            try {
                Desktop.getDesktop().open(generateFile);
            } catch (IOException e) {
                String msg = ExceptionUtils.getStackTrace(e);
                log.error(msg);
                viewAlert(String.format("Open desktop file:\n%s", msg));
            }
        }
    }

    private static void saveRemoteBasicReportFile(String originalFilePath, String copyFilePath) throws IOException {
        if (!originalFilePath.contains(":") || originalFilePath.startsWith("file:")) {
            FileUtils.copyFile(new File(originalFilePath), new File(copyFilePath));
        }
    }

    private static JFileChooser viewFileChooser() throws UnsupportedEncodingException {
        JFileChooser jFileChooser = new JFileChooser() {

            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                dialog.setLocationByPlatform(true);
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
            JOptionPane pane = new JOptionPane();
            pane.setMessage(message);
            JDialog dialog = pane.createDialog("Message");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
        }
    }

    public static String getDecodeAbsolutePath(String sourcePath) throws UnsupportedEncodingException {
        return decode(new File(sourcePath).getAbsolutePath());
    }

    private static String getSourcePath() throws UnsupportedEncodingException {
        return getDecodeAbsolutePath(RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    public static String decode(String path) throws UnsupportedEncodingException {
            return URLDecoder.decode(path, "UTF-8");
    }

    private static String getGenerateReportFileName(String reportTestNGPath, String extension) {
        return FilenameUtils.removeExtension(getFileName(reportTestNGPath)) + extension;
    }

    private static String getFileName(String file) {
        return FilenameUtils.getName(file);
    }

    private static File getGenerateReportFile(String generateFileName) throws UnsupportedEncodingException {
        return new File(Paths.get(getParentFilePath(), generateFileName).toString());
    }

    private static File getGenerateReportFile(String reportTestNGPath, String extension) throws UnsupportedEncodingException {
        return new File(Paths.get(getParentFilePath(), getGenerateReportFileName(reportTestNGPath, extension)).toString());
    }

    private static String getParentFilePath() throws UnsupportedEncodingException {
        return new File(getSourcePath()).getParent();
    }
}
