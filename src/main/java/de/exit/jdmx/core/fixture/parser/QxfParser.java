package de.exit.jdmx.core.fixture.parser;

import de.exit.jdmx.core.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * Nativer XML-Parser für QLC+ Scheinwerfer-Profile (*.qxf).
 * <p>
 * Parst XML-Dateien mit {@link DocumentBuilderFactory} ohne externe Bibliotheken.
 * Mappt QLC+-Gruppen und Kanalnamen automatisch auf standardisierte {@link ChannelType}s.
 * </p>
 */
public class QxfParser implements FixtureParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".qxf");
    }

    @Override
    public FixtureDefinition parse(InputStream inputStream, String sourceIdentifier) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // Schutz vor XXE-Angriffen
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        if (!"FixtureDefinition".equalsIgnoreCase(root.getTagName())) {
            throw new IllegalArgumentException("Invalid QXF XML: Root element must be <FixtureDefinition>");
        }

        String manufacturer = getTagValue(root, "Manufacturer");
        String model = getTagValue(root, "Model");
        if (manufacturer.isEmpty()) manufacturer = "Generic";
        if (model.isEmpty()) model = sourceIdentifier != null ? sourceIdentifier : "Custom Fixture";

        // 1. Alle definierten Kanäle des Geräts einlesen
        Map<String, ChannelDefinition> channelPool = new LinkedHashMap<>();
        NodeList channelNodes = root.getElementsByTagName("Channel");
        int globalIdx = 1;
        for (int i = 0; i < channelNodes.getLength(); i++) {
            Node node = channelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getParentNode() == root) {
                Element chEl = (Element) node;
                String chName = chEl.getAttribute("Name");
                if (chName.isEmpty()) {
                    chName = "Channel " + globalIdx;
                }

                String groupName = getTagValue(chEl, "Group");
                String colorName = getTagValue(chEl, "Colour");
                boolean isFine = false;

                NodeList groupNodes = chEl.getElementsByTagName("Group");
                if (groupNodes.getLength() > 0 && groupNodes.item(0) instanceof Element grpEl) {
                    String byteAttr = grpEl.getAttribute("Byte");
                    if ("1".equals(byteAttr)) {
                        isFine = true;
                    }
                }
                if (chName.toLowerCase(Locale.ROOT).contains("fine") || chName.toLowerCase(Locale.ROOT).contains("16-bit")) {
                    isFine = true;
                }

                ChannelType type = resolveChannelType(chName, groupName, colorName, isFine);
                ChannelDefinition def = new ChannelDefinition(globalIdx++, chName, type, 0, isFine);
                channelPool.put(chName, def);
            }
        }

        // 2. Modi (Modes) einlesen
        List<DmxMode> modes = new ArrayList<>();
        NodeList modeNodes = root.getElementsByTagName("Mode");
        for (int i = 0; i < modeNodes.getLength(); i++) {
            Node node = modeNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element modeEl = (Element) node;
                String modeName = modeEl.getAttribute("Name");
                if (modeName.isEmpty()) {
                    modeName = "Standard Mode";
                }

                List<ChannelDefinition> modeChannels = new ArrayList<>();
                NodeList modeChNodes = modeEl.getElementsByTagName("Channel");
                for (int j = 0; j < modeChNodes.getLength(); j++) {
                    Node mNode = modeChNodes.item(j);
                    if (mNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element mEl = (Element) mNode;
                        String chRefName = mEl.getTextContent().trim();
                        ChannelDefinition matched = channelPool.get(chRefName);
                        if (matched != null) {
                            // Erzeuge neue Definition mit sequentieller Indexierung im Modus
                            ChannelDefinition modeCh = new ChannelDefinition(
                                    modeChannels.size() + 1,
                                    matched.name(),
                                    matched.type(),
                                    matched.defaultValue(),
                                    matched.isFine()
                            );
                            modeChannels.add(modeCh);
                        }
                    }
                }

                if (!modeChannels.isEmpty()) {
                    modes.add(new DmxMode(modeName, modeChannels));
                }
            }
        }

        // Fallback-Modus, falls in der QXF keine expliziten <Mode> Tags deklariert waren
        if (modes.isEmpty() && !channelPool.isEmpty()) {
            List<ChannelDefinition> fallbackChannels = new ArrayList<>(channelPool.values());
            modes.add(new DmxMode("Default Mode", fallbackChannels));
        }

        String id = (manufacturer + "-" + model).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return new FixtureDefinition(id, manufacturer, model, ExplorerItemType.FIXTURE, modes);
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    private ChannelType resolveChannelType(String name, String group, String colour, boolean isFine) {
        String n = name.toLowerCase(Locale.ROOT);
        String g = group.toLowerCase(Locale.ROOT);
        String c = colour.toLowerCase(Locale.ROOT);

        if (n.contains("pan fine") || (g.contains("pan") && isFine)) {
            return ChannelType.PAN_FINE;
        }
        if (n.contains("tilt fine") || (g.contains("tilt") && isFine)) {
            return ChannelType.TILT_FINE;
        }
        if (g.contains("pan") || n.contains("pan")) {
            return ChannelType.PAN;
        }
        if (g.contains("tilt") || n.contains("tilt")) {
            return ChannelType.TILT;
        }
        if (c.contains("red") || n.contains("red") || n.startsWith("r")) {
            return ChannelType.RED;
        }
        if (c.contains("green") || n.contains("green") || n.startsWith("g")) {
            return ChannelType.GREEN;
        }
        if (c.contains("blue") || n.contains("blue") || n.startsWith("b")) {
            return ChannelType.BLUE;
        }
        if (c.contains("white") || n.contains("white") || n.startsWith("w")) {
            return ChannelType.WHITE;
        }
        if (c.contains("amber") || n.contains("amber")) {
            return ChannelType.AMBER;
        }
        if (c.contains("uv") || n.contains("uv")) {
            return ChannelType.UV;
        }
        if (g.contains("intensity") || n.contains("dimmer") || n.contains("intensity")) {
            return ChannelType.DIMMER;
        }
        if (g.contains("shutter") || n.contains("shutter") || n.contains("strobe")) {
            return ChannelType.SHUTTER;
        }
        if (g.contains("colour") || g.contains("color") || n.contains("color wheel") || n.contains("colour wheel")) {
            return ChannelType.COLOR_WHEEL;
        }
        if (g.contains("gobo") || n.contains("gobo")) {
            return ChannelType.GOBO;
        }
        if (g.contains("prism") || n.contains("prism")) {
            return ChannelType.PRISM;
        }
        if (g.contains("speed") || n.contains("speed")) {
            return ChannelType.SPEED;
        }
        if (g.contains("effect") || n.contains("effect") || n.contains("macro")) {
            return ChannelType.EFFECT;
        }
        if (g.contains("maintenance") || n.contains("reset") || n.contains("control")) {
            return ChannelType.MAINTENANCE;
        }
        return ChannelType.GENERIC;
    }
}
