#!/usr/bin/env python3
"""Regression tests for the repository key scanner (no real credentials)."""
import contextlib
import hashlib
import importlib.util
import io
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

path = Path(__file__).with_name("check-supabase-keys.py")
spec = importlib.util.spec_from_file_location("key_scanner", path)
scanner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scanner)


class KeyScannerTest(unittest.TestCase):
    def test_fixed_test_key_is_allowed(self):
        fake = b"sb_" + b"publishable_" + b"fake_for_test"
        self.assertFalse(scanner.candidate_is_forbidden(fake))
        self.assertEqual(scanner.scan(fake), [])

    def test_words_fake_or_test_do_not_make_an_unapproved_key_safe(self):
        candidate = b"sb_" + b"secret_" + b"fake_test_0123456789abcdef"
        self.assertTrue(scanner.candidate_is_forbidden(candidate))
        self.assertEqual(scanner.scan(b"first line\n" + candidate), [2])

    def test_partial_disclosed_fingerprint_is_recognized(self):
        fingerprint = "eab0b605" + "0" * 51 + "3feb1"
        self.assertTrue(scanner.has_disclosed_fingerprint(fingerprint))
        self.assertFalse(scanner.has_disclosed_fingerprint("0" * 64))

    def test_known_disclosed_candidate_in_an_ignored_file_is_found_without_printing_it(self):
        candidate = b"sb_" + b"secret_" + b"fictional_value_1234567890"
        digest = hashlib.sha256(candidate).hexdigest()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "ignored-config").write_bytes(candidate)
            output = io.StringIO()
            with patch.object(scanner, "ROOT", root), \
                    patch.object(scanner.subprocess, "run", return_value=SimpleNamespace(stdout=b"")), \
                    patch.object(scanner, "has_disclosed_fingerprint", side_effect=lambda value: value == digest), \
                    contextlib.redirect_stdout(output):
                self.assertEqual(scanner.main(), 1)
            self.assertIn("ignored-config:1", output.getvalue())
            self.assertNotIn(candidate.decode(), output.getvalue())

    def test_binary_or_non_key_text_is_not_flagged(self):
        self.assertEqual(scanner.scan(b"\x00other_text\xff"), [])


if __name__ == "__main__":
    unittest.main()
