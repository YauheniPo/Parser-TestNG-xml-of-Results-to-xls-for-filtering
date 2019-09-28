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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class RunTestNGResultsParserToXls {

    private static boolean isWindowRun = false;
    private static ExcelGenerator excelGenerator = new ExcelGenerator();

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                isWindowRun = true;
                JFileChooser jFileChooser = viewFileChooser();
                progressBar();
                File[] files = jFileChooser.getSelectedFiles();

                if (files.length == 1) {
                    File file = files[0];
                    if (file.isFile()) {
                        String reportTestNGPath = file.getAbsolutePath();
                        try {
                            saveRemoteBasicReportFile(reportTestNGPath);
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("ERROR of saving TestNG report:\nPATH:\n%s", reportTestNGPath), e);
                        }
                    } else {
                        files = files[0].listFiles();
                    }
                }
                args = Arrays.stream(Objects.requireNonNull(files)).map(File::getAbsolutePath).filter(f -> f.endsWith(".html")).toArray(String[]::new);
                if (args.length == 0) {
                    throw new NullPointerException(String.format(">>>>>>   Report file does not identify! Files: %s   <<<<<<", Arrays.asList(files)));
                }
            }

            log.info(String.format(">>>>>>   Report file PATH:   <<<<<<\n%s", Arrays.toString(args)));

            File generateFile = getGenerateExcelReportFile(args);
            openDesktopFile(generateFile);
        } catch (Exception e) {
            viewAlert(ExceptionUtils.getStackTrace(e));
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
        return getGenerateExcelReportFile(new File(getDecodeAbsolutePath(generateReportPath), generateReportName + ExcelGenerator.EXCEL_EXTENSION), reportTestNGPaths);
    }

    private static File getGenerateExcelReportFile(File generateFile, String... reportTestNGPaths)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        try {
            List<String> failedTestNames = new ArrayList<>();
            List<String> failedTestStacktrace = new ArrayList<>();
            for (String reportTestNGPath : reportTestNGPaths) {
                log.info(String.format("Parsing: %s", reportTestNGPath));
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
            log.info(String.format(">>>>>>   Excel file PATH:   <<<<<<\n%s", generateFile.getPath()));
        } catch (FileNotFoundException fileNotFoundException) {
            log.error(fileNotFoundException);
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
                viewAlert(String.format("Open desktop file:\n%s", ExceptionUtils.getStackTrace(e)));
            }
        }
    }

    private static void saveRemoteBasicReportFile(String originalFilePath) throws IOException {
        if (originalFilePath.contains("corp.mckesson.com")) {
            FileUtils.copyFile(new File(originalFilePath), new File(getParentSourceFilePath(), FilenameUtils.getName(originalFilePath)));
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
        jFileChooser.setMultiSelectionEnabled(true);
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getSourcePath()));
        jFileChooser.showOpenDialog(null);
        return jFileChooser;
    }

    private static void progressBar() {
        final JFrame frame = new JFrame("Progress Bar");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(400, 70));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void viewAlert(String message) {
        log.error(message);

        if (isWindowRun) {
            JOptionPane pane = new JOptionPane();
            pane.setMessage(message);
            JDialog dialog = pane.createDialog("Message");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
        }
    }

    private static String getGenerateReportFileNameWithExtension(String reportTestNGPath, String extension) {
        return FilenameUtils.getBaseName(reportTestNGPath) + extension;
    }

    private static File getGenerateReportFilePath(String reportTestNGPath, String extension) throws UnsupportedEncodingException {
        return new File(getParentSourceFilePath(), getGenerateReportFileNameWithExtension(reportTestNGPath, extension));
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
