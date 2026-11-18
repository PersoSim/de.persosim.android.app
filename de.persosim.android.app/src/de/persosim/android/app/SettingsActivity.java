package de.persosim.android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import de.persosim.android.hce.CardService;
import de.persosim.android.remoteifd.RemoteIfdService;
import de.persosim.android.remoteifd.AndroidRemoteConfig;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

public class SettingsActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView status;
    private LinearLayout pairedDevices;
    private Switch remote;
    private Switch pinPad;
    private EditText name;
    private Button saveName;
    private String pairingId;
    private String pairingCode;
    private AlertDialog pairingDialog;
    private java.util.Map<String, String> shownPairings;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            status.setText(RemoteIfdService.status);
            updateConfigurationEnabled();
            if (RemoteIfdService.hasPairingFinished(pairingId)) dismissPairing();
            refreshPairings();
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        scroll.addView(layout);
        TextView title = new TextView(this);
        title.setText("Einstellungen");
        title.setTextSize(26);
        layout.addView(title);
        addHeading(layout, "Remote IFD");
        TextView help = new TextView(this);
        help.setText("Der WLAN-Kartenleser verbindet PersoSim mit einer AusweisApp im selben WLAN. Für eine neue Verbindung zuerst koppeln.");
        layout.addView(help);
        remote = new Switch(this);
        remote.setText("Remote-IFD-Dienst (WLAN)");
        remote.setChecked(getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).getBoolean("remoteIfdEnabled", true));
        remote.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).edit().putBoolean("remoteIfdEnabled", checked).apply();
            Intent service = new Intent(this, RemoteIfdService.class);
            if (checked) startService(service); else stopService(service);
            updateConfigurationEnabled();
        });
        layout.addView(remote);
        pinPad = new Switch(this);
        pinPad.setText("PIN-Pad-Modus");
        pinPad.setChecked(RemoteIfdService.isUsePinPad(this));
        pinPad.setOnCheckedChangeListener((button, checked) -> {
            if (checked == RemoteIfdService.isUsePinPad(this)) return;
            if (remote.isChecked() || de.persosim.android.simulator.PersoSimService.isInUse()) {
                pinPad.setChecked(RemoteIfdService.isUsePinPad(this));
                return;
            }
            getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).edit().putBoolean("remoteIfdUsePinPad", checked).apply();
        });
        layout.addView(pinPad);
        TextView pinPadHelp = new TextView(this);
        pinPadHelp.setText(R.string.pin_pad_help);
        layout.addView(pinPadHelp);
        TextView configurationHelp = new TextView(this);
        configurationHelp.setText("Zum Ändern von PIN-Pad-Modus oder Gerätename den Remote-IFD-Dienst ausschalten.");
        layout.addView(configurationHelp);
        TextView nameLabel = new TextView(this);
        nameLabel.setText(R.string.remote_reader_name);
        layout.addView(nameLabel);
        name = new EditText(this);
        name.setHint(R.string.remote_reader_name);
        name.setSingleLine(true);
        name.setText(AndroidRemoteConfig.deviceName(this));
        layout.addView(name);
        saveName = new Button(this);
        saveName.setText("Gerätenamen speichern");
        saveName.setOnClickListener(view -> {
            if (remote.isChecked() || de.persosim.android.simulator.PersoSimService.isInUse()) return;
            try {
                AndroidRemoteConfig.setDeviceName(this, name.getText().toString());
                android.widget.Toast.makeText(this, "Gerätename gespeichert", android.widget.Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException exception) { name.setError(exception.getMessage()); }
        });
        layout.addView(saveName);
        status = new TextView(this);
        layout.addView(status);
        Button pair = new Button(this);
        pair.setText("Neues Gerät koppeln");
        pair.setOnClickListener(view -> {
            if (de.persosim.android.simulator.PersoSimService.isInUse()) {
                new AlertDialog.Builder(this).setMessage("Bitte zuerst den laufenden Scan beenden.").setPositiveButton("OK", null).show();
                return;
            }
            remote.setChecked(true);
            pairingId = UUID.randomUUID().toString();
            pairingCode = String.format(Locale.ROOT, "%04d", new SecureRandom().nextInt(10000));
            startService(new Intent(this, RemoteIfdService.class).putExtra("pairingCode", pairingCode)
                    .putExtra(RemoteIfdService.EXTRA_PAIRING_ID, pairingId));
            showPairing();
        });
        layout.addView(pair);
        pairedDevices = new LinearLayout(this);
        pairedDevices.setOrientation(LinearLayout.VERTICAL);
        layout.addView(pairedDevices);
        Button forget = new Button(this);
        forget.setText("WLAN-Kopplungen löschen");
        forget.setOnClickListener(view -> new AlertDialog.Builder(this).setMessage("Alle WLAN-Kopplungen löschen?")
                .setNegativeButton("Abbrechen", null).setPositiveButton("Löschen", (dialog, which) -> {
                    remote.setChecked(false);
                    AndroidRemoteConfig.forgetAll(this);
                    refreshPairings();
                }).show());
        layout.addView(forget);
        addHeading(layout, "HostCardEmulation");
        Switch nfc = new Switch(this);
        nfc.setText("NFC-Kartenemulation");
        nfc.setChecked(CardService.isEnabled(this));
        nfc.setOnCheckedChangeListener((button, checked) -> CardService.setEnabled(this, checked));
        layout.addView(nfc);
        TextView restrictions = new TextView(this);
        restrictions.setText(R.string.hce_restrictions);
        layout.addView(restrictions);
        TextView nfcHelp = new TextView(this);
        nfcHelp.setText("NFC wird beim nächsten App-Start wieder aktiviert.");
        layout.addView(nfcHelp);
        Button back = new Button(this);
        back.setText("Zurück");
        back.setOnClickListener(view -> finish());
        layout.addView(back);
        setContentView(scroll);
        updateConfigurationEnabled();
        if (state != null && de.persosim.android.simulator.PersoSimService.processId.equals(state.getString("processId"))) {
            pairingId = state.getString("pairingId");
            pairingCode = state.getString("pairingCode");
            if (pairingId != null && pairingCode != null && !RemoteIfdService.hasPairingFinished(pairingId)) showPairing();
            else pairingId = null;
        }
    }
    private void addHeading(LinearLayout layout, String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(20);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        heading.setPadding(0, padding, 0, padding / 2);
        layout.addView(heading);
    }
    private void updateConfigurationEnabled() {
        boolean enabled = !remote.isChecked() && !de.persosim.android.simulator.PersoSimService.isInUse();
        pinPad.setEnabled(enabled);
        name.setEnabled(enabled);
        saveName.setEnabled(enabled);
    }
    private void showPairing() {
        final String id = pairingId;
        pairingDialog = new AlertDialog.Builder(this).setTitle("Kopplungscode: " + pairingCode)
                .setMessage("In der AusweisApp diesen WLAN-Kartenleser auswählen und den Code eingeben.")
                .setNegativeButton("Kopplung abbrechen", null).create();
        pairingDialog.setOnDismissListener(dialog -> {
            if (id.equals(pairingId)) cancelPairing();
        });
        pairingDialog.show();
    }
    private void cancelPairing() {
        String cancelled = pairingId;
        pairingId = null;
        if (cancelled != null) startService(new Intent(this, RemoteIfdService.class)
                .putExtra(RemoteIfdService.EXTRA_CANCEL_PAIRING_ID, cancelled));
    }
    private void dismissPairing() {
        pairingId = null;
        if (pairingDialog != null) pairingDialog.dismiss();
        pairingDialog = null;
    }
    private void refreshPairings() {
        try {
            java.util.Map<String, String> entries = AndroidRemoteConfig.pairedDevices(this);
            if (entries.equals(shownPairings)) return;
            shownPairings = entries;
            pairedDevices.removeAllViews();
            TextView heading = new TextView(this);
            heading.setText(entries.isEmpty() ? "Keine gekoppelten Geräte" : "Gekoppelte Geräte");
            pairedDevices.addView(heading);
            for (java.util.Map.Entry<String, String> entry : entries.entrySet()) {
                Button remove = new Button(this);
                remove.setText(entry.getValue() + " entfernen");
                remove.setOnClickListener(view -> new AlertDialog.Builder(this)
                        .setMessage("Kopplung mit „" + entry.getValue() + "“ entfernen?")
                        .setNegativeButton("Abbrechen", null).setPositiveButton("Entfernen", (dialog, which) -> {
                            remote.setChecked(false);
                            AndroidRemoteConfig.forgetDevice(this, entry.getKey());
                            refreshPairings();
                        }).show());
                pairedDevices.addView(remove);
            }
        } catch (RuntimeException exception) { status.setText(exception.getMessage()); }
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("pairingId", pairingId);
        state.putString("pairingCode", pairingCode);
        state.putString("processId", de.persosim.android.simulator.PersoSimService.processId);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (!isChangingConfigurations()) cancelPairing();
        dismissPairing();
        super.onDestroy();
    }
}
