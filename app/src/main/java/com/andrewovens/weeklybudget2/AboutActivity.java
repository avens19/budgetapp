package com.andrewovens.weeklybudget2;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Who wrote this, how to reach him, and the two ways to say thanks.
 *
 * <p>There are no accounts and no support desk, so a user with a problem has
 * nowhere to go: the Play listing's reply box is one-way and slow, and the
 * privacy policy's address is buried on the web app. This screen is the one
 * place in the app that answers "who is behind this and how do I ask them
 * something", and it carries the build number so a bug report says which
 * version it came from without anyone having to ask.
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.title_about);

        ((TextView) findViewById(R.id.about_version))
                .setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));

        findViewById(R.id.button_email).setOnClickListener(v -> Helpers.sendEmail(this,
                getString(R.string.email_support),
                getString(R.string.about_email_subject, BuildConfig.VERSION_NAME)));

        findViewById(R.id.button_review).setOnClickListener(v -> Helpers.openPlayListing(this));

        findViewById(R.id.button_coffee)
                .setOnClickListener(v -> Helpers.openUrl(this, getString(R.string.url_coffee)));

        findViewById(R.id.button_privacy)
                .setOnClickListener(v -> Helpers.openUrl(this, getString(R.string.url_privacy)));
    }
}
