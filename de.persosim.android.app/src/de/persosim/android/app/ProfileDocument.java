package de.persosim.android.app;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ProfileDocument {
    private static final String CARD = "de.persosim.simulator.cardobjects.";

    public static byte[] create(InputStream template, ProfileData data) throws Exception {
        data.validate();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(template);
        Element eid = application(document, "E80704007F00070302");
        group(eid, 1, tlv(0x61, string(0x13, data.get("documentType"))));
        group(eid, 2, tlv(0x62, string(0x13, data.get("issuingCountry"))));
        group(eid, 3, tlv(0x63, string(0x12, data.compactDate("expiryDate"))));
        group(eid, 4, tlv(0x64, string(0x0C, data.get("givenName"))));
        group(eid, 5, tlv(0x65, string(0x0C, data.get("familyName"))));
        group(eid, 6, tlv(0x66, string(0x0C, data.get("artisticName"))));
        group(eid, 7, tlv(0x67, string(0x0C, data.get("academicTitle"))));
        group(eid, 8, tlv(0x68, string(0x12, data.compactDate("birthDate"))));
        group(eid, 9, tlv(0x69, tlv(0xA1, string(0x0C, data.get("birthPlace")))));
        group(eid, 10, tlv(0x6A, string(0x13, data.get("nationality"))));
        group(eid, 13, tlv(0x6D, string(0x0C, data.get("birthName"))));
        group(eid, 17, tlv(0x71, tlv(0x30, concat(
                tlv(0xAA, string(0x0C, data.get("street"))), tlv(0xAB, string(0x0C, data.get("city"))),
                tlv(0xAD, string(0x13, data.get("country"))), tlv(0xAE, string(0x13, data.get("postalCode")))))));
        byte[] community = new byte[7];
        for (int i = 0; i < community.length; i++) {
            community[i] = (byte) Integer.parseInt(data.get("communityId").substring(i * 2, i * 2 + 2), 16);
        }
        group(eid, 18, tlv(0x72, tlv(0x04, community)));
        NodeList auxiliary = document.getElementsByTagName(CARD + "ByteDataAuxObject");
        boolean communityUpdated = false;
        for (int i = 0; i < auxiliary.getLength(); i++) {
            Element object = (Element) auxiliary.item(i);
            if (child(child(child(object, "identifier"), "oid"), "oidByteArray").getTextContent().equals("04007F000703010403")) {
                child(object, "data").setTextContent(hex(community));
                communityUpdated = true;
            }
        }
        if (!communityUpdated) throw new IllegalArgumentException("Wohnortprüfung fehlt in der Vorlage");

        String mrz = mrz(data);
        Element auth = (Element) document.getElementsByTagName(CARD + "MrzAuthObject").item(0);
        child(auth, "mrz").setTextContent(mrz);
        String digestInput = mrz.substring(5, 15) + mrz.substring(30, 37) + mrz.substring(38, 45);
        child(auth, "password").setTextContent(hex(MessageDigest.getInstance("SHA-1").digest(digestInput.getBytes(StandardCharsets.US_ASCII))));
        child(auth, "idPicc").setTextContent(hex(mrz.substring(5, 15).getBytes(StandardCharsets.US_ASCII)));
        group(application(document, "A0000002471001"), 1, tlv(0x61, string(0x5F1F, mrz)));
        NodeList dates = document.getElementsByTagName(CARD + "DateAuxObject");
        SimpleDateFormat localDate = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
        localDate.setLenient(false);
        localDate.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        SimpleDateFormat xmlDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S 'UTC'", Locale.ROOT);
        xmlDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        boolean birthUpdated = false;
        boolean expiryUpdated = false;
        for (int i = 0; i < dates.getLength(); i++) {
            Element date = (Element) dates.item(i);
            String oid = child(child(child(date, "identifier"), "oid"), "oidByteArray").getTextContent();
            if (oid.equals("04007F000703010401")) {
                child(date, "date").setTextContent(xmlDate.format(localDate.parse(data.get("birthDate"))));
                birthUpdated = true;
            } else if (oid.equals("04007F000703010402")) {
                child(date, "date").setTextContent(xmlDate.format(localDate.parse(data.get("expiryDate"))));
                expiryUpdated = true;
            }
        }
        if (!birthUpdated || !expiryUpdated) throw new IllegalArgumentException("Prüfdatum fehlt in der Vorlage");
        NodeList names = document.getElementsByTagName("passwordName");
        for (int i = 0; i < names.getLength(); i++) {
            Node name = names.item(i);
            String key = name.getTextContent().toLowerCase(Locale.ROOT);
            if (key.equals("pin") || key.equals("can") || key.equals("puk")) {
                child((Element) name.getParentNode(), "password").setTextContent(hex(data.get(key).getBytes(StandardCharsets.US_ASCII)));
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private static Element application(Document document, String aid) {
        NodeList files = document.getElementsByTagName(CARD + "DedicatedFile");
        for (int i = 0; i < files.getLength(); i++) {
            Element file = (Element) files.item(i);
            if (child(child(file, "dedicatedFileName"), "fileName").getTextContent().equals(aid)) return file;
        }
        throw new IllegalArgumentException("Anwendung fehlt in der Vorlage: " + aid);
    }

    private static Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && node.getNodeName().equals(name)) return (Element) node;
        }
        throw new IllegalArgumentException("Vorlage ist unvollständig: " + name);
    }

    private static void group(Element application, int number, byte[] content) {
        Element files = child(application, "children");
        for (Node node = files.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element) || !node.getNodeName().equals(CARD + "ElementaryFile")) continue;
            Element file = (Element) node;
            if (child(child(file, "shortFileIdentifier"), "identifier").getTextContent().equals(String.valueOf(number))) {
                child(file, "content").setTextContent(hex(content));
                return;
            }
        }
        throw new IllegalArgumentException("Datengruppe fehlt in der Vorlage: " + number);
    }

    private static byte[] string(int tag, String value) { return tlv(tag, value.getBytes(StandardCharsets.UTF_8)); }

    private static byte[] tlv(int tag, byte[] value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (tag > 255) output.write(tag >> 8);
        output.write(tag);
        if (value.length < 128) output.write(value.length);
        else if (value.length < 256) { output.write(0x81); output.write(value.length); }
        else { output.write(0x82); output.write(value.length >> 8); output.write(value.length); }
        output.write(value, 0, value.length);
        return output.toByteArray();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] part : parts) output.write(part, 0, part.length);
        return output.toByteArray();
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02X", value & 255));
        return result.toString();
    }

    private static char checkDigit(String value) {
        int sum = 0;
        int[] weights = {7, 3, 1};
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            int digit = c == '<' ? 0 : c >= 'A' ? c - 'A' + 10 : c - '0';
            sum += digit * weights[i % 3];
        }
        return (char) ('0' + sum % 10);
    }

    private static String padded(String value, int length) {
        StringBuilder result = new StringBuilder(value);
        while (result.length() < length) result.append('<');
        return result.substring(0, length);
    }

    private static String mrzName(String value) {
        String expanded = value.toUpperCase(Locale.ROOT).replace("ẞ", "SS")
                .replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE");
        return Normalizer.normalize(expanded, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Z]+", "<").replaceAll("^<|<$", "");
    }

    private static String mrz(ProfileData data) {
        String number = data.get("documentNumber");
        String birth = data.compactDate("birthDate").substring(2);
        String expiry = data.compactDate("expiryDate").substring(2);
        String first = data.get("documentType") + padded(data.get("issuingCountry"), 3) + number + checkDigit(number) + padded("", 15);
        String second = birth + checkDigit(birth) + (data.get("sex").equals("X") ? "<" : data.get("sex"))
                + expiry + checkDigit(expiry) + padded(data.get("nationality"), 3) + padded("", 11);
        second += checkDigit(first.substring(5) + second.substring(0, 7) + second.substring(8, 15) + second.substring(18));
        return first + second + padded(mrzName(data.get("familyName")) + "<<" + mrzName(data.get("givenName")), 30);
    }
}
