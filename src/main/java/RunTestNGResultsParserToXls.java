import driver.Browser;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class RunTestNGResultsParserToXls {

    public static final String EXCEL_EXTENSION = "xlsx";
    private static final ReportPage reportPage = new ReportPage();

    public static void main(String[] args) {
        JFileChooser jFileChooser = viewFileChooser();
        File file = jFileChooser.getSelectedFile();
        String reportTestNGPath = file.getAbsolutePath();

        //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
//        String reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\RegressionSuiteFull.html";

        String message = null;
        try {
            Browser.getInstance();
            Browser.openUrl(reportTestNGPath);
            List<String> failedTestsNames = reportPage.getFailedTestsNames();
            List<String> failedTestsStacktrace = reportPage.getFailedTestsStacktraces();
            File reportParsedFile = getXlsParsedReportFile(reportTestNGPath, failedTestsNames, failedTestsStacktrace);
            Map<String, List<String>> reportSummary = SummaryReport.groupingTestsFailed(failedTestsNames, failedTestsStacktrace);
            File summaryParsedFile = getSummaryXlsParsedReportFile(reportTestNGPath, reportSummary);
            message = String.format("Excel file PATH: %s\n\n" +
                    "Excel Summary file PATH: %s", reportParsedFile.getPath(), summaryParsedFile.getPath());
        } catch (Exception e) {
            message = e.getMessage();
        } finally {
            Browser.getInstance().exit();
            viewAlert(message);
        }
    }

    private static JFileChooser viewFileChooser() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getDecodeAbsolutePath(getSourcePath())));
        jFileChooser.showOpenDialog(null);
        return jFileChooser;
    }

    public static File getXlsParsedReportFile(String reportTestNGPath, List<String> failedTestsNames, List<String> failedTestsStacktrace) throws Exception {
        return fetchXlsReport(reportTestNGPath, failedTestsNames, failedTestsStacktrace);
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
            viewAlert(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static File fetchXlsReport(String reportTestNGPath, List<String> failedTestsNames, List<String> failedTestsStacktrace) throws Exception {
        String generateFileName = getGenerateReportFileName(reportTestNGPath, failedTestsNames.size(),
                failedTestsStacktrace.size(), EXCEL_EXTENSION);
        return createFile(generateFileName, failedTestsNames, failedTestsStacktrace);
    }

    private static File getSummaryXlsParsedReportFile(String reportTestNGPath, Map<String, List<String>> mapTests) throws Exception {
        String generateFileName = getGenerateSummaryReportFileName(reportTestNGPath, EXCEL_EXTENSION);
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
        return createFile(generateFileName, countFailed, failedMethods, columnTestsCells);
    }

    @SafeVarargs
    private static File createFile(String fileName, List<String>... columnLists) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("report");
        for (int rowNum = 0; rowNum < columnLists[0].size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);
            for (int columnNum = 0; columnNum < columnLists.length; ++columnNum) {
                Cell cell = row.createCell(columnNum);
                String cellData = columnLists[columnNum].get(rowNum);
                boolean isNumericData = isNumeric(cellData);
                if (isNumericData) {
                    cell.setCellValue(Integer.parseInt(cellData));
                } else {
                    cell.setCellValue(columnLists[columnNum].get(rowNum));
                }
            }
        }
        File excelFile = getGenerateReportFile(fileName);
        try (FileOutputStream out = new FileOutputStream(excelFile)) {
            workbook.write(out);
        }
        return excelFile;
    }

    private static String getGenerateReportFileName(String reportTestNGPath, int failedTestsNamesCount, int failedTestsStacktraceCount, String extension) {
        return String.format("%s_%dTests_%dStacktrace.%s",
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()),
                failedTestsNamesCount,
                failedTestsStacktraceCount,
                extension);
    }

    private static String getGenerateSummaryReportFileName(String reportTestNGPath, String extension) {
        return String.format("%s_summary.%s",
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()),
                extension);
    }

    private static File getGenerateReportFile(String generateFileName) {
        return new File(new File(getDecodeAbsolutePath(getSourcePath())).getParent() + File.separator + generateFileName);
    }
}
