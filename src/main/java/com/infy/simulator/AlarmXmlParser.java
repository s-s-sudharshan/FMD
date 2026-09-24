package com.infy.simulator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.infy.enums.Severity;
import com.infy.enums.TrapType;

/**
 * Parses the simulator's alarm XML (BE US16) into plain records.
 * Format: &lt;alarms&gt;&lt;alarm&gt;&lt;deviceIp/&gt;&lt;severity/&gt;&lt;trap/&gt;&lt;notes/&gt;&lt;/alarm&gt;...&lt;/alarms&gt;
 * DOCTYPEs are disallowed (XXE protection). Entries with a missing field or an
 * unknown severity/trap are skipped with a warning rather than failing the batch;
 * malformed XML as a whole throws IllegalArgumentException.
 */
@Component
public class AlarmXmlParser {

    private static final Logger logger = LoggerFactory.getLogger(AlarmXmlParser.class);

    public record ParsedAlarm(String deviceIp, Severity severity, TrapType trap, String notes) {
    }

    public List<ParsedAlarm> parse(String xml) {
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed alarm XML", ex);
        }

        List<ParsedAlarm> result = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("alarm");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String ip = text(el, "deviceIp");
            String severity = text(el, "severity");
            String trap = text(el, "trap");
            if (ip == null || severity == null || trap == null) {
                logger.warn("Skipping alarm entry #{} - deviceIp/severity/trap is required", i);
                continue;
            }
            try {
                result.add(new ParsedAlarm(ip, Severity.valueOf(severity), TrapType.valueOf(trap), text(el, "notes")));
            } catch (IllegalArgumentException ex) {
                logger.warn("Skipping alarm entry #{} - unknown severity '{}' or trap '{}'", i, severity, trap);
            }
        }
        return result;
    }

    private String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }
        String value = list.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
