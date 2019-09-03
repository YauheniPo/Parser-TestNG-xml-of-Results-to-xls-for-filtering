import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
public class SummaryReport {

    public static Map<String, List<String>> groupingTestsFailed(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        Map<String, List<String>> testsMap = new HashMap<>();

        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            String failedTest = failedTestsNames.get(rowNum).replace("()", "");
            String failedMethod = getMethod(failedTest, failedTestsStacktrace.get(rowNum));

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

    private static String getMethod(String failedTest, String stackTrace) {
        List<String> stackTraceStringsList = Arrays.asList(stackTrace.split("\\n"));
        int testStringNum = getNumberString(failedTest, stackTraceStringsList);
        List<String> testStackTraceList = IntStream.range(0, testStringNum).mapToObj(stackTraceStringsList::get).collect(Collectors.toList());
        String testFailureMethod = getTestFailureMethod(new ArrayList<>(testStackTraceList));
        return getCommonFail(testFailureMethod);
    }

    private static String getCommonFail(String stackTrace) {
        Pattern r = Pattern.compile("\\.(\\w*.<?\\w*>?)\\(");
        Matcher m = r.matcher(stackTrace);
        m.find();
        return m.group(1);
    }

    private static String getTestFailureMethod(List<String> testStackTraceList) {
        int stackTraceSize = testStackTraceList.size();
        for (int i = stackTraceSize - 1; 0 <= i; --i) {
            String stackTraceLine = testStackTraceList.get(i);
            if (i != 0 && (testStackTraceList.get(i - 1).contains(".BaseTestPage.") || !testStackTraceList.get(i - 1).startsWith(" at "))) {
                return stackTraceLine;
            }
        }
        log.debug(String.format("!!!   Debug StackTrace parsing:   !!!\n%s", testStackTraceList.toString()));
        return testStackTraceList.get(stackTraceSize - 1);
    }

    private static int getNumberString(String expectString, List<String> list) {
        for (int rowNum = 0; rowNum < list.size(); ++rowNum) {
            if (list.get(rowNum).contains(expectString) || list.get(rowNum).contains("BaseTestPage.run")) {
                return rowNum;
            }
        }
        RuntimeException runtimeException = new RuntimeException(String.format("List does not contain '%s'.", expectString));
        log.error(runtimeException);
        throw runtimeException;
    }
}
