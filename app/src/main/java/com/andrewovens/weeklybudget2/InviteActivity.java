package com.andrewovens.weeklybudget2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

/**
 * Where an invite link lands: {@code https://budget.andrewovens.com/join/<token>}.
 *
 * <p>A screen of its own rather than another branch inside {@link WeekActivity},
 * which owns the back stack and is complicated enough. This one redeems, hands
 * the budget to {@link Settings}, and gets out of the way.
 *
 * <p>Redeeming is a POST, which matters more than it looks: the same URL is
 * fetched by every messaging client that renders a preview, and if a fetch spent
 * the invitation it would be dead before its recipient tapped it. The server has
 * no GET that redeems, and neither does this.
 */
public class InviteActivity extends BaseActivity {

    /**
     * Survives the rotation that would otherwise redeem a second time.
     *
     * <p>The first redemption spends the invitation, so a re-run on recreate
     * would report a perfectly successful join as an expired link.
     */
    private static final String STARTED = "started";

    private boolean _started;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _started = savedInstanceState.getBoolean(STARTED, false);
        }

        String token = tokenFrom(getIntent().getData());
        if (token == null) {
            // Not a link this app understands. Nothing to explain to the user.
            openApp();
            return;
        }

        if (!_started) {
            _started = true;
            redeem(token);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STARTED, _started);
    }

    /**
     * The token out of a {@code /join/<token>} path, or null.
     *
     * <p>Narrow on purpose: the value goes straight back out as part of a request
     * path, so anything that could change the shape of that path is refused here
     * rather than sent to the server to argue about.
     */
    @Nullable
    static String tokenFrom(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !segments.get(0).equalsIgnoreCase("join")) {
            return null;
        }

        String token = segments.get(1);
        if (token.isEmpty() || token.length() > 64) {
            return null;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean base64url = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!base64url) {
                return null;
            }
        }
        return token;
    }

    private void redeem(String token) {
        setContentView(R.layout.activity_invite);

        new Thread(() -> {
            try {
                Budget budget = API.RedeemInvite(token);
                if (budget == null) {
                    runOnUiThread(() -> explain(R.string.invite_unusable_title,
                                                R.string.invite_unusable_body));
                    return;
                }

                Settings.rememberBudget(InviteActivity.this, budget);
                runOnUiThread(this::openApp);
            } catch (Exception e) {
                // A network failure is worth retrying, so it says so rather than
                // claiming the invitation was bad.
                runOnUiThread(() -> explain(R.string.invite_failed_title,
                                            R.string.invite_failed_body));
                e.printStackTrace();
            }
        }).start();
    }

    private void explain(int title, int body) {
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (d, which) -> openApp())
                .show();
    }

    /**
     * Into the app proper, clearing this screen out of the stack so Back from the
     * week view leaves the app instead of returning to a spent invitation.
     */
    private void openApp() {
        Intent intent = new Intent(this, WeekActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
