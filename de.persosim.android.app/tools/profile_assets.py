#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def original_profiles():
    manifest = json.loads((ROOT / "original-profiles.json").read_text())
    paths = sorted((ROOT / "profiles").glob("*.perso"))
    if {path.name for path in paths} != set(manifest["files"]):
        raise ValueError("Only the original profile set may be bundled")
    for path in paths:
        if hashlib.sha256(path.read_bytes()).hexdigest() != manifest["files"][path.name]:
            raise ValueError(f"Modified profile cannot be bundled: {path.name}")
    return paths


def decode_text(data):
    result = []
    offset = 0
    while offset < len(data):
        tag = data[offset]
        offset += 1
        length = data[offset]
        offset += 1
        if length & 128:
            count = length & 127
            length = int.from_bytes(data[offset:offset + count], "big")
            offset += count
        end = offset + length
        if end > len(data):
            raise ValueError("Invalid profile TLV length")
        value = data[offset:end]
        offset = end
        if tag & 32:
            result.extend(decode_text(value))
        elif tag in (12, 18, 19, 22):
            result.append(value.decode("utf-8"))
    return result


def profile_info(path):
    root = ET.parse(path).getroot()
    df = next(d for d in root.iter("de.persosim.simulator.cardobjects.DedicatedFile")
              if d.findtext("dedicatedFileName/fileName") == "E80704007F00070302")
    values = {}
    for ef in df.findall("children/de.persosim.simulator.cardobjects.ElementaryFile"):
        sfi = int(ef.findtext("shortFileIdentifier/identifier"))
        values[sfi] = decode_text(bytes.fromhex(ef.findtext("content", "")))

    def value(sfi):
        return ", ".join(values.get(sfi, [])) or "Nicht hinterlegt"

    def date(sfi):
        raw = value(sfi)
        return f"{raw[6:8]}.{raw[4:6]}.{raw[:4]}" if re.fullmatch(r"\d{8}", raw) else raw

    person = " ".join(values.get(4, []) + values.get(5, [])).strip()
    title = f"{path.stem} · {person}" if person else path.stem
    credentials = {}
    for element in root.iter():
        name = element.findtext("passwordName")
        password = element.findtext("password")
        if name in ("PIN", "CAN", "PUK") and password:
            credentials[name] = bytes.fromhex(password).decode("ascii")
    address = values.get(17, [])
    mrz = (root.findtext(".//mrz") or "").strip()
    document_number = mrz[5:14].rstrip("<") if len(mrz) == 90 else ""
    details = ["Person", f"Ausweisnummer: {document_number or 'Nicht hinterlegt'}",
               f"Vorname: {value(4)}", f"Nachname: {value(5)}",
               f"Geburtsdatum: {date(8)}", f"Geburtsort: {value(9)}",
               f"Geburtsname: {value(13)}", f"Staatsangehörigkeit: {value(10)}",
               f"Gültig bis: {date(3)}", "", "Anschrift", *address, "", "Test-Zugangsdaten"]
    details.extend(f"{key}: {credentials.get(key, 'Nicht hinterlegt')}" for key in ("PIN", "CAN", "PUK"))
    details.extend(["", "Profil", f"Datei: {path.name}"])
    details.append("Unverändertes Originalprofil aus PersoSim 0.17.2")
    return {"file": path.name, "title": title,
            "documentType": ", ".join(values.get(1, [])),
            "subtitle": date(8),
            "details": "\n".join(details)}


def editor_template(path):
    root = ET.parse(path).getroot()
    df = next(d for d in root.iter("de.persosim.simulator.cardobjects.DedicatedFile")
              if d.findtext("dedicatedFileName/fileName") == "E80704007F00070302")
    groups = {int(ef.findtext("shortFileIdentifier/identifier")): decode_text(bytes.fromhex(ef.findtext("content")))
              for ef in df.findall("children/de.persosim.simulator.cardobjects.ElementaryFile")}
    data = {key: groups[number][0] for key, number in {
        "givenName": 4, "familyName": 5, "birthName": 13, "birthPlace": 9, "nationality": 10,
        "artisticName": 6, "academicTitle": 7}.items()}
    for key, number in {"birthDate": 8, "expiryDate": 3}.items():
        value = groups[number][0]
        data[key] = f"{value[6:8]}.{value[4:6]}.{value[:4]}"
    data.update(zip(("street", "city", "country", "postalCode"), groups[17]))
    for element in root.iter():
        name = element.findtext("passwordName", "").lower()
        if name in ("pin", "can", "puk"):
            data[name] = bytes.fromhex(element.findtext("password")).decode("ascii")
    mrz = root.findtext(".//mrz")
    community = next(e.findtext("data") for e in root.iter("de.persosim.simulator.cardobjects.ByteDataAuxObject")
                     if e.findtext("identifier/oid/oidByteArray") == "04007F000703010403")
    data.update(title="Eigener Ausweis", documentNumber=mrz[5:14], sex=mrz[37], documentType=groups[1][0],
                issuingCountry=groups[2][0], communityId=community)
    return data


def import_schema():
    schema = set()
    for path in original_profiles():
        for element in ET.parse(path).iter():
            schema.add(element.tag)
            if element.get("class"):
                schema.add(element.get("class"))
    schema.update(f"de.persosim.simulator.perso.Profile{index:02}" for index in range(1, 12))
    return sorted(schema)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    paths = original_profiles()
    args.output.mkdir(parents=True, exist_ok=True)
    for existing in args.output.iterdir():
        if existing.is_file():
            existing.unlink()
    for path in paths:
        shutil.copy2(path, args.output / path.name)
    catalog = [profile_info(path) for path in paths]
    catalog.sort(key=lambda profile: profile["file"] != "Profile01.perso")
    (args.output / "profiles.json").write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")
    (args.output / "editor-template.json").write_text(json.dumps(editor_template(ROOT / "profiles/Profile01.perso"), ensure_ascii=False, indent=2), encoding="utf-8")
    (args.output / "import-schema.txt").write_text("\n".join(import_schema()) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
