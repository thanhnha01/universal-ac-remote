import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import build_ir_catalog as catalog
import sync_upstreams as sync


SHA = "1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f"


class CatalogTests(unittest.TestCase):
    def test_brand_normalization(self):
        self.assertEqual(catalog.normalize_brand("Mitsubishi Electric"), "mitsubishielectric")
        self.assertEqual(catalog.normalize_brand("GREE / Gree"), "greegree")

    def test_provenance_and_registry_import(self):
        text = catalog.REGISTRY.read_text(encoding="utf-8")
        records = catalog.parse_irremote_registry(text)
        self.assertEqual(len(records), 8)
        self.assertTrue(all(r["sourceCommitSha"] == SHA and r["sourcePath"].endswith("ProtocolRegistry.kt") for r in records))
        with self.assertRaises(ValueError): catalog.base_profile(source="x", sha="main", path="x", source_id="x", brand="X", ac_model=None, remote_model=None, protocol_id=None, variant=None, encoding="PROTOCOL", capabilities=[], temp=None, fans=[], modes=[], v_swing={"type":"NONE","positions":[]}, h_swing={"type":"NONE","positions":[]}, special=[], verification="candidate")

    def test_swing_support_is_explicit_and_never_inferred_as_positions(self):
        records = catalog.parse_irremote_registry(catalog.REGISTRY.read_text(encoding="utf-8"))
        for record in records:
            self.assertIn(record["verticalSwingCapabilities"]["type"], {"NONE", "ON_OFF"})
            self.assertIn(record["horizontalSwingCapabilities"]["type"], {"NONE", "ON_OFF"})
            self.assertEqual(record["verticalSwingCapabilities"]["positions"], [])

    def test_duplicate_detection_does_not_merge(self):
        records = catalog.parse_irremote_registry(catalog.REGISTRY.read_text(encoding="utf-8"))
        self.assertEqual(catalog.find_duplicates(records), [])
        records.append(dict(records[0]))
        candidates = catalog.find_duplicates(records)
        self.assertEqual(len(candidates), 1)
        self.assertEqual(len(candidates[0]["profileIds"]), 2)
        self.assertEqual(len(records), 9)

    def test_unknown_profiles_with_different_source_ids_are_not_duplicates(self):
        first = {"id": "smartir:100", "sourceProfileId": "100", "normalizedBrand": "midea",
                 "sourcePath": "codes/climate/100.json", "acModel": "Unknown", "remoteModel": None,
                 "protocolId": None, "protocolModel": None}
        second = dict(first, id="smartir:101", sourceProfileId="101")
        self.assertEqual(catalog.find_duplicates([first, second]), [])

    def test_malformed_flipper_profile_rejected(self):
        text = "Filetype: IR signals\nVersion: 1\n#\nname: Cool\ntype: raw\nfrequency: 38000\ndata: 900 450 400\n"
        self.assertEqual(len(catalog.parse_flipper_text(text, SHA, "ACs/Brand/model.ir")[0]["rawCommands"]["Cool"]["durationsMicros"]), 3)
        self.assertEqual(catalog.parse_flipper_text(text, SHA, "TVs/Brand/model.ir"), [])

    def test_flipper_raw_transmission_validation_is_fail_closed(self):
        text = "Filetype: IR signals\n#\nname: Power\ntype: raw\nfrequency: 38000\ndata: 900 450 560 560\n"
        profile = catalog.parse_flipper_text(text, SHA, "ACs/Brand/model.ir")[0]
        self.assertTrue(catalog.is_transmittable(profile))
        bad_frequency = dict(profile, rawCommands={"Power": {"carrierFrequencyHz": 0, "durationsMicros": [900, 450]}})
        bad_timing = dict(profile, rawCommands={"Power": {"carrierFrequencyHz": 38000, "durationsMicros": [900, 0]}})
        self.assertFalse(catalog.is_transmittable(bad_frequency))
        self.assertFalse(catalog.is_transmittable(bad_timing))

    def test_flipper_parsed_encoding_is_not_transmittable(self):
        text = "Filetype: IR signals\n#\nname: Power\ntype: parsed\nprotocol: NEC\naddress: 1\ncommand: 2\n"
        profile = catalog.parse_flipper_text(text, SHA, "ACs/Brand/model.ir")[0]
        self.assertEqual(profile["encodingType"], "FLIPPER_PARSED_UNSUPPORTED")
        self.assertFalse(catalog.is_transmittable(profile))

    def test_smartir_climate_only_and_full_sha(self):
        folder = Path("fixture")
        self.assertEqual(catalog.selected_input_paths("smartir", folder), [])
        self.assertEqual(catalog.selected_input_paths("smartir", folder / "codes"), [])
        self.assertEqual(catalog.normalize_brand("Midea"), "midea")
        record = catalog.base_profile(source="smartir", sha=SHA, path="codes/climate/1.json", source_id="1", brand="Midea", ac_model=None, remote_model=None, protocol_id=None, variant=None, encoding="IMPORTED_RAW", capabilities=[], temp=None, fans=[], modes=[], v_swing={"type":"NONE","positions":[]}, h_swing={"type":"NONE","positions":[]}, special=[], verification="candidate")
        self.assertEqual(record["sourceCommitSha"], SHA)

    def test_flipper_ac_only_filter(self):
        self.assertTrue(catalog.flipper_path_is_ac("ACs/LG/model.ir"))
        self.assertTrue(catalog.flipper_path_is_ac("ACs\\LG\\model.ir"))
        self.assertFalse(catalog.flipper_path_is_ac("TVs/LG/model.ir"))
        self.assertFalse(catalog.flipper_path_is_ac("Fans/LG/model.ir"))

    def test_flipper_license_cutoff_fails_closed(self):
        self.assertTrue(sync.first_add_is_cc0_safe(0))
        self.assertFalse(sync.first_add_is_cc0_safe(1))
        self.assertFalse(sync.first_add_is_cc0_safe(None))

    def test_unclear_license_source_is_not_ingestable(self):
        lock = {"irplus":{"status":"EXCLUDED_LICENSE_UNCLEAR","licenseId":None},
                "smartir":{"status":"ACTIVE","licenseId":"MIT"}}
        self.assertFalse(catalog.license_is_clear("irplus", lock))
        self.assertTrue(catalog.license_is_clear("smartir", lock))
        profiles, report = catalog.build()
        self.assertEqual(report["profilesBySource"]["irplus"], 0)
        self.assertTrue(all(p["source"] != "irplus" for p in profiles))

    def test_smartir_profile_retains_commands_and_untyped_swing(self):
        import json, tempfile
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "123.json"
            p.write_text(json.dumps({"manufacturer":"Example", "supportedModels":["M1"], "commandsEncoding":"Base64", "minTemperature":16.5,
                "maxTemperature":30.5,"operationModes":["cool"],"fanModes":["low"],"swingModes":["auto","Top","Bottom"],
                "commands":{"off":"c29tZQ==","cool":{"low":{"22":"Y29kZQ=="}},"sleep":"c2xlZXA="}}), encoding="utf-8")
            record = catalog.parse_smartir(p, SHA)[0]
            self.assertEqual(record["rawCommands"]["off"], "c29tZQ==")
            self.assertEqual(record["verticalSwingCapabilities"], {"type":"NONE","positions":[]})
            self.assertEqual(record["sourceMetadata"]["swingModes"], ["auto","Top","Bottom"])
            self.assertIn("sleep", record["specialCapabilities"])
            self.assertIn("special:sleep", record["capabilities"])

    def test_broadlink_decoder_accepts_valid_packet_and_rejects_malformed_packets(self):
        valid = "JgAEA AECAwQ=".replace(" ", "")
        self.assertEqual(catalog.decode_broadlink(valid), [32, 65, 98, 131])
        for encoded in ("not-base64", "sgAEA AECAwQ=".replace(" ", ""), "JgAFA AECAwQ=".replace(" ", "")):
            with self.assertRaises(ValueError): catalog.decode_broadlink(encoded)

    def test_smartir_transmittable_status_is_decoded_not_assumed(self):
        import base64
        packet = base64.b64encode(bytes([0x26, 0, 4, 0, 1, 2, 3, 4])).decode()
        record = catalog.base_profile(source="smartir", sha=SHA, path="codes/climate/1.json", source_id="1",
            brand="Example", ac_model="Unknown", remote_model=None, protocol_id=None, variant=None,
            encoding="RAW_PROFILE", capabilities=[], temp={"minC": 16, "maxC": 30}, fans=["low"], modes=["cool"],
            v_swing={"type":"NONE","positions":[]}, h_swing={"type":"NONE","positions":[]}, special=[], verification="candidate")
        record["rawCommands"] = {"off": packet}
        record["sourceMetadata"] = {"supportedController":"Broadlink", "commandsEncoding":"Base64"}
        self.assertTrue(catalog.is_transmittable(record))
        record["rawCommands"] = {"off": "not-base64"}
        self.assertFalse(catalog.is_transmittable(record))

    def test_built_catalog_uses_exact_source_shas_and_climate_profiles(self):
        profiles, report = catalog.build()
        lock = {x["name"]:x for x in __import__("json").loads((catalog.ROOT / "upstream-lock.json").read_text(encoding="utf-8"))["sources"]}
        for profile in profiles:
            self.assertEqual(profile["sourceCommitSha"], lock[profile["source"]]["commitSha"])
        smart = [p for p in profiles if p["source"] == "smartir"]
        self.assertGreater(len(smart), 0)
        self.assertTrue(all(p["sourcePath"].startswith("codes/climate/") for p in smart))
        self.assertEqual(report["validationStatus"], "PASS")
        self.assertEqual(report["smartirTotal"], report["smartirTransmittable"] + report["smartirUnsupported"])
        self.assertEqual(report["flipperCutoffExcludedFiles"], 131)
        self.assertEqual(report["flipperMissingProvenanceFiles"], 25)
        self.assertIn("flipperFilesScanned", report)
        self.assertIn("flipperTransmittableProfiles", report)

    def test_mobile_index_preserves_capabilities_without_raw_commands(self):
        profile = {
            "id": "fixture:1", "brand": "Example", "aliases": ["OEM"], "acModel": "AC-1",
            "remoteModel": "R-1", "encodingType": "PROTOCOL", "capabilities": ["power"],
            "verticalSwingCapabilities": {"type": "ON_OFF", "positions": []},
            "rawCommands": {"power": "large payload"}, "sourceCommitSha": "a" * 40,
        }
        indexed = catalog.to_mobile_index([profile])["profiles"][0]
        self.assertEqual(indexed["aliases"], ["OEM"])
        self.assertEqual(indexed["verticalSwingCapabilities"]["type"], "ON_OFF")
        self.assertNotIn("rawCommands", indexed)


if __name__ == "__main__": unittest.main()
