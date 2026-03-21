package co.razkevich.sflocalstack.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataSoapParser {

    public ParsedSoapRequest parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element body = firstElement(document.getDocumentElement(), "Body");
            Element operationElement = firstChildElement(body);
            if (operationElement == null) {
                throw new IllegalArgumentException("SOAP body is missing operation element");
            }
            return new ParsedSoapRequest(operationElement.getLocalName(), extract(operationElement));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse metadata SOAP request", ex);
        }
    }

    private Map<String, Object> extract(Element element) {
        Map<String, Object> values = new HashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element childElement)) {
                continue;
            }
            String name = childElement.getLocalName();
            Element grandChild = firstChildElement(childElement);
            if (grandChild == null) {
                values.put(name, childElement.getTextContent().trim());
            } else {
                values.compute(name, (key, existing) -> {
                    List<Map<String, Object>> items;
                    if (existing instanceof List<?> existingList) {
                        items = (List<Map<String, Object>>) existingList;
                    } else {
                        items = new ArrayList<>();
                    }
                    items.add(extract(childElement));
                    return items;
                });
            }
        }
        return values;
    }

    private Element firstElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private Element firstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element) {
                return element;
            }
        }
        return null;
    }

    public record ParsedSoapRequest(String operation, Map<String, Object> values) {
    }
}
