package yauhenipo.parser;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
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

    private static boolean isWindowRun = false;
    private static ExcelGenerator excelGenerator = new ExcelGenerator();

    public static void main(String[] args) {
        String msg = null;
        String reportTestNGPath;
        try {
            if (args.length == 0) {
                isWindowRun = true;
                JFileChooser jFileChooser = viewFileChooser();
                File file = jFileChooser.getSelectedFile();
                if (file == null) {
                    throw new NullPointerException(">>>>>>   Report file does not identify   <<<<<<");
                }
                reportTestNGPath = file.getAbsolutePath();
                try {
                    saveRemoteBasicReportFile(reportTestNGPath);
                } catch (Exception e) {
                    msg = String.format("ERROR of saving TestNG report:\nPATH:\n%s\nERROR:\n%s", reportTestNGPath, ExceptionUtils.getStackTrace(e));
                    throw e;
                }
                args = new String[]{reportTestNGPath};
            }

            log.info(String.format(">>>>>>   Report file PATH:   <<<<<<\n%s", Arrays.toString(args)));

            File generateFile = getGenerateExcelReportFile(args);
            openDesktopFile(generateFile);
        } catch (Exception e) {
            if (msg == null) {
                msg = ExceptionUtils.getStackTrace(e);
            }
            log.error(msg);
            viewAlert(msg);
        } finally {
            System.exit(0);
        }
    }

    public static File getGenerateExcelReportFile(String... reportTestNGPaths)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        return getGenerateExcelReportFile(getGenerateReportFilePath(reportTestNGPaths[0], ExcelGenerator.EXCEL_EXTENSION), reportTestNGPaths);
    }

    public static File getGenerateExcelReportFile(String generateReportPath, String generateReportName, String reportTestNGPaths)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        return getGenerateExcelReportFile(new File(
                Paths.get(getDecodeAbsolutePath(generateReportPath), generateReportName + ExcelGenerator.EXCEL_EXTENSION).toString()), reportTestNGPaths);
    }

    private static File getGenerateExcelReportFile(File generateFile, String... reportTestNGPaths)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String message;
        try {
            List<String> failedTestNames = new ArrayList<>();
            List<String> failedTestStacktrace = new ArrayList<>();
            for (String reportTestNGPath : reportTestNGPaths) {
                ReportPageParser reportPageParser = new ReportPageParser(reportTestNGPath);
                failedTestNames.addAll(reportPageParser.getFailedTestsNames().stream().map(
                        test -> test.replace(":", ".").replace("#", ".").replace("()", ""))
                        .collect(Collectors.toList()));
                failedTestStacktrace.addAll(reportPageParser.getFailedTestsStacktrace());
            }
            cleanDuplicates(failedTestNames, failedTestStacktrace);
            fetchReportExcelSheet(failedTestNames, failedTestStacktrace);

            Map<String, List<String>> reportSummary = SummaryReport.groupFailedTests(failedTestNames, failedTestStacktrace);
            fetchSummaryExcelSheet(reportSummary);

            excelGenerator.createFile(generateFile);
            message = String.format(">>>>>>   Excel file PATH:   <<<<<<\n%s", generateFile.getPath());
            log.info(message);
        } catch (FileNotFoundException fileNotFoundException) {
            message = ExceptionUtils.getStackTrace(fileNotFoundException);
            log.error(message);
            throw fileNotFoundException;
        }
        return generateFile;
    }

    private static void cleanDuplicates(List<String> failedTestNames, List<String> failedTestStacktrace) {
        Set<String> duplicatesFailedTestNames = findDuplicates(failedTestNames);
        for (String duplicatesFailedTestName : duplicatesFailedTestNames) {
            int index = failedTestNames.indexOf(duplicatesFailedTestName);
            for (int i = index + 1; i < failedTestNames.size(); ++i) {
                if (failedTestNames.get(i).equals(duplicatesFailedTestName)) {
                    failedTestNames.remove(i);
                    failedTestStacktrace.remove(i--);
                }
            }
        }
    }

    private static Set<String> findDuplicates(List<String> listContainingDuplicates) {
        final Set<String> duplicates = new HashSet<>();
        final Set<String> checkingSet = new HashSet<>();
        for (String item : listContainingDuplicates) {
            if (!checkingSet.add(item)) {
                duplicates.add(item);
            }
        }
        return duplicates;
    }

    private static void fetchReportExcelSheet(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        excelGenerator.writeDataToExcelSheet(ExcelGenerator.REPORT_SHEET, failedTestsNames, failedTestsStacktrace);
    }

    private static void fetchSummaryExcelSheet(Map<String, List<String>> reportSummaryMap) {
        List<String> failedMethods = new ArrayList<>(reportSummaryMap.keySet());
        List<String> failureCounts = new ArrayList<>();
        List<String> failedTests = new ArrayList<>();
        for (String failedMethod : failedMethods) {
            List<String> testsList = reportSummaryMap.get(failedMethod);
            failureCounts.add(String.valueOf(testsList.size()));
            failedTests.add(String.join("\r\n", testsList));
        }
        excelGenerator.writeDataToExcelSheet(ExcelGenerator.SUMMARY_SHEET, failureCounts, failedMethods, failedTests);
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

    private static void saveRemoteBasicReportFile(String originalFilePath) throws IOException {
        if (originalFilePath.matches("[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?\\\\")) {
            FileUtils.copyFile(new File(originalFilePath),
                    new File(Paths.get(getParentSourceFilePath(), getFileName(originalFilePath)).toString()));
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

    private static String getGenerateReportFileNameWithExtension(String reportTestNGPath, String extension) {
        return getFileName(reportTestNGPath) + extension;
    }

    private static String getFileName(String file) {
        return FilenameUtils.getBaseName(file);
    }

    private static File getGenerateReportFilePath(String reportTestNGPath, String extension) throws UnsupportedEncodingException {
        return new File(Paths.get(getParentSourceFilePath(), getGenerateReportFileNameWithExtension(reportTestNGPath, extension)).toString());
    }

    private static String getParentSourceFilePath() throws UnsupportedEncodingException {
        return new File(getSourcePath()).getParent();
    }

    public static String getDecodeAbsolutePath(String sourcePath) throws UnsupportedEncodingException {
        return decode(new File(sourcePath).getAbsolutePath());
    }

    public static String decode(String path) throws UnsupportedEncodingException {
        return URLDecoder.decode(path, "UTF-8");
    }

    private static String getSourcePath() throws UnsupportedEncodingException {
        return getDecodeAbsolutePath(RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }
}
