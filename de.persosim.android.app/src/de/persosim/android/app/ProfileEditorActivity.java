package de.persosim.android.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import de.persosim.android.hce.CardService;
import de.persosim.android.simulator.PersoSimService;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileEditorActivity extends Activity {
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private final Handler handler = new Handler();
    private String file;
    private String pending;
    private Button save;
    private Button reset;
    private Button cancel;
    private TextView message;
    private Spinner documentType;
    private List<String> documentTypes;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (pending != null && pending.equals(PersoSimService.completedSave)) {
                pending = null;
                if (PersoSimService.saveError.isEmpty()) {
                    Toast.makeText(ProfileEditorActivity.this, "Profil gespeichert und aktiviert", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                message.setText(PersoSimService.saveError);
            }
            boolean waiting = pending != null;
            for (EditText field : fields.values()) field.setEnabled(!waiting);
            documentType.setEnabled(!waiting);
            reset.setEnabled(!waiting);
            cancel.setEnabled(!waiting);
            save.setEnabled(!waiting && !PersoSimService.loading && !PersoSimService.isInUse());
            save.setText(waiting ? "Profil wird geprüft …" : "Speichern und aktivieren");
            if (!waiting && PersoSimService.isInUse()) message.setText("Zum Speichern bitte das Lesegerät entfernen.");
            handler.postDelayed(this, 500);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String original = getIntent().getStringExtra("profile");
        String template = getIntent().getStringExtra("template");
        file = state == null ? (original == null ? "custom-" + UUID.randomUUID() + ".json" : original) : state.getString("file");
        ProfileData data;
        try {
            data = state != null && state.containsKey("draft") ? ProfileStore.decode(state.getString("draft")) : template == null
                    ? ProfileStore.read(this, original) : ProfileStore.template(this, template, getIntent().getStringExtra("templateTitle"));
            pending = state == null ? null : state.getString("pending");
            if (state != null && !PersoSimService.processId.equals(state.getString("processId"))) pending = null;
        } catch (Exception exception) {
            Diagnostics.failure("Editor konnte nicht geöffnet werden", exception);
            Toast.makeText(this, "Profil konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        LinearLayout page = column();
        page.setPadding(dp(20), dp(14), dp(20), dp(12));
        page.setBackgroundColor(Color.rgb(243, 246, 251));
        page.addView(label(original == null ? "Neuer Ausweis" : "Ausweis bearbeiten", 24));
        page.addView(label(template == null ? "Eigene Testdaten auf Basis von Originalprofil 01" : "Neue Kopie mit den Daten der gewählten Vorlage", 14));
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = column();
        reset = button("Standardvorlage einsetzen");
        reset.setOnClickListener(view -> {
            try { fill(ProfileStore.read(this, null)); message.setText(""); }
            catch (Exception exception) { message.setText("Vorlage konnte nicht geladen werden."); }
        });
        form.addView(reset);
        TextView typeLabel = label("Ausweisart", 14);
        documentType = new Spinner(this);
        documentType.setId(R.id.document_type);
        documentType.setSaveEnabled(false);
        documentType.setContentDescription("Ausweisart auswählen");
        typeLabel.setLabelFor(documentType.getId());
        form.addView(typeLabel);
        form.addView(documentType, new LinearLayout.LayoutParams(-1, dp(52)));
        for (int i = 0; i < ProfileData.KEYS.length; i++) {
            String key = ProfileData.KEYS[i];
            if (key.equals("documentType")) continue;
            if (key.equals("documentNumber") || key.equals("street") || key.equals("pin")) {
                TextView section = label(key.equals("documentNumber") ? "Person" : key.equals("street") ? "Anschrift" : "Test-Zugangsdaten", 20);
                section.setPadding(0, dp(20), 0, dp(6));
                form.addView(section);
            }
            TextView label = label(ProfileData.LABELS[i], 14);
            EditText field = new EditText(this);
            field.setId(1000 + i);
            label.setLabelFor(field.getId());
            field.setContentDescription(ProfileData.LABELS[i]);
            field.setTextSize(16);
            field.setSingleLine(true);
            field.setInputType(key.equals("pin") || key.equals("can") || key.equals("puk") || key.equals("communityId")
                    ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
            field.setSaveEnabled(false);
            form.addView(label);
            form.addView(field, new LinearLayout.LayoutParams(-1, -2));
            if (key.equals("communityId")) form.addView(label("Für Wohnortabfragen; wird nicht automatisch aus der Anschrift berechnet.", 13));
            fields.put(key, field);
        }
        fill(data);
        scroll.addView(form);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        message = label("", 14);
        message.setTextColor(Color.rgb(145, 60, 20));
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        page.addView(message);
        save = button("Speichern und aktivieren");
        save.setOnClickListener(view -> save());
        page.addView(save);
        cancel = button("Abbrechen");
        cancel.setOnClickListener(view -> finish());
        page.addView(cancel);
        page.setFocusableInTouchMode(true);
        setContentView(page);
        page.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void fill(ProfileData data) {
        for (String key : fields.keySet()) {
            EditText field = fields.get(key);
            InputFilter[] filters = field.getFilters();
            field.setFilters(new InputFilter[0]);
            field.setText(data.get(key));
            field.setFilters(filters);
            field.setError(null);
        }
        documentTypes = new ArrayList<>(Arrays.asList(DocumentType.EDITOR_CODES));
        if (!documentTypes.contains(data.get("documentType"))) documentTypes.add(0, data.get("documentType"));
        List<String> choices = new ArrayList<>();
        for (String code : documentTypes) choices.add(code.isEmpty() ? "Bitte auswählen" : DocumentType.label(code) + " (" + code + ")");
        ArrayAdapter<String> types = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentType.setAdapter(types);
        documentType.setSelection(documentTypes.indexOf(data.get("documentType")));
    }

    private ProfileData collect() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : fields.keySet()) values.put(key, fields.get(key).getText().toString());
        values.put("documentType", documentTypes.get(documentType.getSelectedItemPosition()));
        return new ProfileData(values);
    }

    private void save() {
        if (pending != null) return;
        if (PersoSimService.loading || PersoSimService.isInUse()) {
            message.setText("Bitte zuerst den laufenden Scan oder Profilwechsel abwarten.");
            return;
        }
        try {
            ProfileData data = collect();
            data.validate();
            String draft = ProfileStore.encode(data);
            pending = UUID.randomUUID().toString();
            message.setText("");
            save.setEnabled(false);
            startService(new Intent(this, PersoSimService.class).putExtra("profile", file)
                    .putExtra("draft", draft).putExtra("saveId", pending));
        } catch (ProfileData.InvalidField exception) {
            EditText field = fields.get(exception.key);
            if (field != null) { field.setError(exception.getMessage()); field.requestFocus(); }
            else documentType.requestFocus();
            message.setText(exception.getMessage());
        } catch (Exception exception) {
            pending = null;
            Diagnostics.failure("Speichern konnte nicht gestartet werden", exception);
            message.setText("Speichern konnte nicht gestartet werden.");
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("file", file);
        state.putString("pending", pending);
        state.putString("processId", PersoSimService.processId);
        try { state.putString("draft", ProfileStore.encode(collect())); }
        catch (Exception exception) { Diagnostics.failure("Entwurf konnte nicht gesichert werden", exception); }
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() { super.onResume(); if (save != null) handler.postDelayed(refresh, 250); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onBackPressed() {
        if (pending == null) super.onBackPressed();
        else Toast.makeText(this, "Bitte das Speichern abwarten", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(28, 40, 61));
        return view;
    }
    private Button button(String text) {
        Button view = new Button(this);
        view.setText(text);
        view.setAllCaps(false);
        view.setTextColor(Color.rgb(37, 83, 177));
        return view;
    }
}
