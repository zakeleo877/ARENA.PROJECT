#!/usr/bin/env python3
"""Exercise release metadata without contacting GitHub or using a real keystore."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent
WORKFLOW = ROOT / ".github/workflows/build.yml"


def workflow_script(step_name):
    """Extract a literal run block so the test exercises the shipped workflow."""
    lines = WORKFLOW.read_text(encoding="utf-8").splitlines()
    start = lines.index(f"      - name: {step_name}") + 1
    end = next((i for i in range(start, len(lines)) if lines[i].startswith("      - name:")), len(lines))
    block = lines[start:end]
    run_start = block.index("        run: |") + 1
    script = []
    for line in block[run_start:]:
        if line and not line.startswith("          "):
            break
        script.append(line[10:] if line else "")
    return "\n".join(script) + "\n"


class ReleaseWorkflowTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="saeed-release-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for directory in ("bin", "artifacts/apk", "docs", "scripts"):
            (self.root / directory).mkdir(parents=True)
        for name in ("saeed-store-1.0.apk", "saeed-admin-1.0.apk"):
            (self.root / "artifacts/apk" / name).write_bytes(b"test")
        (self.root / "docs/START-HERE.html").write_text("test", encoding="utf-8")
        shutil.copy(ROOT / "scripts/verify-signing.sh", self.root / "scripts/verify-signing.sh")
        self.mode_file = self.root / "signing-mode.txt"
        self.capture = self.root / "gh-args"
        gh = self.root / "bin/gh"
        gh.write_text(
            "#!/usr/bin/env bash\n"
            "if [ \"$1 $2\" = 'release view' ]; then [ \"${GH_VIEW_EXISTS:-0}\" = 1 ]; exit; fi\n"
            "if [ \"$1 $2\" = 'release create' ]; then printf '%s\\0' \"$@\" > \"$GH_CAPTURE\"; exit 0; fi\n"
            "exit 1\n",
            encoding="utf-8",
        )
        apksigner = self.root / "bin/apksigner"
        apksigner.write_text(
            "#!/usr/bin/env bash\n"
            "if [ \"${NO_CERT:-0}\" = 1 ]; then exit 1; fi\n"
            "printf 'Signer #1 certificate SHA-256 digest: ABCDEF\\n'\n",
            encoding="utf-8",
        )
        gh.chmod(0o755)
        apksigner.chmod(0o755)
        self.env = dict(
            os.environ,
            PATH=f"{self.root / 'bin'}:{os.environ.get('PATH', '')}",
            GITHUB_RUN_NUMBER="999",
            GITHUB_SHA="abcdef0123456789",
            GITHUB_REPOSITORY="example/isolated",
            GH_CAPTURE=str(self.capture),
        )

    def run_step(self, name, mode, **env):
        self.mode_file.write_text(f"SIGNING_MODE={mode}\n", encoding="utf-8")
        script = workflow_script(name).replace("/tmp/signing-mode.txt", str(self.mode_file))
        script = script.replace("/tmp/verify-signing.txt", str(self.root / "verify-signing.txt"))
        return subprocess.run(
            ["bash", "-c", script], cwd=self.root, env={**self.env, **env},
            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
        )

    def arguments(self):
        return self.capture.read_bytes().decode("utf-8").split("\0")

    def test_temporary_key_is_published_as_a_prerelease(self):
        result = self.run_step("Publish the APKs as a release", "throwaway")
        self.assertEqual(result.returncode, 0, result.stderr)
        args = self.arguments()
        self.assertIn("--prerelease", args)
        self.assertTrue(any("مفتاح توقيع مؤقت" in arg for arg in args))

    def test_production_key_is_not_marked_as_a_prerelease(self):
        result = self.run_step("Publish the APKs as a release", "production")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("--prerelease", self.arguments())
        self.assertTrue(any("مفتاح الإنتاج" in arg for arg in self.arguments()))

    def test_unknown_key_never_publishes(self):
        result = self.run_step("Publish the APKs as a release", "unknown")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(self.capture.exists())

    def test_existing_release_is_not_replaced(self):
        result = self.run_step("Publish the APKs as a release", "throwaway", GH_VIEW_EXISTS="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(self.capture.exists())

    def test_signing_report_checks_production_and_rejects_unknown(self):
        production = self.run_step("Report the release signing key (acceptance check 10)", "production")
        self.assertEqual(production.returncode, 0, production.stderr)
        self.assertIn("permanent production key", production.stdout)
        missing_cert = self.run_step("Report the release signing key (acceptance check 10)", "production", NO_CERT="1")
        self.assertNotEqual(missing_cert.returncode, 0)
        unknown = self.run_step("Report the release signing key (acceptance check 10)", "unknown")
        self.assertNotEqual(unknown.returncode, 0)


if __name__ == "__main__":
    unittest.main()
