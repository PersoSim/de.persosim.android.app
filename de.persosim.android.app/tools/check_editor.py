#!/usr/bin/env python3
"""Exercise the production XML writer on the JVM and verify its card data independently."""
import hashlib
import json
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

from profile_assets import ROOT, editor_template


def tlv(data):
    result = []
    offset = 0
    while offset < len(data):
        tag = data[offset]
        offset += 1
        if tag & 31 == 31:
            tag = tag << 8 | data[offset]
            offset += 1
        length = data[offset]
        offset += 1
        if length & 128:
            count = length & 127
            length = int.from_bytes(data[offset:offset + count], "big")
            offset += count
        end = offset + length
        assert end <= len(data)
        result.append((tag, data[offset:end]))
        offset = end
    assert offset == len(data)
    return result


def groups(root, aid):
    df = next(d for d in root.iter("de.persosim.simulator.cardobjects.DedicatedFile")
              if d.findtext("dedicatedFileName/fileName") == aid)
    return {int(f.findtext("shortFileIdentifier/identifier")): bytes.fromhex(f.findtext("content"))
            for f in df.findall("children/de.persosim.simulator.cardobjects.ElementaryFile")}


def digit(value):
    alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return str(sum((0 if c == "<" else alphabet.index(c)) * (7, 3, 1)[i % 3]
                   for i, c in enumerate(value)) % 10)


