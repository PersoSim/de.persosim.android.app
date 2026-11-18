#!/usr/bin/env python3
"""Validate real profile imports and reject malformed or unsupported XML on the JVM."""
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

from profile_assets import ROOT, import_schema
from check_editor import tlv


def main():
    with tempfile.TemporaryDirectory(prefix="persosim-import-check-") as temporary:
        work = Path(temporary)
        (work / "schema.txt").write_text("\n".join(import_schema()))
        root = ET.parse(ROOT / "profiles/Profile01.perso").getroot()
        given = next(n for n in root.iter("content") if n.text == "64070C054552494B41")
        value = ("A" * 159 + "\U0001f642").encode()
        content = b"\x0c\x81" + bytes([len(value)]) + value
        given.text = (b"\x64\x81" + bytes([len(content)]) + content).hex().upper()
        ET.ElementTree(root).write(work / "unicode-title.perso", encoding="utf-8")
        address = ET.parse(ROOT / "profiles/Profile01.perso").getroot()
        eid = next(n for n in address.iter("de.persosim.simulator.cardobjects.DedicatedFile")
                   if n.findtext("dedicatedFileName/fileName") == "E80704007F00070302")
        file = next(n for n in eid.findall("children/de.persosim.simulator.cardobjects.ElementaryFile")
                    if n.findtext("shortFileIdentifier/identifier") == "17")
        parts = tlv(tlv(tlv(bytes.fromhex(file.findtext("content")))[0][1])[0][1])
        partial = b"".join(bytes([tag, len(value)]) + value for tag, value in parts if tag != 0xAA)
        wrapped = bytes([0x30, len(partial)]) + partial
        file.find("content").text = (bytes([0x71, len(wrapped)]) + wrapped).hex()
        ET.ElementTree(address).write(work / "partial-address.perso", encoding="utf-8")
        (work / "ImportCheck.java").write_text(r'''
import de.persosim.android.app.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.io.*;
public class ImportCheck {
    static Set<String> schema;
    static void rejects(byte[] xml) throws Exception {
        try { ProfileImport.parse(xml, "bad.perso", schema); throw new AssertionError("Accepted invalid profile"); }
        catch (IOException | org.xml.sax.SAXException e) { }
    }
    public static void main(String[] args) throws Exception {
        schema = new HashSet<>(Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8));
        String template = null;
        for (int i = 1; i < args.length; i++) {
            byte[] source = Files.readAllBytes(Paths.get(args[i]));
            ProfileImport parsed = ProfileImport.parse(source, "test.perso", schema);
            if (!Arrays.equals(source, parsed.xml)) throw new AssertionError("Import changed source");
            if (parsed.title.isEmpty() || !parsed.details.contains("Ausweisnummer:")) throw new AssertionError("Missing details");
            if (args[i].endsWith("Profile01.perso")) {
                template = new String(source, StandardCharsets.UTF_8);
                if (!parsed.title.equals("ERIKA MUSTERMANN")) throw new AssertionError(parsed.title);
                if (!parsed.details.contains("HEIDESTRAẞE 17, KÖLN, D, 51147")) throw new AssertionError(parsed.details);
                if (!parsed.details.contains("PIN: 123456")) throw new AssertionError(parsed.details);
                ProfileData fields = parsed.editorData;
                fields.validate();
                String[][] expected = {{"documentType", "ID"}, {"issuingCountry", "D"}, {"street", "HEIDESTRAẞE 17"},
                    {"city", "KÖLN"}, {"country", "D"}, {"postalCode", "51147"}, {"birthDate", "12.08.1964"},
                    {"expiryDate", "31.10.2029"}, {"communityId", "02760503150000"}, {"pin", "123456"}};
                for (String[] entry : expected) {
                    if (!fields.get(entry[0]).equals(entry[1])) throw new AssertionError("Wrong template " + entry[0] + ": " + fields.get(entry[0]));
                }
            }
        }
        String unknown = template.replace("610413024944", "610413025858");
        ProfileData unknownData = ProfileImport.parse(unknown.getBytes(StandardCharsets.UTF_8), "unknown.perso", schema).editorData;
        if (!unknownData.get("documentType").equals("XX")) throw new AssertionError("Unknown type silently replaced");
        byte[] partial = Files.readAllBytes(Paths.get(args[0]).resolveSibling("partial-address.perso"));
        ProfileData partialData = ProfileImport.parse(partial, "partial.perso", schema).editorData;
        if (!partialData.get("street").isEmpty() || !partialData.get("city").equals("KÖLN")
                || !partialData.get("country").equals("D") || !partialData.get("postalCode").equals("51147")) {
            throw new AssertionError("Partial address fields shifted");
        }
        rejects(new byte[0]);
        rejects(new byte[ProfileImport.MAX_BYTES + 1]);
        rejects("<not-a-profile/>".getBytes(StandardCharsets.UTF_8));
        rejects("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>".getBytes(StandardCharsets.UTF_8));
        rejects(template.replaceFirst("<layers id=\"2\">", "<layers id=\"2\" class=\"java.lang.ProcessBuilder\">").getBytes(StandardCharsets.UTF_8));
        rejects(template.replaceFirst("<layers id=\"2\">", "<layers id=\"2\" resolves-to=\"java.lang.ProcessBuilder\">").getBytes(StandardCharsets.UTF_8));
        rejects(template.replace("<masterFile id=\"6\">", "<masterFile id=\"6\"><java.lang.ProcessBuilder/>").getBytes(StandardCharsets.UTF_8));
        rejects(template.replace("<masterFile id=\"6\">", "<children>").replace("</masterFile>", "</children>").getBytes(StandardCharsets.UTF_8));
        rejects(template.replace("64070C054552494B41", "640F0C").getBytes(StandardCharsets.UTF_8));
        rejects(template.replace("<identifier>17</identifier>", "<identifier>invalid</identifier>").getBytes(StandardCharsets.UTF_8));
        String nested = template.replace("<children id=\"9\"/>", String.join("", Collections.nCopies(85, "<children>"))
                + String.join("", Collections.nCopies(85, "</children>")));
        rejects(nested.getBytes(StandardCharsets.UTF_8));
        rejects(new byte[]{(byte) 0xC3, 0x28});
        ProfileImport bom = ProfileImport.parse(("\uFEFF" + template).getBytes(StandardCharsets.UTF_8), "test.perso", schema);
        if (!Arrays.equals(bom.xml, template.getBytes(StandardCharsets.UTF_8))) throw new AssertionError("BOM normalization failed");
        byte[] longTitle = Files.readAllBytes(Paths.get(args[0]).resolveSibling("unicode-title.perso"));
        String title = ProfileImport.parse(longTitle, "test.perso", schema).title;
        if (title.length() != 159 || Character.isSurrogate(title.charAt(title.length() - 1))) throw new AssertionError("Split Unicode title");
        try { ProfileImport.readLimited(new ByteArrayInputStream(new byte[ProfileImport.MAX_BYTES + 1])); throw new AssertionError("Size limit ignored"); }
        catch (IOException expected) { }
        System.out.println("Import checks passed: all bundled profiles, byte preservation, editor template fields, partial addresses, unknown types, Unicode/details, malformed XML/TLV, external entities, unknown classes/attributes, depth/size limits and BOM.");
    }
}
''')
        source = ROOT / "src/de/persosim/android/app"
        subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(work),
                        *[str(source / name) for name in ("ProfileImport.java", "ProfileData.java", "DocumentType.java")],
                        str(work / "ImportCheck.java")], check=True)
        subprocess.run(["java", "-cp", str(work), "ImportCheck", str(work / "schema.txt"),
                        *map(str, sorted((ROOT / "profiles").glob("*.perso")))], check=True)


if __name__ == "__main__":
    main()
