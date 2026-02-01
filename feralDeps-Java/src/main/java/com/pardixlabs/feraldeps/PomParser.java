package com.pardixlabs.feraldeps;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class PomParser {

    public static List<Dependency> parse(File pomFile) throws Exception {
        List<Dependency> deps = new ArrayList<>();

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        // Parse properties first
        Map<String, String> properties = parseProperties(doc);

        NodeList nodes = doc.getElementsByTagName("dependency");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);

            String groupId = getTag(el, "groupId");
            String artifactId = getTag(el, "artifactId");
            String version = getTag(el, "version");
            String scope = getTag(el, "scope"); // Get scope if specified

            if (version != null) {
                // Resolve property placeholders
                version = resolveProperties(version, properties);
                Dependency dep = new Dependency(groupId, artifactId, version);
                if (scope != null) {
                    dep.scope = scope;
                }
                deps.add(dep);
            }
        }

        return deps;
    }

    private static Map<String, String> parseProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();
        
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesEl = (Element) propertiesNodes.item(0);
            NodeList children = propertiesEl.getChildNodes();
            
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element prop = (Element) children.item(i);
                    properties.put(prop.getTagName(), prop.getTextContent().trim());
                }
            }
        }
        
        return properties;
    }

    private static String resolveProperties(String value, Map<String, String> properties) {
        if (value == null) return null;
        
        // Replace ${property.name} with actual value
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            value = value.replace(placeholder, entry.getValue());
        }
        
        return value;
    }

    private static String getTag(Element el, String tag) {
        NodeList list = el.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent().trim();
    }
}
