"""Exercise validation cleanup using shell mocks; never contact AWS."""

from pathlib import Path
import os
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parent / "validate-service.sh"
MOCKS = r'''
aws() {
  printf '%s\n' "$*" >> "$CALL_LOG"
  case "$*" in
    *'--desired-count 0'*) [[ "$SCENARIO" != cleanup_failure ]];;
    *'--desired-count 1'*) [[ "$SCENARIO" != start_failure ]];;
    *'services[0].desiredCount'*)
      if [[ "$SCENARIO" == already_active ]]; then echo 1; else echo 0; fi;;
    *'describe-services'*)
      if [[ "$SCENARIO" == api_failure ]]; then return 1; fi
      if [[ "$SCENARIO" == timeout || "$SCENARIO" == interrupted ]]; then echo False; else echo True; fi;;
    *'list-tasks'*) echo task-test;;
    *'describe-tasks'*) echo True;;
    *) return 1;;
  esac
}
sleep() {
  if [[ "$SCENARIO" == interrupted ]]; then kill -TERM $$; else SECONDS=$((SECONDS + 601)); fi
}
'''


class ValidateServiceTest(unittest.TestCase):
    def run_scenario(self, scenario):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            mocks = root / "mocks.sh"
            log = root / "calls.txt"
            mocks.write_text(MOCKS)
            result = subprocess.run(
                ["bash", str(SCRIPT)],
                env={**os.environ, "BASH_ENV": str(mocks), "CALL_LOG": str(log), "SCENARIO": scenario},
                capture_output=True, text=True, timeout=5,
            )
            return result, log.read_text()

    def test_restores_zero_for_all_validation_exit_paths(self):
        for scenario in ("success", "timeout", "start_failure", "api_failure", "interrupted"):
            with self.subTest(scenario=scenario):
                result, calls = self.run_scenario(scenario)
                self.assertIn("--desired-count 1", calls)
                self.assertIn("--desired-count 0", calls.splitlines()[-1])
                self.assertEqual(result.returncode == 0, scenario == "success")

    def test_reports_cleanup_failure(self):
        result, calls = self.run_scenario("cleanup_failure")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Shutdown failed", result.stderr)
        self.assertIn("--desired-count 0", calls)

    def test_refuses_an_already_active_service(self):
        result, calls = self.run_scenario("already_active")
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("update-service", calls)


if __name__ == "__main__":
    unittest.main()
