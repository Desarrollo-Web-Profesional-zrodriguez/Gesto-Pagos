#!/usr/bin/env python3
"""
Gesto Pago E2E Test Suite Orchestrator & CLI Runner.
Standard Python 3.12 standard library only (unittest, argparse, urllib).
Executes across 4 tiers:
  Tier 1: Feature Coverage (65 tests, F01-F13)
  Tier 2: Boundary & Corner Cases (65 tests, B01-B13)
  Tier 3: Pairwise Combinations (14 tests, C01-C14)
  Tier 4: Real-World Scenarios (8 scenarios, S01-S08)
Total: 152 test cases.
"""
import argparse
import io
import os
import re
import sys
import time
import unittest
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

# Ensure project root is in sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from tests.e2e.config import BACKEND_URL, SONAR_URL
from tests.e2e.client import is_backend_online, is_sonar_online

TIER_DIRS = {
    "1": SCRIPT_DIR / "tier1_features",
    "2": SCRIPT_DIR / "tier2_boundaries",
    "3": SCRIPT_DIR / "tier3_combinations",
    "4": SCRIPT_DIR / "tier4_scenarios",
}

TIER_NAMES = {
    "1": "Tier 1: Feature Coverage",
    "2": "Tier 2: Boundary & Corner Cases",
    "3": "Tier 3: Pairwise Combinations",
    "4": "Tier 4: Real-World Scenarios",
}


def extract_test_cases(suite: unittest.TestSuite) -> List[unittest.TestCase]:
    """Recursively extracts all TestCase instances from a TestSuite."""
    cases = []
    for item in suite:
        if isinstance(item, unittest.TestSuite):
            cases.extend(extract_test_cases(item))
        elif isinstance(item, unittest.TestCase):
            cases.append(item)
    return cases


def get_feature_id(case: unittest.TestCase) -> str:
    """Extracts feature code (F01..F13) from test method name, class name, or docstring."""
    name = case._testMethodName
    # Match T1_F01_01, T2_F03_02, F01, B01, etc.
    m = re.search(r"[TB]?(?:F|B|0)?(0[1-9]|1[0-3])", name)
    if m:
        return f"F{m.group(1)}"
    # Check docstring
    doc = getattr(case, case._testMethodName).__doc__ or ""
    m_doc = re.search(r"F(0[1-9]|1[0-3])", doc)
    if m_doc:
        return f"F{m_doc.group(1)}"
    return "Cross"


def discover_suite(
    selected_tier: Optional[str] = None,
    selected_feature: Optional[str] = None,
) -> Tuple[unittest.TestSuite, Dict[str, List[unittest.TestCase]]]:
    """Discovers tests matching tier and feature filters."""
    loader = unittest.TestLoader()
    combined_suite = unittest.TestSuite()
    tier_cases: Dict[str, List[unittest.TestCase]] = {"1": [], "2": [], "3": [], "4": []}

    tiers_to_scan = [selected_tier] if selected_tier in TIER_DIRS else ["1", "2", "3", "4"]

    for tier_key in tiers_to_scan:
        tier_dir = TIER_DIRS[tier_key]
        if not tier_dir.exists():
            continue
        discovered = loader.discover(start_dir=str(tier_dir), pattern="test_*.py")
        extracted = extract_test_cases(discovered)

        for case in extracted:
            if selected_feature:
                feat_norm = selected_feature.upper()
                if not feat_norm.startswith("F") and feat_norm.isdigit():
                    feat_norm = f"F{int(feat_norm):02d}"
                case_feat = get_feature_id(case)
                if feat_norm not in case._testMethodName.upper() and feat_norm != case_feat:
                    continue

            tier_cases[tier_key].append(case)
            combined_suite.addTest(case)

    return combined_suite, tier_cases


def format_table_row(cols: List[str], widths: List[int]) -> str:
    """Formats a single row of a clean ASCII table."""
    formatted = [f" {col:<{w-2}} " for col, w in zip(cols, widths)]
    return "|" + "|".join(formatted) + "|"


