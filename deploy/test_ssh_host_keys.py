"""Exercise the actual workflow's SSH host-key verification with real OpenSSH."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class HostKeyVerificationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        workflow = (Path(__file__).resolve().parents[1] / ".github/workflows/deploy.yml").read_text()
        start = workflow.index("          ssh-keyscan ")
        end = workflow.index("          ssh_options=", start)
        cls.verification = "set -euo pipefail\n" + workflow[start:end]

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="gradinator-host-key-")
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        key = self.directory / "host-key"
        subprocess.run(["ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", str(key)], check=True)
        self.key = key.with_suffix(".pub").read_text().strip()
        self.record = "[example.invalid]:1313 " + self.key
        self.fingerprint = subprocess.check_output(
            ["ssh-keygen", "-lf", str(key.with_suffix(".pub"))], text=True
        ).split()[1]
        self.binary_directory = self.directory / "bin"
        self.binary_directory.mkdir()
        scanner = self.binary_directory / "ssh-keyscan"
        scanner.write_text('#!/bin/sh\ncat "$SCAN_FIXTURE"\nexit "${SCAN_STATUS:-0}"\n')
        scanner.chmod(0o700)

    def verify(self, scan, fingerprint=None, scan_status=0):
        fixture = self.directory / "fixture"
        fixture.write_text(scan)
        environment = dict(os.environ, PATH=str(self.binary_directory) + os.pathsep + os.environ["PATH"],
                           SCAN_FIXTURE=str(fixture), SCAN_STATUS=str(scan_status),
                           DEPLOY_HOST="example.invalid", DEPLOY_PORT="1313",
                           DEPLOY_HOST_FINGERPRINT=fingerprint or self.fingerprint,
                           ssh_dir=str(self.directory))
        result = subprocess.run(["bash", "-c", self.verification], env=environment,
                                text=True, capture_output=True, timeout=10)
        return result, self.directory / "known_hosts"

    def test_comments_and_blank_lines_before_and_after_key(self):
        result, known_hosts = self.verify("# example.invalid:1313 SSH-2.0-OpenSSH\n\n  # another banner\n  \n"
                                         + self.record + "\n# trailing banner\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(known_hosts.read_text(), self.record + "\n")

    def test_legacy_scan_without_comments(self):
        result, known_hosts = self.verify(self.record + "\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(known_hosts.read_text(), self.record + "\n")

    def test_fingerprint_mismatch_is_rejected(self):
        result, known_hosts = self.verify(self.record + "\n", fingerprint="SHA256:untrusted")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(known_hosts.exists())
        self.assertIn("Server fingerprint does not match", result.stdout)

    def test_only_matching_key_is_trusted(self):
        other = self.directory / "other-key"
        subprocess.run(["ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", str(other)], check=True)
        other_record = "[example.invalid]:1313 " + other.with_suffix(".pub").read_text().strip()
        result, known_hosts = self.verify(other_record + "\n" + self.record + "\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(known_hosts.read_text(), self.record + "\n")

    def test_empty_or_comment_only_scan_is_rejected(self):
        for scan in ("", "\n# banner\n  # banner\n"):
            with self.subTest(scan=scan):
                result, known_hosts = self.verify(scan)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(known_hosts.exists())

    def test_malformed_key_is_rejected(self):
        result, known_hosts = self.verify("[example.invalid]:1313 ssh-ed25519 invalid\n" + self.record + "\n")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(known_hosts.exists())

    def test_failed_scan_is_rejected_even_with_matching_output(self):
        result, known_hosts = self.verify(self.record + "\n", scan_status=1)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(known_hosts.exists())


if __name__ == "__main__":
    unittest.main()
