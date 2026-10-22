package com.andrewovens.weeklybudget2;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Who wrote this, how to reach him, and where the rest of it is.
 *
 * <p>There are no accounts and no support desk, so a user with a problem has
 * nowhere to go: the Play listing's reply box is one-way and slow, and the
 * privacy policy's address is buried on the web app. This screen is the one
 * place in the app that answers "who is behind this and how do I ask them
 * something", and it carries the build number so a bug report says which
 * version it came from without anyone having to ask.
 *
 * <p>There is no tip jar here on purpose. Google Play's payments policy treats
 * an in-app link that asks for money outside Play Billing as a violation, so
 * the ask lives on the web page behind "Learn more", the same way the iOS app
 * handles it. The link out stays a plain "there is more about this on the web".
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

        findViewById(R.id.button_learn_more)
                .setOnClickListener(v -> Helpers.openUrl(this, getString(R.string.url_about)));

        findViewById(R.id.button_privacy)
                .setOnClickListener(v -> Helpers.openUrl(this, getString(R.string.url_privacy)));
    }
}