def print_summary_table(
    tier_stats: Dict[str, Dict[str, int]],
    total_stats: Dict[str, int],
    elapsed: float,
):
    """Prints a structured summary table of execution results."""
    headers = ["Tier / Category", "Total", "Passed", "Skipped", "Failed", "Errors"]
    widths = [36, 9, 10, 11, 10, 10]
    separator = "+" + "+".join(["-" * w for w in widths]) + "+"

    print("\n" + separator)
    print(format_table_row(headers, widths))
    print(separator)

    for tier_key in ["1", "2", "3", "4"]:
        stats = tier_stats.get(tier_key, {"total": 0, "passed": 0, "skipped": 0, "failed": 0, "errors": 0})
        cols = [
            TIER_NAMES[tier_key],
            str(stats["total"]),
            str(stats["passed"]),
            str(stats["skipped"]),
            str(stats["failed"]),
            str(stats["errors"]),
        ]
        print(format_table_row(cols, widths))

    print(separator)
    tot_cols = [
        "TOTAL (All Active Suites)",
        str(total_stats["total"]),
        str(total_stats["passed"]),
        str(total_stats["skipped"]),
        str(total_stats["failed"]),
        str(total_stats["errors"]),
    ]
    print(format_table_row(tot_cols, widths))
    print(separator)
    print(f"Elapsed Time: {elapsed:.2f} seconds | Backend: {BACKEND_URL} | SonarQube: {SONAR_URL}\n")


def main():
    parser = argparse.ArgumentParser(
        description="Gesto Pago 4-Tier Comprehensive E2E Test Suite Runner"
    )
    parser.add_argument(
        "--tier",
        choices=["1", "2", "3", "4"],
        help="Run only the specified tier (1, 2, 3, or 4)",
    )
    parser.add_argument(
        "--feature",
        help="Filter tests by feature code (e.g., F01, F04, F13)",
    )
    parser.add_argument(
        "--dry-run",
        "--discover",
        action="store_true",
        dest="dry_run",
        help="Discover and enumerate test catalog without executing tests",
    )
    parser.add_argument(
        "-v",
        "--verbose",
        action="store_true",
        help="Enable detailed verbose output for each test case",
    )
    args = parser.parse_args()

    print("=" * 80)
    print(" GESTO PAGO - COMPREHENSIVE 4-TIER E2E TEST SUITE")
    print(" Standard Library Runner (Python 3.12 unittest)")
    print(f" Environment: Backend={BACKEND_URL} (online={is_backend_online(BACKEND_URL)})")
    print(f" SonarQube:  {SONAR_URL} (online={is_sonar_online(SONAR_URL)})")
    print("=" * 80)

    suite, tier_cases = discover_suite(
        selected_tier=args.tier,
        selected_feature=args.feature,
    )

    all_cases = extract_test_cases(suite)
    total_discovered = len(all_cases)

    if args.dry_run:
        print(f"\n[DRY RUN / DISCOVERY MODE] Discovered {total_discovered} test cases:\n")
        for tier_key in ["1", "2", "3", "4"]:
            cases = tier_cases.get(tier_key, [])
            if not cases:
                continue
            print(f"--- {TIER_NAMES[tier_key]} ({len(cases)} tests) ---")
            for c in cases:
                doc = (getattr(c, c._testMethodName).__doc__ or "").strip().split("\n")[0]
                print(f"  [{c._testMethodName}] - {doc}")
            print()
        print(f"Total discovered tests: {total_discovered}")
        sys.exit(0)

    # Execute tests
    start_time = time.time()
    runner = unittest.TextTestRunner(
        verbosity=2 if args.verbose else 1,
        stream=sys.stdout,
    )
    result = runner.run(suite)
    elapsed = time.time() - start_time

    # Categorize results per tier
    failed_cases = {test for test, _ in result.failures}
    error_cases = {test for test, _ in result.errors}
    skipped_cases = {test for test, _ in result.skipped}

    tier_stats = {}
    for tier_key, cases in tier_cases.items():
        t_total = len(cases)
        t_failed = sum(1 for c in cases if c in failed_cases)
        t_errors = sum(1 for c in cases if c in error_cases)
        t_skipped = sum(1 for c in cases if c in skipped_cases)
        t_passed = t_total - (t_failed + t_errors + t_skipped)
        tier_stats[tier_key] = {
            "total": t_total,
            "passed": t_passed,
            "skipped": t_skipped,
            "failed": t_failed,
            "errors": t_errors,
        }

    total_stats = {
        "total": total_discovered,
        "passed": total_discovered - (len(result.failures) + len(result.errors) + len(result.skipped)),
        "skipped": len(result.skipped),
        "failed": len(result.failures),
        "errors": len(result.errors),
    }

    print_summary_table(tier_stats, total_stats, elapsed)

    if result.wasSuccessful():
        print(">> ALL TEST SUITES PASSED SUCCESSFULLY (Exit Code 0)")
        sys.exit(0)
    else:
        print(f">> FAILURES DETECTED: {len(result.failures)} failures, {len(result.errors)} errors (Exit Code 1)")
        sys.exit(1)


if __name__ == "__main__":
    main()
