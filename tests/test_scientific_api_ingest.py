import importlib.util
import os
import pathlib
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("scientific_api_ingest", ROOT / "scripts" / "scientific_api_ingest.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MOD)


class ScientificApiIngestTests(unittest.TestCase):
    def setUp(self):
        self.cfg = MOD.load_config(ROOT / "configs" / "scientific-api-sources.v1.json")

    def test_no_github_pat_is_external_api_secret(self):
        allowed = set(self.cfg["secret_policy"]["allowed_external_secret_references"])
        self.assertFalse(allowed & MOD.FORBIDDEN_GITHUB_PATS)

    def test_power_is_keyless_and_forces_json(self):
        url, headers, secret_names = MOD.build_request(self.cfg, "NASA_POWER_DAILY_POINT", {
            "parameters": "T2M", "community": "AG", "longitude": -48.5, "latitude": -27.6,
            "start": "20260901", "end": "20260902", "format": "JSON"
        })
        self.assertIn("power.larc.nasa.gov/api/temporal/daily/point", url)
        self.assertIn("format=JSON", url)
        self.assertEqual(secret_names, set())
        self.assertNotIn("Authorization", headers)

    def test_unknown_query_key_fails_closed(self):
        source = MOD.get_source(self.cfg, "NASA_POWER_DAILY_POINT")
        with self.assertRaisesRegex(ValueError, "query_key_not_allowlisted"):
            MOD.validate_query(source, {"parameters": "T2M", "evil": "x"})

    def test_nasa_query_secret_is_redacted(self):
        with patch.dict(os.environ, {"NASA_API_KEY": "super-secret-key"}, clear=False):
            url, _, secret_names = MOD.build_request(self.cfg, "NASA_DONKI_CME", {
                "startDate": "2026-09-01", "endDate": "2026-09-02"
            })
        self.assertIn("super-secret-key", url)
        redacted = MOD.redact_url(url, secret_names)
        self.assertNotIn("super-secret-key", redacted)
        self.assertIn("api_key=REDACTED", redacted)

    def test_noaa_token_goes_to_header_not_url(self):
        with patch.dict(os.environ, {"NOAA_CDO_TOKEN": "noaa-secret"}, clear=False):
            url, headers, secret_names = MOD.build_request(self.cfg, "NOAA_CDO_DATA", {
                "datasetid": "GHCND", "startdate": "2026-09-01", "enddate": "2026-09-02"
            })
        self.assertNotIn("noaa-secret", url)
        self.assertEqual(headers["token"], "noaa-secret")
        self.assertEqual(secret_names, set())


if __name__ == "__main__":
    unittest.main()
