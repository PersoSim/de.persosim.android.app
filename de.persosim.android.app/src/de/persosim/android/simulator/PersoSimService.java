package de.persosim.android.simulator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.net.Uri;
import android.widget.Toast;
import de.persosim.android.app.Diagnostics;
import de.persosim.android.app.ProfileCatalog;
import de.persosim.android.app.ProfileData;
import de.persosim.android.app.ProfileStore;
import de.persosim.android.app.ProfileImport;
import de.persosim.android.hce.CardService;
import de.persosim.driver.connector.SimulatorManager;
import de.persosim.simulator.PersoSim;
import de.persosim.simulator.perso.Personalization;
import de.persosim.simulator.perso.PersonalizationFactory;
import de.persosim.simulator.utils.Base64;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import org.globaltester.cryptoprovider.Crypto;
import org.globaltester.cryptoprovider.sc.ProviderSc;

public class PersoSimService extends Service {
    public static final String processId = UUID.randomUUID().toString();
    public static volatile boolean loading;
    public static volatile String activeFile = "";
    public static volatile String error = "";
    public static volatile String completedSave = "";
    public static volatile String saveError = "";
    private static volatile boolean ready;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int generation;
    private String activeSaveId;

    public static boolean isInUse() {
        return CardService.isFieldActive() || de.persosim.android.remoteifd.RemoteIfdService.connected;
    }

    public static boolean isRunning() { return ready; }

    public static String savedFile(Context context) {
        String file = context.getSharedPreferences("profileSelection", MODE_PRIVATE)
                .getString("file", "Profile01.perso");
        return ProfileStore.isOwned(file) && !ProfileStore.exists(context, file) ? "Profile01.perso" : file;
    }

