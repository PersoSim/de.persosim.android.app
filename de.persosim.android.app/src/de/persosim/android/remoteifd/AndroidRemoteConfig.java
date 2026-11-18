package de.persosim.android.remoteifd;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import android.util.Base64;
import de.persosim.websocket.RemoteIfdConfigManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class AndroidRemoteConfig implements RemoteIfdConfigManager {
    private static long pairingRevision;
    private final long revision;
    private final SharedPreferences preferences;
    private final SharedPreferences configuration;
    private final Certificate certificate;
    private final RSAPrivateKey privateKey;
    private boolean pairingCancelled;

    public AndroidRemoteConfig(Context context) throws Exception {
        preferences = context.getSharedPreferences("remoteReaderPairings", Context.MODE_PRIVATE);
        configuration = context.getSharedPreferences("persoSimRemoteIfdConfig", Context.MODE_PRIVATE);
        migratePairings(context);
        synchronized (AndroidRemoteConfig.class) { revision = pairingRevision; }
        String legacy = configuration.getString("remote.ifd.config.keystore.hex", null);
        if (legacy != null) {
            KeyStore previous = KeyStore.getInstance(KeyStore.getDefaultType());
            previous.load(new ByteArrayInputStream(de.persosim.simulator.utils.HexString.toByteArray(legacy)),
                    configuration.getString("remote.ifd.config.keystore.password", "").toCharArray());
            certificate = previous.getCertificate("default");
            privateKey = (RSAPrivateKey) previous.getKey("default",
                    configuration.getString("remote.ifd.config.keystore.key.password", "").toCharArray());
            if (certificate == null || privateKey == null) throw new IllegalStateException("WLAN-Identität ist unvollständig");
            return;
        }
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "remote-reader.p12"));
        KeyStore store = KeyStore.getInstance("PKCS12");
        char[] password = new char[0];
        if (file.getBaseFile().exists() || new File(file.getBaseFile() + ".bak").exists()) {
            try (FileInputStream input = file.openRead()) { store.load(input, password); }
        } else {
            store.load(null, password);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            long now = System.currentTimeMillis();
            X500Name name = new X500Name("CN=PersoSim Android");
            org.bouncycastle.jce.provider.BouncyCastleProvider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            Certificate created = new JcaX509CertificateConverter().setProvider(provider).getCertificate(
                    new JcaX509v3CertificateBuilder(name, new BigInteger(128, new SecureRandom()),
                            new Date(now - 86400000L), new Date(now + 315360000000L), name, pair.getPublic())
                            .build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.getPrivate())));
            store.setKeyEntry("reader", pair.getPrivate(), password, new Certificate[] {created});
            FileOutputStream output = file.startWrite();
            try {
                store.store(output, password);
                file.finishWrite(output);
            } catch (Exception exception) {
                file.failWrite(output);
                throw exception;
            }
        }
        certificate = store.getCertificate("reader");
        privateKey = (RSAPrivateKey) store.getKey("reader", password);
    }

    @Override public Certificate getHostCertificate() { return certificate; }
    @Override public RSAPrivateKey getHostPrivateKey() { return privateKey; }
    @Override public String getName() { return configuration.getString("remoteIfdDeviceName", "PersoSimAndroid"); }
    public static String deviceName(Context context) {
        return context.getSharedPreferences("persoSimRemoteIfdConfig", Context.MODE_PRIVATE).getString("remoteIfdDeviceName", "PersoSimAndroid");
    }
    public static void setDeviceName(Context context, String name) {
        String value = name.trim();
        if (value.isEmpty() || value.length() > 80) throw new IllegalArgumentException("Bitte einen Namen mit 1 bis 80 Zeichen eingeben.");
        context.getSharedPreferences("persoSimRemoteIfdConfig", Context.MODE_PRIVATE).edit().putString("remoteIfdDeviceName", value).apply();
    }
    private static synchronized SharedPreferences migratePairings(Context context) {
        SharedPreferences target = context.getSharedPreferences("remoteReaderPairings", Context.MODE_PRIVATE);
        if (target.getBoolean("legacyImported", false)) return target;
        SharedPreferences legacy = context.getSharedPreferences("persoSimRemoteIfdConfig", Context.MODE_PRIVATE);
        try {
            org.json.JSONObject entries = new org.json.JSONObject(legacy.getString("remote.ifd.config.pairedcerts", "{}"));
            SharedPreferences.Editor editor = target.edit();
            java.util.Iterator<String> keys = entries.keys();
            while (keys.hasNext()) {
                String hex = keys.next();
                String key = Base64.encodeToString(de.persosim.simulator.utils.HexString.toByteArray(hex), Base64.NO_WRAP);
                if (!target.contains(key)) editor.putString(key, entries.getString(hex));
            }
            if (!editor.putBoolean("legacyImported", true).commit()) throw new IllegalStateException("Kopplungen konnten nicht übernommen werden");
            return target;
        } catch (org.json.JSONException exception) { throw new IllegalStateException("Alte Kopplungen sind ungültig", exception); }
    }
    public static synchronized Map<String, String> pairedDevices(Context context) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : migratePairings(context).getAll().entrySet()) {
            if (entry.getValue() instanceof String) result.put(entry.getKey(), (String) entry.getValue());
        }
        return result;
    }
    public static synchronized void forgetDevice(Context context, String key) {
        pairingRevision++;
        if (!migratePairings(context).edit().remove(key).commit()) throw new IllegalStateException("Kopplung konnte nicht gelöscht werden");
    }
    public static synchronized void forgetAll(Context context) {
        pairingRevision++;
        if (!migratePairings(context).edit().clear().putBoolean("legacyImported", true).commit()) throw new IllegalStateException("Kopplungen konnten nicht gelöscht werden");
    }
    @Override public synchronized Map<Certificate, String> getPairedCertificates() {
        Map<Certificate, String> result = new LinkedHashMap<>();
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                if (!(entry.getValue() instanceof String)) continue;
                result.put(factory.generateCertificate(new ByteArrayInputStream(Base64.decode(entry.getKey(), Base64.NO_WRAP))), String.valueOf(entry.getValue()));
            }
            return result;
        } catch (Exception exception) { throw new IllegalStateException("Gespeicherte Kopplung ist ungültig", exception); }
    }
    private String key(Certificate value) {
        try { return Base64.encodeToString(value.getEncoded(), Base64.NO_WRAP); }
        catch (Exception exception) { throw new IllegalArgumentException(exception); }
    }
    public void cancelPairing() {
        synchronized (AndroidRemoteConfig.class) { pairingCancelled = true; }
    }
    @Override public synchronized void addPairedCertificate(Certificate value) {
        synchronized (AndroidRemoteConfig.class) {
            if (pairingCancelled || revision != pairingRevision) throw new IllegalStateException("Kopplung wurde abgebrochen");
            if (!preferences.edit().putString(key(value), "Gekoppeltes Gerät").commit()) throw new IllegalStateException("Kopplung konnte nicht gespeichert werden");
        }
    }
    @Override public synchronized void deletePairedCertificate(Certificate value) {
        if (!preferences.edit().remove(key(value)).commit()) throw new IllegalStateException("Kopplung konnte nicht gelöscht werden");
    }
    @Override public synchronized void updateUdNameForCertificate(Certificate value, String name) {
        synchronized (AndroidRemoteConfig.class) {
            if (!pairingCancelled && revision == pairingRevision && preferences.contains(key(value))) {
                if (!preferences.edit().putString(key(value), name).commit()) throw new IllegalStateException("Gerätename konnte nicht gespeichert werden");
            }
        }
    }
}
