package de.persosim.android.hce;

import android.content.Context;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import de.persosim.android.app.Diagnostics;
import de.persosim.android.simulator.PersoSimService;
import de.persosim.driver.connector.SimulatorManager;
import org.globaltester.simulator.Simulator;

public class CardService extends HostApduService {
    private static final String TAG = "PersoSimHce";
    private static volatile boolean fieldActive;
    public static volatile long lastContact;
    public static volatile int commandCount;
    private Simulator sessionSimulator;

    public static boolean isFieldActive() { return fieldActive; }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences("persoSimHce", MODE_PRIVATE).getBoolean("hceEnabled", false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("persoSimHce", MODE_PRIVATE).edit().putBoolean("hceEnabled", enabled).apply();
    }

    @Override public byte[] processCommandApdu(byte[] command, Bundle extras) {
        synchronized (PersoSimService.class) {
            if (de.persosim.android.remoteifd.RemoteIfdService.connected) return status(0x6985);
            fieldActive = true;
            lastContact = System.currentTimeMillis();
            commandCount++;
            if (!isEnabled(this)) return status(0x6985);
            Simulator simulator = SimulatorManager.getSim();
            if (simulator == null || !simulator.isRunning()) return status(0x6985);
            try {
                Log.i(TAG, "received new command APDU: " + hex(command));
                if (sessionSimulator != simulator) {
                    simulator.processCommand(new byte[] {(byte) 0xff, (byte) 0xff, 0, 0});
                    sessionSimulator = simulator;
                }
                if (hex(command).equals("00A4040007F04E66E75C02D8")) {
                    command = new byte[] {0, (byte) 0xa4, 0, 0x0c};
                }
                byte[] response = simulator.processCommand(command);
                Log.i(TAG, "received new response APDU: " + hex(response));
                return response;
            } catch (RuntimeException exception) {
                Diagnostics.failure("NFC-Verarbeitung fehlgeschlagen", exception);
                return status(0x6f00);
            }
        }
    }

    @Override public void onDeactivated(int reason) { release(); }

    @Override public void onDestroy() {
        release();
        super.onDestroy();
    }

    private void release() {
        synchronized (PersoSimService.class) {
            fieldActive = false;
            if (sessionSimulator != null) {
                sessionSimulator.cardPowerDown();
                sessionSimulator = null;
                Diagnostics.record("NFC-Verbindung beendet.");
            }
        }
    }

    private static byte[] status(int status) { return new byte[] {(byte) (status >> 8), (byte) status}; }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append("0123456789ABCDEF".charAt((value & 255) >> 4));
            result.append("0123456789ABCDEF".charAt(value & 15));
        }
        return result.toString();
    }
}
