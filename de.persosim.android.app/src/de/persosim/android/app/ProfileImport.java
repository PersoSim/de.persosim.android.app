package de.persosim.android.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class ProfileImport {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public final byte[] xml;
    public final String title;
    public final String details;
    public final String documentType;
    public final ProfileData editorData;

    private ProfileImport(byte[] xml, String title, String details, String documentType, ProfileData editorData) {
        this.xml = xml;
        this.title = title;
        this.details = details;
        this.documentType = documentType;
        this.editorData = editorData;
    }

    public static byte[] readLimited(InputStream input) throws IOException {
        if (input == null) throw new IOException("Datei konnte nicht geöffnet werden.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > MAX_BYTES) throw new IOException("Die Datei ist größer als 2 MB.");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    public static ProfileImport parse(byte[] bytes, String filename, Set<String> schema) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("Bitte eine PersoSim-Datei bis 2 MB auswählen.");
        String xml = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        if (xml.startsWith("\uFEFF")) xml = xml.substring(1);
        if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("XML-Dokumenttypen und externe Entitäten werden nicht unterstützt.");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("Externe XML-Referenz nicht erlaubt"); });
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        Element root = document.getDocumentElement();
        if (!root.getTagName().matches("de\\.persosim\\.simulator\\.perso\\.(Profile[A-Za-z0-9]+|DefaultPersoGt)")) {
            throw new IOException("Keine unterstützte PersoSim-Profildatei.");
        }
        validate(root, schema, 0, new int[1]);
        if (document.getElementsByTagName("masterFile").getLength() != 1
                || document.getElementsByTagName("protocols").getLength() != 1) {
            throw new IOException("Die PersoSim-Datei enthält keine vollständige Karte.");
        }
        Map<Integer, String> groups = new HashMap<>();
        Map<String, String> fields = new LinkedHashMap<>();
        for (String key : ProfileData.KEYS) fields.put(key, "");
        NodeList applications = document.getElementsByTagName("de.persosim.simulator.cardobjects.DedicatedFile");
        for (int i = 0; i < applications.getLength(); i++) {
            Element application = (Element) applications.item(i);
            if (!value(application, "dedicatedFileName", "fileName").equals("E80704007F00070302")) continue;
            Element children = child(application, "children");
            if (children == null) continue;
            for (Node node = children.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (!(node instanceof Element) || !node.getNodeName().equals("de.persosim.simulator.cardobjects.ElementaryFile")) continue;
                Element file = (Element) node;
                int sfi;
                try { sfi = Integer.parseInt(value(file, "shortFileIdentifier", "identifier")); }
                catch (NumberFormatException exception) { throw new IOException("Ungültige Dateikennung im PersoSim-Profil.", exception); }
                if (sfi <= 18) {
                    List<String> strings = new ArrayList<>();
                    Map<Integer, String> components = new HashMap<>();
                    decodeText(unhex(value(file, "content")), 0, 0, strings, components);
                    if (sfi == 17) {
                        String[] keys = {"street", "city", "country", "postalCode"};
                        int[] tags = {0xAA, 0xAB, 0xAD, 0xAE};
                        for (int j = 0; j < keys.length; j++) fields.put(keys[j], group(components, tags[j]));
                    }
                    if (sfi == 18) fields.put("communityId", group(components, 0x72));
                    groups.put(sfi, join(strings));
                }
            }
        }
        String title = (group(groups, 4) + " " + group(groups, 5)).trim();
        if (title.isEmpty()) title = filename.replaceFirst("(?i)\\.(perso|xml)$", "");
        if (title.isEmpty()) title = "Importierter Ausweis";
        if (title.length() > 160) title = title.substring(0, Character.isHighSurrogate(title.charAt(159)) ? 159 : 160);
        fields.put("title", title);
        String[] fieldKeys = {"documentType", "issuingCountry", "expiryDate", "givenName", "familyName",
                "artisticName", "academicTitle", "birthDate", "birthPlace", "nationality", "birthName"};
        int[] fieldGroups = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13};
        for (int i = 0; i < fieldKeys.length; i++) fields.put(fieldKeys[i], group(groups, fieldGroups[i]));
        for (String key : new String[]{"birthDate", "expiryDate"}) {
            String date = fields.get(key);
            if (date.matches("[0-9]{8}")) fields.put(key, date.substring(6) + "." + date.substring(4, 6) + "." + date.substring(0, 4));
        }
        StringBuilder details = new StringBuilder();
        NodeList mrz = document.getElementsByTagName("mrz");
        if (mrz.getLength() > 0) {
            String text = mrz.item(0).getTextContent().trim();
            if (text.length() == 90) {
                fields.put("documentNumber", text.substring(5, 14).replace("<", ""));
                fields.put("sex", text.charAt(37) == '<' ? "X" : text.substring(37, 38));
                details.append("Ausweisnummer: ").append(fields.get("documentNumber")).append('\n');
            }
        }
        int[] numbers = {4, 5, 8, 9, 13, 10, 3, 6, 7, 17};
        String[] labels = {"Vorname", "Nachname", "Geburtsdatum", "Geburtsort", "Geburtsname", "Staatsangehörigkeit",
                "Gültig bis", "Ordens- oder Künstlername", "Doktorgrad", "Anschrift"};
        for (int i = 0; i < numbers.length; i++) {
            String text = group(groups, numbers[i]);
            if ((numbers[i] == 8 || numbers[i] == 3) && text.matches("[0-9]{8}")) {
                text = text.substring(6) + "." + text.substring(4, 6) + "." + text.substring(0, 4);
            }
            if (!text.isEmpty()) details.append(labels[i]).append(": ").append(text).append('\n');
        }
        NodeList passwords = document.getElementsByTagName("passwordName");
        for (int i = 0; i < passwords.getLength(); i++) {
            Node name = passwords.item(i);
            if (name.getTextContent().matches("PIN|CAN|PUK")) {
                String text = new String(unhex(value((Element) name.getParentNode(), "password")), StandardCharsets.UTF_8);
                fields.put(name.getTextContent().toLowerCase(Locale.ROOT), text);
                if (text.matches("[0-9]{1,16}")) details.append(name.getTextContent()).append(": ").append(text).append('\n');
            }
        }
        details.append("\nImportiert aus: ").append(filename);
        return new ProfileImport(xml.getBytes(StandardCharsets.UTF_8), title, details.toString(), group(groups, 1), new ProfileData(fields));
    }

    private static void validate(Element element, Set<String> schema, int depth, int[] count) throws IOException {
        if (depth > 80 || ++count[0] > 30000) throw new IOException("Die XML-Struktur ist zu groß oder zu tief verschachtelt.");
        if (!schema.contains(element.getTagName())) throw new IOException("Nicht unterstütztes Profilelement: " + element.getTagName().substring(0, Math.min(120, element.getTagName().length())));
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String name = attribute.getNodeName();
            if (name.equals("class")) {
                if (!schema.contains(attribute.getNodeValue())) throw new IOException("Nicht unterstützter Datentyp im Profil.");
            } else if (!name.equals("id") && !name.equals("reference")) {
                throw new IOException("Nicht unterstütztes XML-Attribut im Profil.");
            }
        }
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) validate((Element) node, schema, depth + 1, count);
        }
    }

    private static Element child(Element element, String name) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && node.getNodeName().equals(name)) return (Element) node;
        }
        return null;
    }

    private static String value(Element element, String... path) {
        for (String name : path) { element = child(element, name); if (element == null) return ""; }
        return element.getTextContent().trim();
    }

    private static byte[] unhex(String value) throws IOException {
        if (value.length() % 2 != 0) throw new IOException("Ungültige Kartendaten.");
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IOException("Ungültige Kartendaten.");
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static void decodeText(byte[] data, int depth, int parent, List<String> output, Map<Integer, String> components) throws IOException {
        if (depth > 12) throw new IOException("Ungültige Kartendaten.");
        int offset = 0;
        while (offset < data.length) {
            int firstTag = data[offset++] & 255;
            if ((firstTag & 31) == 31) {
                int extra;
                do {
                    if (offset >= data.length) throw new IOException("Ungültige Kartendaten.");
                    extra = data[offset++] & 255;
                } while ((extra & 128) != 0);
            }
            if (offset >= data.length) throw new IOException("Ungültige Kartendaten.");
            int length = data[offset++] & 255;
            if ((length & 128) != 0) {
                int count = length & 127;
                if (count == 0 || count > 3 || offset + count > data.length) throw new IOException("Ungültige Kartendaten.");
                length = 0;
                while (count-- > 0) length = (length << 8) | (data[offset++] & 255);
            }
            if (length > data.length - offset) throw new IOException("Ungültige Kartendaten.");
            byte[] content = java.util.Arrays.copyOfRange(data, offset, offset + length);
            if ((firstTag & 32) != 0) decodeText(content, depth + 1, firstTag, output, components);
            else if (firstTag == 12 || firstTag == 18 || firstTag == 19 || firstTag == 22) {
                String text = new String(content, StandardCharsets.UTF_8);
                output.add(text);
                components.put(parent, text);
            } else if (firstTag == 4) {
                StringBuilder hex = new StringBuilder();
                for (byte value : content) hex.append(String.format(Locale.ROOT, "%02X", value & 255));
                components.put(parent, hex.toString());
            }
            offset += length;
        }
    }

    private static String group(Map<Integer, String> groups, int number) {
        return groups.containsKey(number) ? groups.get(number) : "";
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) { if (result.length() > 0) result.append(", "); result.append(value); }
        return result.toString();
    }
}