    @Override public void onCreate() {
        super.onCreate();
        Diagnostics.initialize();
        Crypto.setCryptoProvider(new ProviderSc().getCryptoProviderObject());
        Base64.setEncoder(new Base64.Encoder() {
            @Override public String encode(byte[] bytes) {
                return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
            }
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String requested = intent == null ? null : intent.getStringExtra("profile");
        final String draft = intent == null ? null : intent.getStringExtra("draft");
        final String saveId = intent == null ? null : intent.getStringExtra("saveId");
        final String importUri = intent == null ? null : intent.getStringExtra("importUri");
        final String importName = intent == null ? "Ausweis.perso" : intent.getStringExtra("importName");
        final boolean deleting = intent != null && intent.getBooleanExtra("delete", false);
        if (requested == null && (isRunning() || loading)) return START_NOT_STICKY;
        if (loading || PersoSimService.isInUse()) {
            completeSave(saveId, "Bitte zuerst den laufenden Scan oder Profilwechsel abwarten.");
            Toast.makeText(this, "Bitte zuerst den laufenden Scan oder Profilwechsel abwarten.", Toast.LENGTH_SHORT).show();
            return START_NOT_STICKY;
        }
        final String file = requested == null ? savedFile(this) : requested;
        final boolean replaceDeleted = deleting && (file.equals(activeFile) || file.equals(savedFile(this)) || !isRunning());
        final int request = ++generation;
        activeSaveId = saveId;
        loading = true;
        error = "";
        worker.execute(new Runnable() {
            @Override public void run() {
                boolean posted = false;
                try {
                    if ((deleting ? 1 : 0) + (draft != null ? 1 : 0) + (importUri != null ? 1 : 0) > 1) {
                        throw new IllegalArgumentException("Ungültiger Profilauftrag.");
                    }
                    final ProfileData edited = draft == null ? null : ProfileStore.decode(draft);
                    final ProfileImport imported;
                    if (importUri != null) {
                        if (!ProfileStore.isImported(file) || !"content".equals(Uri.parse(importUri).getScheme())) {
                            throw new IllegalArgumentException("Bitte eine Datei über die Dateiauswahl öffnen.");
                        }
                        try (InputStream input = getContentResolver().openInputStream(Uri.parse(importUri))) {
                            imported = ProfileStore.prepareImport(PersoSimService.this, input, importName == null ? "Ausweis.perso" : importName);
                        }
                    } else imported = null;
                    boolean known = false;
                    for (ProfileCatalog profile : ProfileCatalog.read(PersoSimService.this)) {
                        if (profile.file.equals(file)) known = true;
                    }
                    if (deleting && (!ProfileStore.isOwned(file) || !ProfileStore.exists(PersoSimService.this, file))) {
                        throw new IllegalArgumentException("Dieser Ausweis kann nicht gelöscht werden.");
                    }
                    if (!deleting && !known && !(edited != null && ProfileStore.isCustom(file)) && imported == null) {
                        throw new IllegalArgumentException("Unbekanntes Profil: " + file);
                    }
                    if (edited != null && !ProfileStore.isCustom(file)) throw new IllegalArgumentException("Originalprofile können nicht überschrieben werden");
                    final String nextFile = replaceDeleted ? "Profile01.perso" : file;
                    final PersoSim candidate = deleting && !replaceDeleted ? null : new PersoSim();
                    if (candidate != null) {
                        try (InputStream input = imported != null ? new ByteArrayInputStream(imported.xml)
                                : edited == null ? ProfileStore.open(PersoSimService.this, nextFile)
                                : new ByteArrayInputStream(ProfileStore.document(PersoSimService.this, edited))) {
                            Personalization profile = (Personalization) PersonalizationFactory.unmarshal(input);
                            if (!candidate.loadPersonalization(profile)) {
                                throw new IllegalStateException("Profil konnte nicht initialisiert werden");
                            }
                        }
                    }
                    main.post(new Runnable() {
                        @Override public void run() {
                            synchronized (PersoSimService.class) {
                                if (request != generation) return;
                                loading = false;
                                if (PersoSimService.isInUse()) {
                                    completeSave(saveId, "Bitte das Lesegerät entfernen und erneut versuchen.");
                                    Toast.makeText(PersoSimService.this, "Profilwechsel abgebrochen: Bitte zuerst das Lesegerät entfernen.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    if (edited != null) ProfileStore.save(PersoSimService.this, file, edited);
                                    if (imported != null) ProfileStore.saveImport(PersoSimService.this, file, imported);
                                    if (deleting) ProfileStore.delete(PersoSimService.this, file);
                                } catch (Exception exception) {
                                    Diagnostics.failure("Profiländerung fehlgeschlagen", exception);
                                    completeSave(saveId, "Änderung fehlgeschlagen. Bitte erneut versuchen.");
                                    return;
                                }
                                if (candidate != null) {
                                    SimulatorManager.setSimulator(candidate);
                                    ready = candidate.isRunning();
                                    activeFile = nextFile;
                                    getSharedPreferences("profileSelection", MODE_PRIVATE)
                                            .edit().putString("file", nextFile).apply();
                                }
                                error = "";
                                Diagnostics.record((deleting ? "Profil gelöscht: " : "Profil geladen: ") + file);
                                completeSave(saveId, "");
                            }
                        }
                    });
                    posted = true;
                } catch (final Exception exception) {
                    Diagnostics.failure("Profilauftrag fehlgeschlagen", exception);
                    final String reason = exception instanceof java.io.IOException || exception instanceof IllegalArgumentException
                            ? exception.getMessage() : "Die Datei ist kein vollständiges, unterstütztes PersoSim-Profil.";
                    main.post(() -> {
                        if (request == generation) completeSave(saveId, reason == null ? "Profilauftrag fehlgeschlagen." : reason);
                    });
                } finally {
                    if (!posted) {
                        main.post(new Runnable() {
                            @Override public void run() {
                                if (request != generation) return;
                                loading = false;
                                if (saveId == null) error = "Profil konnte nicht geladen werden. Details im Protokoll.";
                            }
                        });
                    }
                }
            }
        });
        return START_NOT_STICKY;
    }

    private static void completeSave(String id, String message) {
        if (id == null) return;
        saveError = message;
        completedSave = id;
    }

    @Override public void onDestroy() {
        synchronized (PersoSimService.class) {
            if (activeSaveId != null && !activeSaveId.equals(completedSave)) {
                completeSave(activeSaveId, "Vorgang wurde unterbrochen. Bitte erneut versuchen.");
            }
            generation++;
            loading = false;
            stopService(new Intent(this, de.persosim.android.remoteifd.RemoteIfdService.class));
            if (!isInUse()) SimulatorManager.setSimulator(null);
            ready = false;
            activeFile = "";
        }
        worker.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
