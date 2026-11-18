package de.persosim.android.remoteifd;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import de.persosim.android.app.Diagnostics;
import de.persosim.android.hce.CardService;
import de.persosim.android.reader.AndroidReaderUi;
import de.persosim.android.reader.ReaderUiActivity;
import de.persosim.android.simulator.PersoSimService;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.features.*;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.websocket.HandshakeResultListener;
import de.persosim.websocket.WebsocketComm;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteIfdService extends Service {
    public static final String EXTRA_PAIRING_ID = "pairingId";
    public static final String EXTRA_CANCEL_PAIRING_ID = "cancelPairingId";
    public static volatile boolean connected;
    public static volatile String status = "Aus";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private WebsocketComm communication;
    private volatile AndroidRemoteConfig activeConfiguration;
    private String pairingId;
    private static volatile String finishedPairingId;
    private volatile boolean destroyed;
    private volatile int generation;
    private static Object connectionOwner;
    private static final Object commands = new Object();
    private static volatile RemoteIfdService currentService;

    @Override public void onCreate() {
        super.onCreate();
        currentService = this;
    }

    public static boolean isUsePinPad(android.content.Context context) {
        return context.getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).getBoolean("remoteIfdUsePinPad", true);
    }

    public static boolean hasPairingFinished(String id) {
        return id != null && id.equals(finishedPairingId);
    }

    private void finishPairing() {
        if (pairingId != null) finishedPairingId = pairingId;
        pairingId = null;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String cancelled = intent == null ? null : intent.getStringExtra(EXTRA_CANCEL_PAIRING_ID);
        if (cancelled != null && !cancelled.equals(pairingId)) {
            if (generation == 0) stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (!getSharedPreferences("persoSimRemoteIfd", MODE_PRIVATE).getBoolean("remoteIfdEnabled", true)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        final String code = intent == null ? null : intent.getStringExtra("pairingCode");
        final int request = ++generation;
        finishPairing();
        pairingId = code == null ? null : intent.getStringExtra(EXTRA_PAIRING_ID);
        AndroidRemoteConfig previous = activeConfiguration;
        if (previous != null) previous.cancelPairing();
        status = "Wird gestartet …";
        ReaderUiActivity.cancelPending();
        worker.execute(() -> {
            try {
                stopCommunication();
                if (!isCurrent(request)) return;
                AndroidRemoteConfig config = new AndroidRemoteConfig(this);
                activeConfiguration = config;
                IfdConnectorImpl connector = new IfdConnectorImpl();
                connector.addUi(new AndroidReaderUi(this));
                connector.addListener(new DefaultListener());
                if (isUsePinPad(this)) {
                    connector.addListener(new VerifyPinDirect(new UnsignedInteger(0x42000DB2)));
                    connector.addListener(new ModifyPinDirect(new UnsignedInteger(0x42000DB3)));
                    connector.addListener(new MctReaderDirect(new UnsignedInteger(0x42000DB4)));
                    connector.addListener(new MctUniversal(new UnsignedInteger(0x42000DB5)));
                    connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(0x42000DCC)));
                }
                List<PcscListener> listeners = connector.getListeners();
                final Object session = new Object();
                communication = new WebsocketComm(code, config, new HandshakeResultListener() {
                    @Override public void onHandshakeFinished(boolean success) {
                        synchronized (PersoSimService.class) {
                            if (!isCurrent(request)) return;
                            if (success && connectionOwner == null && !CardService.isFieldActive()
                                    && !PersoSimService.loading && PersoSimService.isRunning()) {
                                connectionOwner = session;
                                connected = true;
                                status = "Verbunden";
                            } else status = success ? "Karte wird bereits verwendet" : "Kopplung fehlgeschlagen";
                        }
                    }
                    @Override public void onConnectionClosed() {
                        synchronized (PersoSimService.class) {
                            if (connectionOwner == session) {
                                if (de.persosim.driver.connector.SimulatorManager.getSim() != null) {
                                    de.persosim.driver.connector.SimulatorManager.getSim().cardPowerDown();
                                }
                                connectionOwner = null;
                                connected = false;
                            }
                            if (!isCurrent(request)) return;
                            status = "Bereit im WLAN";
                        }
                        if (code != null) main.post(() -> {
                            if (isCurrent(request)) onStartCommand(null, 0, 0);
                        });
                    }
                });
                communication.setListeners(Collections.singletonList(data -> {
                    synchronized (commands) {
                    synchronized (PersoSimService.class) {
                        if (connectionOwner != session || !isCurrent(request) || CardService.isFieldActive() || PersoSimService.loading || !PersoSimService.isRunning()) {
                            return new SimplePcscCallResult(PcscConstants.IFD_ICC_NOT_PRESENT);
                        }
                    }
                    de.persosim.driver.connector.pcsc.PcscCallResult result = null;
                    for (PcscListener listener : listeners) {
                        de.persosim.driver.connector.pcsc.PcscCallResult next = listener.processPcscCall(data);
                        if (result == null) result = next;
                    }
                    return result;
                    }
                }));
                if (!isCurrent(request)) return;
                communication.start();
                if (!isCurrent(request)) { stopCommunication(); return; }
                status = code == null ? "Bereit im WLAN" : "Kopplungscode: " + code;
            } catch (Exception exception) {
                if (isCurrent(request)) {
                    status = "Start fehlgeschlagen";
                    main.post(() -> { if (isCurrent(request)) finishPairing(); });
                }
                Diagnostics.failure("WLAN-Kartenleser", exception);
            }
        });
        return START_NOT_STICKY;
    }
    private boolean isCurrent(int request) {
        return !destroyed && generation == request && currentService == this;
    }
    private void stopCommunication() {
        AndroidRemoteConfig config = activeConfiguration;
        if (config != null) config.cancelPairing();
        if (communication != null) communication.stop();
        communication = null;
        activeConfiguration = null;
    }
    @Override public void onDestroy() {
        destroyed = true;
        generation++;
        finishPairing();
        AndroidRemoteConfig config = activeConfiguration;
        if (config != null) config.cancelPairing();
        ReaderUiActivity.cancelPending();
        worker.execute(() -> {
            stopCommunication();
            if (currentService == this) status = "Aus";
        });
        worker.shutdown();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
