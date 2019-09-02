import driver.Browser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class ReportPage {

    private static final By FAILED_TESTS_NAMES_LOCATOR = By.xpath("//div[@class='stacktrace']/preceding::*[starts-with(@id, 'm')]");
    private static final By FAILED_TESTS_STACKTRACE_LOCATOR = By.xpath("//*[@id]//following-sibling::*//div[@class='stacktrace']");
    private static final String FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH = "//table[@class='invocation-failed']//tr//td[@title]";
    private static final By FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH);
    private static final By FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH + "/following-sibling::td[.//pre]");

    public List<String> getFailedTestsNames() {
        List<String> failedTestsNames = getTextElements(Browser.getDriver(), FAILED_TESTS_NAMES_LOCATOR);
        if (failedTestsNames.isEmpty()) {
            failedTestsNames = Browser.getDriver().findElements(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                    .map(test -> test.getAttribute("title"))
                    .collect(Collectors.toList());
        }
        return failedTestsNames.stream().map(test -> test.replace(":", ".").replace("#", ".")).collect(Collectors.toList());
    }

    public List<String> getFailedTestsStacktraces() {
        List<String> failedTestsStacktraces = getTextElements(Browser.getDriver(), FAILED_TESTS_STACKTRACE_LOCATOR);
        if (failedTestsStacktraces.isEmpty()) {
            failedTestsStacktraces = Browser.getDriver().findElements(FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                    .map(stacktrace -> stacktrace.getText()
                            .replace("Click to show all stack frames", ""))
                    .collect(Collectors.toList());
        }
        return failedTestsStacktraces;
    }

    private static List<String> getTextElements(WebDriver driver, By locator) {
        return driver.findElements(locator).stream().map(WebElement::getText).collect(Collectors.toList());
    }
}
