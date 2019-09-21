package yauhenipo.parser;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for parsing HTML file by XPATH
 */
@Log4j2
class ReportPageParser {

    private XPath xPath;
    private Document xmlDocument;

    private static final String FAILED_TESTS_NAMES_XPATH = "//div[@class='stacktrace']/ancestor::*/preceding-sibling::*[starts-with(@id, 'm') or contains(text(), '.test.')]";
    private static final String FAILED_TESTS_STACKTRACE_XPATH = "//*[@id]//following-sibling::*//div[@class='stacktrace']";
    private static final String FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH = "//table[@class='invocation-failed']//tr//td[@title]";
    private static final String FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_XPATH = FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH + "/following-sibling::td[.//pre]";

    ReportPageParser(String reportPath) throws ParserConfigurationException, SAXException, IOException {
        try {
            String data = new String(Files.readAllBytes(Paths.get(reportPath)));
            data = data.replace("<meta", "<meta/>");
            data = data.replace("<br>", "<br/>");
            data = data.replace("<p>", "<p/>");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream targetStream = new ByteArrayInputStream(data.getBytes());
            InputSource is = new InputSource(targetStream);
            xmlDocument = db.parse(is);
            xPath = XPathFactory.newInstance().newXPath();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    List<String> getFailedTestsNames() throws XPathExpressionException {
        List<Node> nodeList = getNodeList(FAILED_TESTS_NAMES_XPATH);
        if (nodeList.isEmpty()) {
            nodeList = getNodeList(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH);
            return nodeList.stream().map(
                    node -> node.getAttributes().getNamedItem("title").getTextContent()).collect(Collectors.toList());
        }
        return nodeList.stream().map(Node::getTextContent).collect(Collectors.toList());
    }

    List<String> getFailedTestsStacktrace() throws XPathExpressionException {
        List<Node> nodeList = getNodeList(FAILED_TESTS_STACKTRACE_XPATH);
        if (nodeList.isEmpty()) {
            nodeList = getNodeList(FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_XPATH);
        }
        return nodeList.stream().map(
                node -> node.getTextContent().replace("Click to show all stack frames", "")).collect(Collectors.toList());
    }

    private List<Node> getNodeList(String expression) throws XPathExpressionException {
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            nodes.add(nodeList.item(i));
        }
        return nodes;
    }
}
