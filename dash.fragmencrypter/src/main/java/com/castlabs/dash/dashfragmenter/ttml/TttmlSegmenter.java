package com.castlabs.dash.dashfragmenter.ttml;

import com.googlecode.mp4parser.authoring.tracks.SMPTETTTrackImpl;
import com.sun.org.apache.xpath.internal.NodeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlecode.mp4parser.authoring.tracks.SMPTETTTrackImpl.toTime;
import static com.googlecode.mp4parser.authoring.tracks.SMPTETTTrackImpl.toTimeExpression;

public class TttmlSegmenter {


    public static final String SMPTE_TT_NAMESPACE = "http://www.smpte-ra.org/schemas/2052-1/2010/smpte-tt";
    public static final String TTML_NAMESPACE = "http://www.w3.org/ns/ttml";


    private static class TextTrackNamespaceContext implements NamespaceContext {
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("ttml")) {
                return TTML_NAMESPACE;
            }
            if (prefix.equals("smpte")) {
                return SMPTE_TT_NAMESPACE;
            }
            return null;
        }

        public Iterator getPrefixes(String val) {
            return Arrays.asList("ttml", "smpte").iterator();
        }

        public String getPrefix(String uri) {
            if (uri.equals(TTML_NAMESPACE)) {
                return "ttml";
            }
            if (uri.equals(SMPTE_TT_NAMESPACE)) {
                return "smpte";
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File("C:\\dev\\dashencrypt\\a.xml"));
        // doc = normalizeTimes(doc);
        List<Document> splits = split(doc, 60000);
        for (Document split : splits) {

            pretty(split, System.out, 4);
            System.out.println("==========================================================================================================");
        }
    }

    public static List<Document> split(Document doc, int splitTime) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        NamespaceContext ctx = new TextTrackNamespaceContext();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression xp = xpath.compile("//*[name()='p']");

        boolean thereIsMore;

        List<Document> subDocs = new ArrayList<Document>();
        do {
            Document d = (Document) doc.cloneNode(true);
            NodeList timedNodes = (NodeList) xp.evaluate(d, XPathConstants.NODESET);
            long segmentStartTime = subDocs.size() * splitTime;
            long segmentEndTime = (subDocs.size()+1) * splitTime;
            thereIsMore = false;

            for (int i = 0; i < timedNodes.getLength(); i++) {
                Node p = timedNodes.item(i);
                long startTime = getStartTime(p);
                long endTime = getEndTime(p);
                p.appendChild(d.createComment(toTimeExpression(startTime) + " -> " + toTimeExpression(endTime)));
                if (startTime < segmentStartTime && endTime > segmentStartTime) {
                    changeTime(p, "begin", segmentStartTime - startTime);
                    startTime = segmentStartTime;

                }

                if (startTime >= segmentStartTime && startTime < segmentEndTime && endTime > segmentEndTime) {
                    changeTime(p, "end", segmentEndTime - endTime );
                    startTime = segmentStartTime;
                    endTime = segmentEndTime;
                }

                if (startTime > segmentEndTime) {
                    thereIsMore = true;
                }

                if (!(startTime >= segmentStartTime && endTime <= segmentEndTime)) {
                    Node parent = p.getParentNode();
                    parent.removeChild(p);
                    trimWhitespace(parent);
                }
            }
            subDocs.add(d);
        } while (thereIsMore);

        return subDocs;
    }

    public static void changeTime(Node p, String attribute, long amount) {
        if (p.getAttributes() != null && p.getAttributes().getNamedItem(attribute) != null) {
            long nuTime = toTime(p.getAttributes().getNamedItem(attribute).getNodeValue()) + amount;
            p.getAttributes().getNamedItem(attribute).setNodeValue(toTimeExpression(nuTime));
        }

    }

    public static long getStartTime(Node p) {
        long time = 0;
        Node current = p;
        while ((current = current.getParentNode()) != null) {
            if (current.getAttributes() != null && current.getAttributes().getNamedItem("begin") != null) {
                time += toTime(current.getAttributes().getNamedItem("begin").getNodeValue());
            }
        }

        if (p.getAttributes() != null && p.getAttributes().getNamedItem("begin") != null) {
            return time + toTime(p.getAttributes().getNamedItem("begin").getNodeValue());
        }
        return time;
    }

    public static long getEndTime(Node p) {
        long time = 0;
        Node current = p;
        while ((current = current.getParentNode()) != null) {
            if (current.getAttributes() != null && current.getAttributes().getNamedItem("begin") != null) {
                time += toTime(current.getAttributes().getNamedItem("begin").getNodeValue());
            }
        }

        if (p.getAttributes() != null && p.getAttributes().getNamedItem("end") != null) {
            return time + toTime(p.getAttributes().getNamedItem("end").getNodeValue());
        }
        return time;
    }

    public static Document normalizeTimes(Document doc) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        NamespaceContext ctx = new TextTrackNamespaceContext();
        XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(ctx);
        XPathExpression xp = xpath.compile("//*[name()='p']");
        NodeList timedNodes = (NodeList) xp.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < timedNodes.getLength(); i++) {
            Node p = timedNodes.item(i);
            pushDown(p);

        }
        for (int i = 0; i < timedNodes.getLength(); i++) {
            Node p = timedNodes.item(i);
            removeAfterPushDown(p, "begin");
            removeAfterPushDown(p, "end");

        }
        return doc;
    }

    private static void pushDown(Node p) {
        long time = 0;

        Node current = p;
        while ((current = current.getParentNode()) != null) {
            if (current.getAttributes() != null && current.getAttributes().getNamedItem("begin") != null) {
                time += toTime(current.getAttributes().getNamedItem("begin").getNodeValue());
            }
        }

        if (p.getAttributes() != null && p.getAttributes().getNamedItem("begin") != null) {
            p.getAttributes().getNamedItem("begin").setNodeValue(SMPTETTTrackImpl.toTimeExpression(time + toTime(p.getAttributes().getNamedItem("begin").getNodeValue())));
        }
        if (p.getAttributes() != null && p.getAttributes().getNamedItem("end") != null) {
            p.getAttributes().getNamedItem("end").setNodeValue(SMPTETTTrackImpl.toTimeExpression(time + toTime(p.getAttributes().getNamedItem("end").getNodeValue())));
        }

    }

    private static void removeAfterPushDown(Node p, String begin) {
        Node current = p;
        while ((current = current.getParentNode()) != null) {
            if (current.getAttributes() != null && current.getAttributes().getNamedItem(begin) != null) {
                current.getAttributes().removeNamedItem(begin);
            }
        }
    }

    public static void trimWhitespace(Node node)
    {
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if(child.getNodeType() == Node.TEXT_NODE) {
                child.setTextContent(child.getTextContent().trim());
            }
            trimWhitespace(child);
        }
    }

    private static void pretty(Document document, OutputStream outputStream, int indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        if (indent > 0) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
        }
        Result result = new StreamResult(outputStream);
        Source source = new DOMSource(document);
        transformer.transform(source, result);
    }


}
