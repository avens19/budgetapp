#!/usr/bin/env python3
"""Check the Play listing files against Play's limits, before an upload does.

    python3 tools/store_check.py

Play rejects an over-long field rather than truncating it, and it rejects the
whole edit — so one long German sentence fails the upload for all twenty
languages at once, after the bundle has already gone up. Cheaper to find here.

It also checks that every language has every field, that the set of languages
matches the app's own twenty, and that a translation has not lost the URL, which
is the one piece of the description that has to survive intact.
"""

import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LISTINGS = os.path.join(ROOT, 'store', 'listing')
NOTES = os.path.join(ROOT, 'store', 'release-notes')

# Play's limits. Not negotiable and not warned about: the API returns 400.
LIMITS = {'title': 30, 'shortDescription': 80, 'fullDescription': 4000}
NOTES_LIMIT = 500

# Play's language codes, against the app's resource qualifiers. They agree less
# often than you would hope: Play has no bare `es`, wants `es-419` for Latin
# America, and uses `id` where Android resources use the legacy `in`.
LANGUAGES = {
    'en-US': 'values',
    'es-ES': 'values-es',
    'es-419': 'values-es-rUS',
    'pt-BR': 'values-pt-rBR',
    'pt-PT': 'values-pt-rPT',
    'fr-FR': 'values-fr',
    'de-DE': 'values-de',
    'it-IT': 'values-it',
    'nl-NL': 'values-nl',
    'ru-RU': 'values-ru',
    'uk': 'values-uk',
    'pl-PL': 'values-pl',
    'tr-TR': 'values-tr',
    'ar': 'values-ar',
    'hi-IN': 'values-hi',
    'id': 'values-in',
    'ja-JP': 'values-ja',
    'ko-KR': 'values-ko',
    'zh-CN': 'values-zh-rCN',
    'zh-TW': 'values-zh-rTW',
    'vi': 'values-vi',
}

URL = 'https://budget.andrewovens.com'


def main():
    problems = []

    found = {os.path.basename(p)[:-5] for p in glob.glob(os.path.join(LISTINGS, '*.json'))}
    missing = sorted(set(LANGUAGES) - found)
    extra = sorted(found - set(LANGUAGES))
    problems += ['no listing for %s' % lang for lang in missing]
    problems += ['%s is not a language this app ships' % lang for lang in extra]

    for language in sorted(found & set(LANGUAGES)):
        with open(os.path.join(LISTINGS, '%s.json' % language), encoding='utf-8') as handle:
            listing = json.load(handle)

        for field, limit in LIMITS.items():
            value = listing.get(field)
            if not value:
                problems.append('%s: %s is empty' % (language, field))
                continue
            if len(value) > limit:
                problems.append('%s: %s is %d characters, limit is %d'
                                % (language, field, len(value), limit))

        # The one piece of the description that is not prose. A translation that
        # drops it leaves a sentence pointing at nothing.
        if URL not in listing.get('fullDescription', ''):
            problems.append('%s: the full description has lost %s' % (language, URL))

        # Play shows the store name; the app's own name is a separate string.
        # Both should read the same, and neither should have been translated
        # into something the user cannot search for.
        if listing.get('title') != 'Weekly Budget':
            problems.append('%s: title is %r, expected the untranslated brand'
                            % (language, listing.get('title')))

    for path in sorted(glob.glob(os.path.join(NOTES, '*.txt'))):
        language = os.path.basename(path)[:-4]
        if language not in LANGUAGES:
            problems.append('release notes for %s, which is not a shipped language' % language)
        with open(path, encoding='utf-8') as handle:
            text = handle.read().strip()
        if len(text) > NOTES_LIMIT:
            problems.append('%s release notes are %d characters, limit is %d'
                            % (language, len(text), NOTES_LIMIT))

    if problems:
        print('\n'.join(problems))
        print('\n%d problem(s)' % len(problems))
        return 1

    print('%d languages, every field present and inside Play\'s limits' % len(found))
    longest = max(
        (len(json.load(open(os.path.join(LISTINGS, '%s.json' % l), encoding='utf-8'))['fullDescription']), l)
        for l in sorted(found))
    print('longest description: %s at %d characters (limit %d)'
          % (longest[1], longest[0], LIMITS['fullDescription']))
    return 0


if __name__ == '__main__':
    sys.exit(main())
