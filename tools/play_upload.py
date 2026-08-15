#!/usr/bin/env python3
"""Upload an app bundle to Google Play via the Developer API.

Deliberately uploads as a **draft** by default: the release is staged in Play
Console with its notes attached, and nothing reaches users until someone
presses publish. Pass --rollout to actually release.

    pip install google-auth requests

    python3 tools/play_upload.py \
        --key /path/to/service-account.json \
        --aab build/store-package-6.0.0/weeklybudget-6.0.0-signed.aab \
        --notes build/store-package-6.0.0/release-notes.txt

Add --dry-run first: it authenticates and prints what Play currently has on
each track without creating an edit.
"""

import argparse
import json
import sys

try:
    import requests
    from google.oauth2 import service_account
    from google.auth.transport.requests import Request as AuthRequest
except ImportError:
    sys.exit("Missing dependencies. Run: pip install google-auth requests")

PACKAGE = "com.andrewovens.weeklybudget2"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
UPLOAD = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications"


def session_for(key_path):
    creds = service_account.Credentials.from_service_account_file(
        key_path, scopes=[SCOPE])
    creds.refresh(AuthRequest())
    s = requests.Session()
    s.headers["Authorization"] = "Bearer " + creds.token
    return s, creds.service_account_email


def check(response, what):
    if response.status_code >= 400:
        sys.exit("{} failed: HTTP {}\n{}".format(
            what, response.status_code, response.text[:900]))
    return response.json() if response.content else {}


def explain(response, who, package):
    """Report what the API actually said, then add the fix for the two setup
    steps that are easy to miss. Guessing at the cause instead of reading the
    response sends you to the wrong console."""
    try:
        err = response.json().get("error", {})
        reason = next((d.get("reason") for d in err.get("details", [])
                       if d.get("@type", "").endswith("ErrorInfo")), None)
        message = err.get("message", "")
    except ValueError:
        reason, message = None, response.text[:400]

    print("HTTP {}: {}".format(response.status_code, message), file=sys.stderr)

    if reason == "SERVICE_DISABLED":
        sys.exit("\nFix: enable the Google Play Android Developer API on the Cloud\n"
                 "project that owns this key, then wait a minute for it to "
                 "propagate.")
    if response.status_code in (401, 403):
        sys.exit("\nFix: in Play Console -> Users and permissions -> Invite new user,\n"
                 "add {}\nand grant it release access to {}.".format(who, package))
    if response.status_code == 404:
        sys.exit("\nNo app '{}' is visible to this account.".format(package))
    sys.exit(1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--key", required=True, help="service account JSON")
    ap.add_argument("--aab", required=True)
    ap.add_argument("--notes", help="release notes file (<=500 chars)")
    ap.add_argument("--track", default="production")
    ap.add_argument("--package", default=PACKAGE)
    ap.add_argument("--rollout", type=float, metavar="FRACTION",
                    help="release to this fraction of users, e.g. 0.1. "
                         "Omit to stage the release as a draft.")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    s, who = session_for(args.key)
    print("authenticated as {}".format(who))

    if args.dry_run:
        # An edit is a scratch space: nothing changes unless it is committed,
        # and this one is deleted before returning.
        r = s.post("{}/{}/edits".format(BASE, args.package))
        if r.status_code >= 400:
            explain(r, who, args.package)
        edit = check(r, "edits.insert")
        tracks = check(s.get("{}/{}/edits/{}/tracks".format(BASE, args.package, edit["id"])),
                       "tracks.list")
        for t in tracks.get("tracks", []):
            for rel in t.get("releases", []):
                print("  {:<12} {:<10} versionCodes={}".format(
                    t["track"], rel.get("status"), rel.get("versionCodes")))
        s.delete("{}/{}/edits/{}".format(BASE, args.package, edit["id"]))
        print("dry run only — edit discarded, nothing changed")
        return

    notes = None
    if args.notes:
        notes = open(args.notes, encoding="utf-8").read().strip()
        if len(notes) > 500:
            sys.exit("release notes are {} chars; Play allows 500".format(len(notes)))

    r = s.post("{}/{}/edits".format(BASE, args.package))
    if r.status_code >= 400:
        explain(r, who, args.package)
    edit_id = check(r, "edits.insert")["id"]
    print("edit {}".format(edit_id))

    with open(args.aab, "rb") as f:
        r = s.post("{}/{}/edits/{}/bundles?uploadType=media".format(
                       UPLOAD, args.package, edit_id),
                   headers={"Content-Type": "application/octet-stream"},
                   data=f)
    if r.status_code >= 400:
        explain(r, who, args.package)
    bundle = check(r, "bundles.upload")
    version_code = bundle["versionCode"]
    print("uploaded versionCode {}".format(version_code))

    release = {"versionCodes": [str(version_code)]}
    if args.rollout is None:
        release["status"] = "draft"
    elif args.rollout >= 1:
        release["status"] = "completed"
    else:
        release["status"] = "inProgress"
        release["userFraction"] = args.rollout
    if notes:
        release["releaseNotes"] = [{"language": "en-US", "text": notes}]

    check(s.put("{}/{}/edits/{}/tracks/{}".format(BASE, args.package, edit_id, args.track),
                json={"track": args.track, "releases": [release]}),
          "tracks.update")
    print("track '{}' set to {}".format(args.track, release["status"]))

    check(s.post("{}/{}/edits/{}:commit".format(BASE, args.package, edit_id)),
          "edits.commit")

    if release["status"] == "draft":
        print("\nDone. Staged as a DRAFT — nothing is live. Open Play Console,\n"
              "review, and press publish when you are ready.")
    else:
        print("\nDone. Released to '{}' as {}.".format(args.track, release["status"]))


if __name__ == "__main__":
    main()
