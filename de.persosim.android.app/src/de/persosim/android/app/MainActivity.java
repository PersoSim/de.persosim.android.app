package de.persosim.android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.database.Cursor;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import de.persosim.android.hce.CardService;
import de.persosim.android.simulator.PersoSimService;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(28, 40, 61);
    private static final int BLUE = Color.rgb(37, 83, 177);
    private final Handler handler = new Handler();
    private final List<RadioButton> selections = new ArrayList<>();
    private final List<Button> profileActions = new ArrayList<>();
    private static final int IMPORT_FILE = 42;
    private String pending;
    private String pendingMessage;
    private List<ProfileCatalog> profiles = new ArrayList<>();
    private TextView status;
    private TextView contact;
    private TextView active;
    private String checkDetails = "";
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (pending != null && pending.equals(PersoSimService.completedSave)) {
                String failure = PersoSimService.saveError;
                pending = null;
                reloadProfiles();
                if (failure.isEmpty()) Toast.makeText(MainActivity.this, pendingMessage, Toast.LENGTH_SHORT).show();
                else showText("Vorgang nicht abgeschlossen", failure);
            }
            updateStatus();
            handler.postDelayed(this, 750);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Diagnostics.initialize();
        if (state != null && PersoSimService.processId.equals(state.getString("processId"))) {
            pending = state.getString("pending");
            pendingMessage = state.getString("pendingMessage");
        }
        if (state == null) CardService.setEnabled(this, true);
        startService(new Intent(this, PersoSimService.class));
        if (getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).getBoolean("remoteIfdEnabled", true)) {
            startService(new Intent().setClassName(this, "de.persosim.android.remoteifd.RemoteIfdService"));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        reloadProfiles();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    private void reloadProfiles() {
        try { profiles = ProfileCatalog.read(this); }
        catch (Exception exception) { Diagnostics.failure("Profilliste konnte nicht gelesen werden", exception); }
        buildScreen();
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private void buildScreen() {
        selections.clear();
        profileActions.clear();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243, 246, 251));
        LinearLayout page = column();
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(page);
        TextView title = text("PersoSim", 30, true);
        page.addView(title);
        page.addView(text("Testausweis per NFC", 15, false));
        Button settings = button("Einstellungen", "PersoSim-Einstellungen öffnen");
        settings.setOnClickListener(view -> startActivity(new Intent().setClassName(this, "de.persosim.android.app.SettingsActivity")));
        page.addView(settings, spaced());

        LinearLayout statusCard = card();
        status = text("Simulator startet …", 19, true);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusCard.addView(status);
        active = text("Profil wird geladen", 14, false);
        statusCard.addView(active);
        contact = text("Noch kein NFC-Kontakt", 13, false);
        statusCard.addView(contact);
        Button checks = button("NFC prüfen", "NFC-Status und Einstellungen öffnen");
        checks.setOnClickListener(view -> showChecks());
        statusCard.addView(checks);
        page.addView(statusCard, spaced());

        page.addView(text("Profile", 22, true), spaced());
        page.addView(text("Antippen zum Aktivieren. Das letzte Profil wird beim Start geladen.", 14, false));
        Button create = button("Neuer Ausweis", "Eigenes Profil aus der Standardvorlage erstellen");
        create.setOnClickListener(view -> startActivity(new Intent(this, ProfileEditorActivity.class)));
        page.addView(create);
        profileActions.add(create);
        Button importFile = button("PersoSim importieren", "PersoSim-Datei auswählen und importieren");
        importFile.setOnClickListener(view -> {
            if (busy()) return;
            try {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), IMPORT_FILE);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, "Keine Dateiauswahl verfügbar.", Toast.LENGTH_LONG).show();
            }
        });
        page.addView(importFile);
        profileActions.add(importFile);
        for (final ProfileCatalog profile : profiles) {
            LinearLayout row = card();
            LinearLayout heading = new LinearLayout(this);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            RadioButton selection = new RadioButton(this);
            selection.setText(profile.title);
            selection.setTextSize(17);
            selection.setTextColor(INK);
            selection.setMinHeight(dp(52));
            selection.setContentDescription(profile.title + " aktivieren");
            selection.setOnClickListener(view -> {
                if (!profile.file.equals(PersoSimService.activeFile)) {
                    if (busy()) return;
                    startService(new Intent(this, PersoSimService.class).putExtra("profile", profile.file));
                }
                updateStatus();
            });
            selections.add(selection);
            heading.addView(selection, new LinearLayout.LayoutParams(0, -2, 1));
            Button details = button("Details", "Details zu " + profile.title);
            details.setOnClickListener(view -> showDetails(profile));
            heading.addView(details, new LinearLayout.LayoutParams(dp(88), dp(52)));
            row.addView(heading);
            TextView subtitle = text(profile.subtitle, 13, false);
            subtitle.setPadding(dp(8), dp(2), dp(8), dp(3));
            row.addView(subtitle);
            if (ProfileStore.isCustom(profile.file)) {
                Button edit = button("Bearbeiten", profile.title + " bearbeiten");
                edit.setOnClickListener(view -> startActivity(new Intent(this, ProfileEditorActivity.class).putExtra("profile", profile.file)));
                row.addView(edit);
                profileActions.add(edit);
            }
            if (ProfileStore.isOwned(profile.file)) {
                Button delete = button("Löschen", profile.title + " löschen");
                delete.setOnClickListener(view -> {
                    if (busy()) return;
                    new AlertDialog.Builder(this).setTitle("Ausweis löschen?")
                            .setMessage("„" + profile.title + "“ wird gelöscht. Ist dieser Ausweis aktiv, wird Originalprofil 01 aktiviert.")
                            .setNegativeButton("Abbrechen", null)
                            .setPositiveButton("Löschen", (dialog, which) -> submit(
                                    new Intent(this, PersoSimService.class).putExtra("profile", profile.file).putExtra("delete", true),
                                    "Ausweis gelöscht")).show();
                });
                row.addView(delete);
                profileActions.add(delete);
            }
            page.addView(row, spaced());
        }
        page.addView(text("Das Display bleibt an, solange diese App sichtbar ist.", 13, false), spaced());
        Button logs = button("Protokoll", "Diagnoseprotokoll anzeigen");
        logs.setOnClickListener(view -> showText("Protokoll", Diagnostics.text()));
        page.addView(logs, spaced());
        Button about = button("Über PersoSim", "Version und Projektinformationen anzeigen");
        about.setOnClickListener(view -> {
            try {
                String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                showText("Über PersoSim", "PersoSim\nVersion " + version + "\n\nhttps://persosim.github.io/");
            } catch (PackageManager.NameNotFoundException exception) {
                Diagnostics.failure("Versionsinformation nicht verfügbar", exception);
            }
        });
        page.addView(about);
        setContentView(scroll);
    }

    private void updateStatus() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        boolean enabled = adapter != null && adapter.isEnabled();
        boolean routed = false;
        if (hce && enabled) {
            try {
                routed = CardEmulation.getInstance(adapter).isDefaultServiceForAid(
                        new ComponentName(this, CardService.class), "E80704007F00070302");
            } catch (RuntimeException exception) {
                Diagnostics.record("NFC-Zuordnung konnte nicht geprüft werden: " + exception.getMessage());
            }
        }
        boolean service = CardService.isEnabled(this);
        boolean ready = PersoSimService.isRunning();
        checkDetails = "NFC vorhanden: " + yes(adapter != null)
                + "\nKartenemulation unterstützt: " + yes(hce)
                + "\nAndroid-NFC eingeschaltet: " + yes(enabled)
                + "\nPersoSim-Kartenemulation aktiv: " + yes(service)
                + "\nAusweis-Kennung zugeordnet: " + yes(routed)
                + "\nProfil geladen: " + yes(ready);
        String message;
        if (adapter == null || !hce) message = "NFC-Kartenemulation nicht verfügbar";
        else if (!enabled) message = "NFC ist ausgeschaltet";
        else if (!service) message = "Kartenemulation ist ausgeschaltet";
        else if (!routed) message = "NFC-Zuordnung prüfen";
        else if (!PersoSimService.error.isEmpty()) message = PersoSimService.error;
        else if (pending != null) message = "Profilauftrag wird ausgeführt …";
        else if (PersoSimService.loading) message = "Profil wird geladen …";
        else if (!ready) message = "Simulator ist nicht bereit";
        else if (PersoSimService.isInUse()) message = "NFC-Verbindung aktiv";
        else message = "Bereit zum Scannen";
        boolean healthy = enabled && hce && service && routed && ready && PersoSimService.error.isEmpty();
        status.setText(message);
        status.setTextColor(healthy ? Color.rgb(26, 112, 76) : Color.rgb(145, 82, 20));
        String activeTitle = "Kein aktives Profil";
        for (int i = 0; i < profiles.size(); i++) {
            boolean selected = profiles.get(i).file.equals(PersoSimService.activeFile) && ready;
            selections.get(i).setChecked(selected);
            selections.get(i).setEnabled(pending == null && !PersoSimService.loading && !PersoSimService.isInUse());
            if (selected) activeTitle = profiles.get(i).title;
        }
        active.setText(activeTitle);
        for (Button action : profileActions) action.setEnabled(pending == null && !PersoSimService.loading && !PersoSimService.isInUse());
        contact.setText(CardService.lastContact == 0 ? "Noch kein NFC-Kontakt"
                : "Letzter NFC-Kontakt: " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(CardService.lastContact))
                + " · " + CardService.commandCount + " Kartenbefehle");
    }

    private boolean busy() {
        if (pending == null && !PersoSimService.loading && !PersoSimService.isInUse()) return false;
        Toast.makeText(this, "Bitte zuerst den laufenden Scan oder Profilwechsel abwarten.", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void submit(Intent intent, String successMessage) {
        if (busy()) return;
        pending = UUID.randomUUID().toString();
        pendingMessage = successMessage;
        try { startService(intent.putExtra("saveId", pending)); }
        catch (RuntimeException exception) {
            pending = null;
            Diagnostics.failure("Profilauftrag konnte nicht gestartet werden", exception);
            showText("Vorgang nicht abgeschlossen", "Bitte erneut versuchen.");
        }
        if (status != null) updateStatus();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        String filename = "Ausweis.perso";
        try (Cursor cursor = getContentResolver().query(data.getData(), new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null) filename = name.substring(0, Math.min(200, name.length()));
            }
        } catch (RuntimeException exception) { Diagnostics.record("Dateiname nicht verfügbar."); }
        submit(new Intent(this, PersoSimService.class).putExtra("profile", "imported-" + UUID.randomUUID() + ".json")
                .putExtra("importUri", data.getData().toString()).putExtra("importName", filename), "Ausweis importiert und aktiviert");
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("pending", pending);
        state.putString("pendingMessage", pendingMessage);
        state.putString("processId", PersoSimService.processId);
        super.onSaveInstanceState(state);
    }

    private void showChecks() {
        updateStatus();
        new AlertDialog.Builder(this).setTitle("NFC-Status").setMessage(checkDetails)
                .setPositiveButton("Schließen", null)
                .setNeutralButton("NFC-Einstellungen", (dialog, which) -> startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)))
                .setNegativeButton("Aktivieren", (dialog, which) -> {
                    CardService.setEnabled(this, true);
                    startService(new Intent(this, PersoSimService.class));
                    updateStatus();
                }).show();
    }

    private void showDetails(ProfileCatalog profile) {
        new AlertDialog.Builder(this).setTitle(profile.title).setView(detailContent(profile.subtitle + "\n\n" + profile.details))
                .setPositiveButton("Als Vorlage verwenden", (dialog, which) -> {
                    if (busy()) return;
                    startActivity(new Intent(this, ProfileEditorActivity.class).putExtra("template", profile.file)
                            .putExtra("templateTitle", profile.title));
                }).setNegativeButton("Schließen", null).show();
    }

    private void showText(String title, String content) {
        new AlertDialog.Builder(this).setTitle(title).setView(detailContent(content)).setPositiveButton("Schließen", null).show();
    }

    private ScrollView detailContent(String content) {
        ScrollView scroll = new ScrollView(this);
        TextView body = text(content, 16, false);
        body.setTextIsSelectable(true);
        body.setPadding(dp(24), dp(16), dp(24), dp(16));
        scroll.addView(body);
        return scroll;
    }

    private static String yes(boolean value) { return value ? "Ja" : "Nein"; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(222, 228, 237));
        layout.setBackground(background);
        return layout;
    }
    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        return params;
    }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(INK);
        view.setLineSpacing(dp(3), 1);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private Button button(String label, String description) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(BLUE);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setContentDescription(description);
        return button;
    }
}
