package de.exit.jdmx.core.fixture.parser;

import de.exit.jdmx.core.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Nativer Parser für das GDTF-Format (*.gdtf nach DIN SPEC 15800).
 * <p>
 * Entpackt das ZIP-Container-Archiv im Speicher und parst die enthaltene {@code description.xml}.
 * </p>
 */
public class GdtfParser implements FixtureParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".gdtf");
    }

    @Override
    public FixtureDefinition parse(InputStream inputStream, String sourceIdentifier) throws Exception {
        byte[] descriptionXmlBytes = null;

        // 1. description.xml aus dem ZIP-Container extrahieren
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("description.xml".equalsIgnoreCase(entry.getName()) || entry.getName().endsWith("/description.xml")) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int nRead;
                    while ((nRead = zis.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    descriptionXmlBytes = buffer.toByteArray();
                    break;
                }
            }
        }

        if (descriptionXmlBytes == null || descriptionXmlBytes.length == 0) {
            throw new IllegalArgumentException("Invalid GDTF file: Missing 'description.xml' inside container");
        }

        // 2. XML parsen
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(descriptionXmlBytes));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        NodeList fixtureTypeNodes = root.getElementsByTagName("FixtureType");
        if (fixtureTypeNodes.getLength() == 0) {
            throw new IllegalArgumentException("Invalid GDTF XML: Missing <FixtureType> element");
        }

        Element ftEl = (Element) fixtureTypeNodes.item(0);
        String name = ftEl.getAttribute("Name");
        String manufacturer = ftEl.getAttribute("Manufacturer");
        if (manufacturer.isEmpty()) manufacturer = "Generic";
        if (name.isEmpty()) name = sourceIdentifier != null ? sourceIdentifier : "GDTF Fixture";

        // 3. DMXModes parsen
        List<DmxMode> modes = new ArrayList<>();
        NodeList dmxModesList = ftEl.getElementsByTagName("DMXMode");
        for (int i = 0; i < dmxModesList.getLength(); i++) {
            Node modeNode = dmxModesList.item(i);
            if (modeNode.getNodeType() == Node.ELEMENT_NODE) {
                Element modeEl = (Element) modeNode;
                String modeName = modeEl.getAttribute("Name");
                if (modeName.isEmpty()) modeName = "Mode " + (i + 1);

                List<ChannelDefinition> channels = new ArrayList<>();
                NodeList chNodes = modeEl.getElementsByTagName("DMXChannel");
                for (int j = 0; j < chNodes.getLength(); j++) {
                    Node chNode = chNodes.item(j);
                    if (chNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element chEl = (Element) chNode;
                        String offset = chEl.getAttribute("Offset");

                        // LogicalChannel Attribute suchen
                        String attr = "Generic";
                        NodeList logicalNodes = chEl.getElementsByTagName("LogicalChannel");
                        if (logicalNodes.getLength() > 0 && logicalNodes.item(0) instanceof Element lcEl) {
                            attr = lcEl.getAttribute("Attribute");
                            if (attr.isEmpty()) attr = "Generic";
                        }

                        boolean isFine = offset.contains(",");
                        ChannelType type = resolveGdtfAttribute(attr);
                        if (isFine && type == ChannelType.PAN) {
                            // GDTF hat 16-Bit Pan z. B. als Offset="1,2"
                            channels.add(new ChannelDefinition(channels.size() + 1, attr, ChannelType.PAN, 128, false));
                            channels.add(new ChannelDefinition(channels.size() + 1, attr + " Fine", ChannelType.PAN_FINE, 0, true));
                            continue;
                        } else if (isFine && type == ChannelType.TILT) {
                            channels.add(new ChannelDefinition(channels.size() + 1, attr, ChannelType.TILT, 128, false));
                            channels.add(new ChannelDefinition(channels.size() + 1, attr + " Fine", ChannelType.TILT_FINE, 0, true));
                            continue;
                        }

                        ChannelDefinition chDef = new ChannelDefinition(channels.size() + 1, attr, type, 0, false);
                        channels.add(chDef);
                    }
                }

                if (!channels.isEmpty()) {
                    modes.add(new DmxMode(modeName, channels));
                }
            }
        }

        if (modes.isEmpty()) {
            throw new IllegalArgumentException("GDTF file contains no valid DMX channels or modes");
        }

        String id = (manufacturer + "-" + name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return new FixtureDefinition(id, manufacturer, name, ExplorerItemType.FIXTURE, modes);
    }

    private ChannelType resolveGdtfAttribute(String attr) {
        String a = attr.toLowerCase(Locale.ROOT);
        if (a.contains("panfine")) return ChannelType.PAN_FINE;
        if (a.contains("tiltfine")) return ChannelType.TILT_FINE;
        if (a.contains("pan")) return ChannelType.PAN;
        if (a.contains("tilt")) return ChannelType.TILT;
        if (a.contains("dimmer") || a.contains("intensity")) return ChannelType.DIMMER;
        if (a.contains("coloradd_r") || a.contains("red")) return ChannelType.RED;
        if (a.contains("coloradd_g") || a.contains("green")) return ChannelType.GREEN;
        if (a.contains("coloradd_b") || a.contains("blue")) return ChannelType.BLUE;
        if (a.contains("coloradd_w") || a.contains("white")) return ChannelType.WHITE;
        if (a.contains("amber")) return ChannelType.AMBER;
        if (a.contains("uv")) return ChannelType.UV;
        if (a.contains("shutter") || a.contains("strobe")) return ChannelType.SHUTTER;
        if (a.contains("colorwheel") || a.contains("color1")) return ChannelType.COLOR_WHEEL;
        if (a.contains("gobo")) return ChannelType.GOBO;
        if (a.contains("prism")) return ChannelType.PRISM;
        return ChannelType.GENERIC;
    }
}
