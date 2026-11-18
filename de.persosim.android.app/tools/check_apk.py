#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from zipfile import ZipFile

from profile_assets import ROOT, editor_template, import_schema, original_profiles, profile_info


def verify(path):
    profiles = original_profiles()
    with ZipFile(path) as apk:
        expected = {"assets/" + profile.name for profile in profiles}
        actual = {name for name in apk.namelist() if name.endswith(".perso")}
        if actual != expected:
            raise ValueError("APK contains an unexpected profile set")
        for profile in profiles:
            if apk.read("assets/" + profile.name) != profile.read_bytes():
                raise ValueError("APK contains a modified original: " + profile.name)
        catalog = [profile_info(profile) for profile in profiles]
        catalog.sort(key=lambda profile: profile["file"] != "Profile01.perso")
        if json.loads(apk.read("assets/profiles.json")) != catalog:
            raise ValueError("APK profile catalog differs from original profiles")
        if json.loads(apk.read("assets/editor-template.json")) != editor_template(ROOT / "profiles/Profile01.perso"):
            raise ValueError("APK contains a different editor template")
        if apk.read("assets/import-schema.txt").decode().splitlines() != import_schema():
            raise ValueError("APK contains an unexpected import schema")
        if not any(name.endswith(".dex") for name in apk.namelist()):
            raise ValueError("APK has no compiled code")
    print("APK verified: all 17 original profiles and generated metadata match")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
