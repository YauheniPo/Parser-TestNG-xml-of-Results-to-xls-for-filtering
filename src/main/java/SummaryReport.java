import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class SummaryReport {

    public static Map<String, List<String>> groupingTestsFailed(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        Map<String, List<String>> testsMap = new HashMap<>();

        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            String failedTest = failedTestsNames.get(rowNum).replace(":", ".").replace("()", "");
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

        String[] failedMethodArray = stackTraceStringsList.get(testStringNum - 1).split("\\.");
        String failedPage = failedMethodArray[failedMethodArray.length - 3];
        String failedMethod = StringUtils.substringBeforeLast(failedMethodArray[failedMethodArray.length - 2], "(");

        return String.format("%s.%s", failedPage, failedMethod);
    }

    private static int getNumberString(String expectString, List<String> list) {
        for (int rowNum = 0; rowNum < list.size(); ++rowNum) {
            if (list.get(rowNum).contains(expectString) || list.get(rowNum).contains("BaseTestPage.run")) {
                return rowNum;
            }
        }
        throw new RuntimeException(String.format("List does not contain '%s'.", expectString));
    }
}
