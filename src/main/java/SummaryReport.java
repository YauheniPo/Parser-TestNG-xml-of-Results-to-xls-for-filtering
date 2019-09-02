import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
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
        String[] failedMethodArray = testFailureMethod.split("\\.");
        String failedPage = failedMethodArray[failedMethodArray.length - 3];
        String failedMethod = StringUtils.substringBeforeLast(failedMethodArray[failedMethodArray.length - 2], "(");

        return String.format("%s.%s", failedPage, failedMethod);
    }

    private static String getTestFailureMethod(List<String> testStackTraceList) {
        List<String> stackTraceList = new ArrayList<>(testStackTraceList);
        int stackTraceSize = stackTraceList.size();
//        for (int i = 0; i < stackTraceSize; ++i) {
//            if (!testStackTraceList.get(i).contains(".BaseTestPage.") || testStackTraceList.get(i).contains(" at com.mscs.emr.test.functional.g2.")) {
//                return testStackTraceList.get(i);
//            }
//        }
        return stackTraceList.get(stackTraceSize - 1);
    }

    private static int getNumberString(String expectString, List<String> list) {
        for (int rowNum = 0; rowNum < list.size(); ++rowNum) {
            if (list.get(rowNum).contains(expectString) || list.get(rowNum).contains("BaseTestPage.run")) {
                return rowNum;
            }
        }
        RuntimeException runtimeException = new RuntimeException(String.format("List does not contain '%s'.", expectString));
        log.fatal(runtimeException);
        throw runtimeException;
    }
}
