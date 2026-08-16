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
import os
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
    if reason in ("IAM_PERMISSION_DENIED", "ACCESS_TOKEN_SCOPE_INSUFFICIENT") or (
            response.status_code == 401) or "permission" in message.lower():
        sys.exit("\nFix: in Play Console -> Users and permissions -> Invite new user,\n"
                 "add {}\nand grant it release access to {}.".format(who, package))
    if "already been used" in message:
        sys.exit("\nThat version code is already uploaded — to move the existing\n"
                 "release between statuses or tracks, pass --promote <versionCode>\n"
                 "instead of --aab.")
    if response.status_code == 404:
        sys.exit("\nNo app '{}' is visible to this account.".format(package))
    sys.exit(1)


IMAGE_TYPE = "phoneScreenshots"


def upload_screenshots(s, args, edit_id, who):
    """Replace the whole phone set. Play keeps images in upload order and has
    no way to reorder them afterwards, so the existing ones are cleared first
    and the new files sent in filename order."""
    import glob
    files = sorted(glob.glob(os.path.join(args.screenshots, "*.png")))
    if not files:
        sys.exit("no PNGs in {}".format(args.screenshots))

    listing = "{}/{}/edits/{}/listings/{}/{}".format(
        BASE, args.package, edit_id, args.language, IMAGE_TYPE)
    r = s.delete(listing)
    if r.status_code >= 400:
        explain(r, who, args.package)
    print("cleared existing {}".format(IMAGE_TYPE))

    for path in files:
        with open(path, "rb") as f:
            r = s.post("{}/{}/edits/{}/listings/{}/{}?uploadType=media".format(
                           UPLOAD, args.package, edit_id, args.language, IMAGE_TYPE),
                       headers={"Content-Type": "image/png"}, data=f)
        if r.status_code >= 400:
            explain(r, who, args.package)
        print("  uploaded {}".format(os.path.basename(path)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--key", required=True, help="service account JSON")
    ap.add_argument("--aab", help="bundle to upload; omit when using --promote")
    ap.add_argument("--promote", type=int, metavar="VERSIONCODE",
                    help="reuse a version code already uploaded to Play rather "
                         "than uploading again")
    ap.add_argument("--notes", help="release notes file (<=500 chars)")
    ap.add_argument("--track", default="production")
    ap.add_argument("--package", default=PACKAGE)
    ap.add_argument("--rollout", type=float, metavar="FRACTION",
                    help="release to this fraction of users, e.g. 0.1. "
                         "Omit to stage the release as a draft.")
    ap.add_argument("--screenshots", metavar="DIR",
                    help="replace the phone screenshots with the PNGs in DIR, "
                         "in filename order")
    ap.add_argument("--language", default="en-US")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    if not (args.aab or args.promote or args.screenshots):
        ap.error("one of --aab, --promote or --screenshots is required")

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

    # Screenshots on their own: no track change, just the listing images.
    if args.screenshots and not (args.aab or args.promote):
        r = s.post("{}/{}/edits".format(BASE, args.package))
        if r.status_code >= 400:
            explain(r, who, args.package)
        edit_id = check(r, "edits.insert")["id"]
        upload_screenshots(s, args, edit_id, who)
        check(s.post("{}/{}/edits/{}:commit".format(BASE, args.package, edit_id)),
              "edits.commit")
        print("\nDone. Listing screenshots replaced.")
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

    if args.promote:
        version_code = args.promote
        print("promoting existing versionCode {}".format(version_code))
    else:
        with open(args.aab, "rb") as f:
            r = s.post("{}/{}/edits/{}/bundles?uploadType=media".format(
                           UPLOAD, args.package, edit_id),
                       headers={"Content-Type": "application/octet-stream"},
                       data=f)
        if r.status_code >= 400:
            explain(r, who, args.package)
        version_code = check(r, "bundles.upload")["versionCode"]
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

    if args.screenshots:
        upload_screenshots(s, args, edit_id, who)

    check(s.post("{}/{}/edits/{}:commit".format(BASE, args.package, edit_id)),
          "edits.commit")

    if release["status"] == "draft":
        print("\nDone. Staged as a DRAFT — nothing is live. Open Play Console,\n"
              "review, and press publish when you are ready.")
    else:
        print("\nDone. Released to '{}' as {}.".format(args.track, release["status"]))


if __name__ == "__main__":
    main()
