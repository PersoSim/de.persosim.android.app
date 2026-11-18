package de.persosim.android.reader;

import android.content.Context;
import android.content.Intent;
import de.persosim.android.app.Diagnostics;
import de.persosim.driver.connector.VirtualReaderUi;
import java.io.IOException;

public final class AndroidReaderUi implements VirtualReaderUi {
    private final Context context;
    private String prompt = "Bitte Geheimzahl eingeben";
    public AndroidReaderUi(Context context) { this.context = context; }
    @Override public byte[] getPin() throws IOException {
        ReaderUiActivity.PinRequest result = new ReaderUiActivity.PinRequest();
        long id = ReaderUiActivity.begin(result);
        try {
            context.startActivity(new Intent(context, ReaderUiActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("requestId", id).putExtra("prompt", prompt));
            return result.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("PIN-Eingabe unterbrochen", exception);
        } catch (Exception exception) {
            throw new IOException("PIN-Eingabe nicht abgeschlossen", exception);
        } finally { ReaderUiActivity.cancel(id); }
    }
    @Override public void display(String... lines) {
        prompt = String.join("\n", lines);
        Diagnostics.record("Kartenleser: " + prompt);
    }
    @Override public byte[] getDeviceDescriptors() { return new byte[0]; }
    @Override public void iccPresence(boolean present) { }
    @Override public void displayChat(byte[] chat) {
        if (chat != null) Diagnostics.record("Kartenleser: Berechtigungsanfrage " + de.persosim.simulator.utils.HexString.encode(chat));
    }
}
