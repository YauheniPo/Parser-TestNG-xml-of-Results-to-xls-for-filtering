import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import yauhenipo.parser.ExcelGenerator;
import yauhenipo.parser.RunTestNGResultsParserToXls;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * The type Report parsing test.
 * @author Yauheni Papovich
 */
public class ReportParsingTest {

    private static final String TEST_REPORT_FILE_NAME = "report.html";
    private String testReport;
    private File reportParsedFile;

    private final String failedTestName = "com.test.Test.failedTest";

    @BeforeClass
    public void beforeClass() {
        try {
            testReport = RunTestNGResultsParserToXls.getDecodeAbsolutePath(
                    Objects.requireNonNull(ReportParsingTest.class.getClassLoader().getResource(TEST_REPORT_FILE_NAME)).getPath());
            reportParsedFile = RunTestNGResultsParserToXls.getGenerateExcelReportFile(testReport);
        } catch (IOException | XPathExpressionException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void excelReportFileGenerateTest() {
        Assert.assertTrue(reportParsedFile.exists(), String.format("File '%s' does not exist", reportParsedFile.getAbsolutePath()));
    }

    @Test(dependsOnMethods = "excelReportFileGenerateTest")
    public void excelReportFileDataRowsTest() {
        try {
            XSSFSheet reportSheet = new ExcelGenerator(reportParsedFile.getAbsolutePath()).getSheet(ExcelGenerator.REPORT_SHEET);
            int actualFailedTestRows = reportSheet.getLastRowNum();
            Assert.assertEquals(actualFailedTestRows, 0, "Invalid generated test report failed test name.");
        } catch (IOException | InvalidFormatException e) {
            e.printStackTrace();
        }
    }

    @Test(dependsOnMethods = "excelReportFileGenerateTest")
    public void excelReportFileDataColumnsTest() {
        try {
            DataFormatter dataFormatter = new DataFormatter();
            XSSFSheet reportSheet = new ExcelGenerator(reportParsedFile.getAbsolutePath()).getSheet(ExcelGenerator.REPORT_SHEET);
            String actualFailedTestName = dataFormatter.formatCellValue(reportSheet.getRow(0).getCell(0));
            Assert.assertEquals(actualFailedTestName, failedTestName, "Invalid generated test report failed test name.");
        } catch (IOException | InvalidFormatException e) {
            e.printStackTrace();
        }
    }
}
