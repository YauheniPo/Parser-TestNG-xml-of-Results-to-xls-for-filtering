import driver.Browser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class RunTestNGResultsParserToXls {

    public static final String EXCEL_EXTENSION = "xlsx";
    private static final ReportPage reportPage = new ReportPage();
    private static XSSFWorkbook workbook = new XSSFWorkbook();

    public static void main(String[] args) {
        String message = null;
        String reportTestNGPath;
        try {
            if (args.length == 0) {
                JFileChooser jFileChooser = viewFileChooser();
                File file = jFileChooser.getSelectedFile();
                reportTestNGPath = file.getAbsolutePath();
            } else {
                reportTestNGPath = args[0];
            }

            log.info(String.format("Report file path: %s", reportTestNGPath));
            //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
//            String reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\RegressionSuiteFull.html";

            saveRemoteBasicReportFile(reportTestNGPath, getGenerateReportFile(getFileName(reportTestNGPath)).getAbsolutePath());

            Browser.getInstance();
            Browser.openUrl(reportTestNGPath);
            List<String> failedTestsNames = reportPage.getFailedTestsNames();
            List<String> failedTestsStacktrace = reportPage.getFailedTestsStacktraces();
            File generateFile = getGenerateReportFile(reportTestNGPath, failedTestsNames.size(), failedTestsStacktrace.size(), EXCEL_EXTENSION);
            fetchReportExcelSheet(failedTestsNames, failedTestsStacktrace);
            Map<String, List<String>> reportSummary = SummaryReport.groupingTestsFailed(failedTestsNames, failedTestsStacktrace);
            fetchSummaryExcelSheet(reportSummary);
            createFile(generateFile);
            message = String.format("Excel file PATH: %s", generateFile.getPath());
        } catch (Exception e) {
            message = e.getMessage();
            log.error(e);
        } finally {
            log.info(message);
            Browser.getInstance().exit();
            viewAlert(message);
        }
    }

    private static void saveRemoteBasicReportFile(String originalFilePath, String copyFilePath) throws IOException {
        if (!originalFilePath.contains(":")) {
            FileUtils.copyFile(new File(originalFilePath), new File(copyFilePath));
        }
    }

    private static JFileChooser viewFileChooser() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getDecodeAbsolutePath(getSourcePath())));
        jFileChooser.showOpenDialog(null);
        return jFileChooser;
    }

    private static void viewAlert(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    public static String getDecodeAbsolutePath(String sourcePath) {
        return decode(new File(sourcePath).getAbsolutePath());
    }

    private static String getSourcePath() {
        return RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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

    public static void fetchReportExcelSheet(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        writeFileSheet("report", failedTestsNames, failedTestsStacktrace);
    }

    private static void fetchSummaryExcelSheet(Map<String, List<String>> mapTests) {
        List<String> failedMethods = new ArrayList<>(mapTests.keySet());
        List<String> countFailed = new ArrayList<>();
        List<String> columnTestsCells = new ArrayList<>();
        for (String failedMethod : failedMethods) {
            String columnTestsCell = "";
            countFailed.add(String.valueOf(mapTests.get(failedMethod).size()));
            List<String> testsList = mapTests.get(failedMethod);
            for (String test : testsList) {
                columnTestsCell = columnTestsCell.concat(test).concat("\r\n");
            }
            columnTestsCells.add(columnTestsCell);
        }
        writeFileSheet("summary", countFailed, failedMethods, columnTestsCells);
    }

    @SafeVarargs
    private static void writeFileSheet(String sheetName, List<String>... columnLists) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        for (int rowNum = 0; rowNum < columnLists[0].size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);
            for (int columnNum = 0; columnNum < columnLists.length; ++columnNum) {
                Cell cell = row.createCell(columnNum);
                String cellData = columnLists[columnNum].get(rowNum);
                boolean isNumericData = StringUtils.isNumeric(cellData);
                if (isNumericData) {
                    cell.setCellValue(Integer.parseInt(cellData));
                } else {
                    cell.setCellValue(columnLists[columnNum].get(rowNum));
                }
            }
        }
    }

    public static void createFile(File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
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

    public static File getGenerateReportFile(String generateFileName) {
        return new File(new File(getDecodeAbsolutePath(getSourcePath())).getParent() + File.separator + generateFileName);
    }

    public static File getGenerateReportFile(String reportTestNGPath, int failedTestsNamesCount, int failedTestsStacktraceCount, String extension) {
        return new File(new File(getDecodeAbsolutePath(getSourcePath())).getParent()
                + File.separator + getGenerateReportFileName(reportTestNGPath, failedTestsNamesCount, failedTestsStacktraceCount, extension));
    }
}
