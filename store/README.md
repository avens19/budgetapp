# Play Store listing

The text Play shows, kept here so it is version-controlled and reviewable rather
than living only in a web form. `tools/play_upload.py --listings store/listing`
pushes it.

    listing/<language>.json     title, short and full description
    release-notes/<language>.txt   the notes for the release being uploaded

Language codes are Play's, which are not always the ones the app uses for its
own resources: Play wants `es-419` for Latin American Spanish, `pt-PT` and
`pt-BR` separately, `id` for Indonesian and `zh-CN` / `zh-TW` for Chinese. The
mapping to the app's twenty locales is in `tools/store_check.py`, which also
enforces the limits — title 30 characters, short description 80, full
description 4000, release notes 500. Play rejects an over-long field rather than
truncating it, and it rejects the whole edit, so a single long German sentence
fails the upload for every language at once.

## The English is the source

Everything else is a translation of `listing/en-US.json`, and the same wording
as the app itself where they overlap — the tutorial's explanation of what the app
deliberately does not do is the same explanation in the store copy, because
someone reading one and then the other should not be told two different things.

The listing that was live until August 2026 ended with "Web application available
(for iOS/Windows Phone)". Windows Phone has not existed for years and there is now
an iPhone app, so that line is replaced rather than translated twenty times.
