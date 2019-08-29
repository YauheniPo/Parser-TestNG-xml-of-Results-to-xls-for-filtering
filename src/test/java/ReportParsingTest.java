import driver.Browser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Objects;

public class ReportParsingTest {

    private static final String TEST_REPORT_FILE_NAME = "report.html";
    private final String testReport = RunTestNGResultsParserToXls.getDecodeAbsolutePath(
            Objects.requireNonNull(ReportParsingTest.class.getClassLoader().getResource(TEST_REPORT_FILE_NAME)).getPath());
    private File reportParsedFile;
    private static ReportPage reportPage = new ReportPage();

    @BeforeClass
    public void beforeClass() {
        Browser.getInstance();
        Browser.openUrl(testReport);
        try {
            this.reportParsedFile = RunTestNGResultsParserToXls.getXlsParsedReportFile(testReport, reportPage.getFailedTestsNames(), reportPage.getFailedTestsStacktraces());
        } catch (Exception e) {
            Browser.getInstance().exit();
            e.printStackTrace();
        }
    }

    @Test
    public void excelReportFileGenerateTest() {
        Assert.assertTrue(reportParsedFile.exists(), String.format("File '%s' does not exist", reportParsedFile.getAbsolutePath()));
    }

    // test2: check rows count = 1
    @Test
    public void excelReportFileDataRowsTest() {

    }
    // test3 depends on test1: parsing file - asserts testName and stacktrace
    @Test
    public void excelReportFileDataColumnsTest() {

    }
}
