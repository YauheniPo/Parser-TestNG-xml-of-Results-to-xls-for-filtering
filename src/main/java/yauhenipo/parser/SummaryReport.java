package yauhenipo.parser;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
public class SummaryReport {

    static Map<String, List<String>> groupingTestsFailed(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        Map<String, List<String>> testsMap = new HashMap<>();

        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            String failedTest = failedTestsNames.get(rowNum).replace("()", "");
            String failedMethod = fetchFailStacktraceMethod(failedTest, failedTestsStacktrace.get(rowNum));

            List<String> actualList;
            if (testsMap.get(failedMethod) == null) {
                actualList = new ArrayList<>();
            } else {
                actualList = testsMap.get(failedMethod);
            }
            actualList.add(failedTest);
            testsMap.put(failedMethod, actualList);
        }
        return testsMap;
    }

    private static String fetchFailStacktraceMethod(String failedTest, String stackTrace) {
        List<String> stackTraceStringsList = Arrays.asList(stackTrace.split("\n"));
        int testStringNum = getIndexTestNameFromStacktrace(failedTest, stackTraceStringsList);
        List<String> testStackTraceList = IntStream.range(0, testStringNum).mapToObj(stackTraceStringsList::get).collect(Collectors.toList());
        return getFormattingFailStacktraceMethod(testStackTraceList);
    }

    private static String getFormattingFailStacktraceMethod(List<String> testStackTraceList) {
        String testFailureMethod = findTestFailureStacktraceLine(new ArrayList<>(testStackTraceList));
        if (testStackTraceList.size() == 1) {
            return testFailureMethod;
        }
        Pattern r = Pattern.compile("\\.(\\w*\\.\\w*)\\(");
        Matcher m = r.matcher(testFailureMethod);
        m.find();
        try {
            return m.group(1);
        } catch (IllegalStateException e) {
            String message = String.format("ERROR for matching of '%s' from stacktrace: %s", testFailureMethod, String.join("\n", testStackTraceList));
            log.error(message);
            RunTestNGResultsParserToXls.viewAlert(message);
        }
        return null;
    }

    private static String findTestFailureStacktraceLine(List<String> testStackTraceList) {
        int stackTraceSize = testStackTraceList.size();
        for (int rowNum = stackTraceSize - 1; 0 <= rowNum; --rowNum) {
            String stackTraceLine = testStackTraceList.get(rowNum);
            if (rowNum != 0 && !testStackTraceList.get(rowNum - 1).contains("com.mscs.emr.test.functional.g2.pages.")) {
                return stackTraceLine;
            }
        }
        return testStackTraceList.get(stackTraceSize - 1);
    }

    private static int getIndexTestNameFromStacktrace(String expectString, List<String> testStackTraceList) {
        int stackTraceSize = testStackTraceList.size();
        for (int rowNum = 0; rowNum < stackTraceSize; ++rowNum) {
            if (testStackTraceList.get(rowNum).contains(expectString)) {
                return rowNum;
            }
        }
        for (int rowNum = stackTraceSize - 1; 0 <= rowNum; --rowNum) {
            String stackTraceLine = testStackTraceList.get(rowNum);
            if (stackTraceLine.contains("com.mscs.emr.test.functional.BaseTestPage.")) {
                return rowNum;
            }
        }

        RuntimeException runtimeException = new RuntimeException(String.format("List does not contain '%s'.", expectString));
        log.error(runtimeException);
        throw runtimeException;
    }
}
