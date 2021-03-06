package yauhenipo.parser;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Summary report.
 *
 * @author Yauheni Papovich
 */
@Log4j2
class SummaryReport {

    private static final String TYPE_FAIL_IS_NOT_SUPPORTED = "THIS TYPE OF FAIL IS NOT SUPPORTED";

    /**
     * Grouping the common tests failures methods by the list of failures tests and by the list of stacktrace
     *
     * @return Data Map where key is common test failure method and value is a list of tests failures
     */
    static Map<String, List<String>> groupFailedTests(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        Map<String, List<String>> reportSummary = new HashMap<>();

        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            String failedTest = failedTestsNames.get(rowNum).replace("()", "").replace(":", ".").replace("#", ".");
            String failedMethod = getFailedMethod(failedTestsStacktrace.get(rowNum));

            List<String> testFailedBySpecifiedMethod = reportSummary.containsKey(failedMethod) ? reportSummary.get(failedMethod) : new ArrayList<>();
            testFailedBySpecifiedMethod.add(failedTest);
            reportSummary.put(failedMethod, testFailedBySpecifiedMethod);
        }
        return sortSummaryByFailuresCount(reportSummary);
    }

    /**
     * Sorting Summary Map by the count of failed tests
     */
    private static Map<String, List<String>> sortSummaryByFailuresCount(Map<String, List<String>> unsortSummaryReportMap) {
        Map<String, List<String>> sortSummaryMap = new LinkedHashMap<>();
        unsortSummaryReportMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder()))
                .forEachOrdered(x -> sortSummaryMap.put(x.getKey(), x.getValue()));
        return sortSummaryMap;
    }

    private static String getFailedMethod(String failStackTrace) {
        List<String> stackTraceStringsList = Arrays.asList(failStackTrace.split("\n"));
        // Get index of line in stacktrace, where test method is failing
        PropertyReader propertyReader = new PropertyReader();
        String testFailureMethod = stackTraceStringsList.stream()
                .filter(stackTraceLine ->
                        propertyReader.getAllKeys().stream().anyMatch(mark -> stackTraceLine.contains((CharSequence) mark))
                ).findFirst().orElse("");
        return parseTestFailedMethodByRegExp(testFailureMethod);
    }

    /**
     * Get failure test method in format <className>.<failureMethodName> by the regExp '\.([\w\d]*\.(<init>|[\w\d]*))\('
     */
    private static String parseTestFailedMethodByRegExp(String failedTest) {
        Pattern r = Pattern.compile("\\.([\\w\\d]*\\.(<init>|[\\w\\d]*))\\(");
        Matcher m = r.matcher(failedTest);
        m.find();
        try {
            return m.group(1);
        } catch (IllegalStateException e) {
            return TYPE_FAIL_IS_NOT_SUPPORTED;
        }
    }
}