def main():
    template = ROOT / "profiles/Profile01.perso"
    data = editor_template(template)
    with tempfile.TemporaryDirectory(prefix="persosim-editor-check-") as temporary:
        work = Path(temporary)
        assignments = "\n".join(f"values.put({json.dumps(k)}, {json.dumps(v, ensure_ascii=False)});" for k, v in data.items())
        harness = r'''
import de.persosim.android.app.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public class EditorCheck {
    static Map<String, String> values = new LinkedHashMap<>();
    static void write(String source, String destination) throws Exception {
        try (InputStream input = new FileInputStream(source)) {
            Files.write(Paths.get(destination), ProfileDocument.create(input, new ProfileData(values)));
        }
    }
    static void invalid(String key, String value) {
        Map<String, String> changed = new LinkedHashMap<>(values);
        changed.put(key, value);
        try { new ProfileData(changed).validate(); throw new AssertionError("Accepted invalid " + key); }
        catch (ProfileData.InvalidField e) { if (!e.key.equals(key)) throw new AssertionError(e); }
    }
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        ASSIGNMENTS
        write(args[0], args[1] + "/default.xml");
        for (String zone : new String[]{"UTC", "Asia/Tokyo", "America/Los_Angeles"}) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            write(args[0], args[1] + "/timezone.xml");
            if (!Arrays.equals(Files.readAllBytes(Paths.get(args[1], "default.xml")),
                    Files.readAllBytes(Paths.get(args[1], "timezone.xml")))) throw new AssertionError("Profile changes with timezone " + zone);
        }
        invalid("birthDate", "31.02.2000");
        invalid("expiryDate", "1.01.2029");
        invalid("documentNumber", "12345678");
        invalid("pin", "12345a");
        invalid("puk", "123");
        invalid("street", "A\u0000B");
        invalid("country", "DEUTSCHLAND");
        invalid("sex", "other");
        invalid("documentType", "OA");
        invalid("documentType", "XX");
        invalid("issuingCountry", "DEUTSCHLAND");
        invalid("communityId", "0276050315000A");
        invalid("communityId", "123");
        Map<String, String> legacy = new LinkedHashMap<>(values);
        legacy.remove("documentType"); legacy.remove("issuingCountry"); legacy.remove("communityId");
        ProfileData old = new ProfileData(legacy);
        old.validate();
        for (String key : new String[]{"documentType", "issuingCountry", "communityId"}) {
            if (!old.get(key).equals(values.get(key))) throw new AssertionError("Wrong legacy default " + key);
        }
        values.put("documentNumber", "C01X23456");
        values.put("givenName", "Jörg");
        values.put("familyName", "Weiß-Müller");
        values.put("birthDate", "29.02.2000");
        values.put("expiryDate", "31.12.2035");
        values.put("sex", "X");
        values.put("birthName", "");
        values.put("artisticName", "");
        values.put("academicTitle", "");
        values.put("street", String.join("", Collections.nCopies(40, "Äöß")));
        values.put("city", "Köln & <Bonn>");
        values.put("pin", "654321");
        values.put("can", "123456");
        values.put("puk", "0123456789");
        write(args[0], args[1] + "/edited.xml");
        values.put("issuingCountry", "aut");
        values.put("communityId", "04000102030405");
        for (String type : new String[]{"ID", "UB", "AR", "AS", "AF"}) {
            values.put("documentType", type.toLowerCase(Locale.ROOT));
            write(args[0], args[1] + "/" + type + ".xml");
        }
    }
}
'''.replace("ASSIGNMENTS", assignments)
        (work / "EditorCheck.java").write_text(harness)
        source = ROOT / "src/de/persosim/android/app"
        subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(work),
                        str(source / "DocumentType.java"), str(source / "ProfileData.java"), str(source / "ProfileDocument.java"), str(work / "EditorCheck.java")], check=True)
        subprocess.run(["java", "-cp", str(work), "EditorCheck", str(template), str(work)], check=True)
        original = ET.parse(template).getroot()
        original_groups = groups(original, "E80704007F00070302")
        for name in ("default", "edited", "ID", "UB", "AR", "AS", "AF"):
            root = ET.parse(work / f"{name}.xml").getroot()
            current = groups(root, "E80704007F00070302")
            if name == "default":
                assert current == original_groups
            else:
                assert tlv(tlv(current[4])[0][1])[0][1].decode() == "Jörg"
                assert tlv(tlv(current[5])[0][1])[0][1].decode() == "Weiß-Müller"
                address = tlv(tlv(tlv(current[17])[0][1])[0][1])
                assert [tlv(v)[0][1].decode() for _, v in address] == ["Äöß" * 40, "Köln & <Bonn>", "D", "51147"]
                assert len(current[17]) > 255
                if name == "edited":
                    for group in (1, 2, 18):
                        assert current[group] == original_groups[group]
            community = "02760503150000" if name in ("default", "edited") else "04000102030405"
            assert tlv(tlv(current[18])[0][1]) == [(4, bytes.fromhex(community))]
            aux = next(n for n in root.iter("de.persosim.simulator.cardobjects.ByteDataAuxObject")
                       if n.findtext("identifier/oid/oidByteArray") == "04007F000703010403")
            assert aux.findtext("data") == community
            auth = root.find(".//de.persosim.simulator.cardobjects.MrzAuthObject")
            mrz = auth.findtext("mrz")
            assert len(mrz) == 90
            if name not in ("default", "edited"):
                assert mrz[:5] == name + "AUT"
                assert tlv(tlv(current[1])[0][1]) == [(0x13, name.encode())]
                assert tlv(tlv(current[2])[0][1]) == [(0x13, b"AUT")]
            assert mrz[14] == digit(mrz[5:14])
            assert mrz[36] == digit(mrz[30:36])
            assert mrz[44] == digit(mrz[38:44])
            assert mrz[59] == digit(mrz[5:30] + mrz[30:37] + mrz[38:45] + mrz[48:59])
            assert auth.findtext("password") == hashlib.sha1((mrz[5:15] + mrz[30:37] + mrz[38:45]).encode()).hexdigest().upper()
            assert bytes.fromhex(auth.findtext("idPicc")).decode() == mrz[5:15]
            passport = groups(root, "A0000002471001")
            assert tlv(tlv(passport[1])[0][1]) == [(0x5F1F, mrz.encode())]
            before_passport = groups(original, "A0000002471001")
            assert {k: v for k, v in passport.items() if k != 1} == {k: v for k, v in before_passport.items() if k != 1}
            if name == "edited":
                assert mrz[5:14] == "C01X23456" and mrz[30:36] == "000229" and mrz[38:44] == "351231"
                assert mrz[37] == "<" and "WEISS<MUELLER<<JOERG" in mrz
                for key, expected in {"PIN": "654321", "CAN": "123456", "PUK": "0123456789"}.items():
                    node = next(n for n in root.iter() if n.findtext("passwordName") == key)
                    assert bytes.fromhex(node.findtext("password")).decode() == expected
            aux_dates = {n.findtext("identifier/oid/oidByteArray"): n.findtext("date")
                         for n in root.iter("de.persosim.simulator.cardobjects.DateAuxObject")}
            assert aux_dates == {
                "04007F000703010401": "1964-08-11 23:00:00.0 UTC" if name == "default" else "2000-02-28 23:00:00.0 UTC",
                "04007F000703010402": "2029-10-30 23:00:00.0 UTC" if name == "default" else "2035-12-30 23:00:00.0 UTC"}
            assert [(n.tag, n.attrib) for n in original.iter()] == [(n.tag, n.attrib) for n in root.iter()]
        print("Editor checks passed: template DGs, Unicode, long DER, XML references, validation, TD1/MRZ/PACE, passport DG1, credentials, all editor document types, issuing state, legacy defaults, auxiliary birth/expiry/residence values.")


if __name__ == "__main__":
    main()
