package de.persosim.android.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.test.InstrumentationTestCase;
import de.persosim.android.reader.ReaderUiActivity;
import de.persosim.android.remoteifd.AndroidRemoteConfig;
import de.persosim.simulator.PersoSim;
import de.persosim.simulator.perso.Personalization;
import de.persosim.simulator.perso.PersonalizationFactory;
import de.persosim.simulator.utils.HexString;
import java.io.*;
import java.util.*;
import org.globaltester.cryptoprovider.Crypto;
import org.globaltester.cryptoprovider.sc.ProviderSc;

public class RuntimeTest extends InstrumentationTestCase {
    private Context context;
    private File directory;
    private final java.util.Set<String> preferenceNames = new java.util.HashSet<>();
    @Override protected void setUp() throws Exception {
        Context target = getInstrumentation().getTargetContext();
        directory = new File(target.getCacheDir(), "runtime-" + UUID.randomUUID());
        assertTrue(directory.mkdirs());
        context = new ContextWrapper(target) {
            @Override public File getFilesDir() { return directory; }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                String scoped = directory.getName() + "-" + name;
                preferenceNames.add(scoped);
                return target.getSharedPreferences(scoped, mode);
            }
        };
        context.getSharedPreferences("remoteReaderPairings", 0).edit().clear().commit();
        context.getSharedPreferences("persoSimRemoteIfdConfig", 0).edit().clear().commit();
        Crypto.setCryptoProvider(new ProviderSc().getCryptoProviderObject());
    }
    @Override protected void tearDown() throws Exception {
        ReaderUiActivity.cancelPending();
        remove(directory);
        for (String name : preferenceNames) getInstrumentation().getTargetContext().deleteSharedPreferences(name);
        super.tearDown();
    }
    private void remove(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) remove(child);
        assertTrue(file.delete());
    }
    private void success(PersoSim simulator, String command) {
        byte[] response = simulator.processCommand(HexString.toByteArray(command));
        assertTrue(HexString.encode(response), response.length >= 2);
        assertEquals(HexString.encode(response), 0x90, response[response.length - 2] & 255);
        assertEquals(0, response[response.length - 1]);
    }
    private void exercise(InputStream input) throws Exception {
        PersoSim simulator = new PersoSim();
        try {
            assertTrue(simulator.loadPersonalization((Personalization) PersonalizationFactory.unmarshal(input)));
            simulator.cardReset();
            success(simulator, "00A4040C09E80704007F00070302");
            simulator.cardReset();
            success(simulator, "00A4020C02011C");
            success(simulator, "00B0000000");
            success(simulator, "0022C1A412800A04007F0007020204020283010284010D");
            success(simulator, "10860000027C0000");
        } finally { simulator.stopSimulator(); }
    }
    public void testAllOriginalsAndEditorImportStorage() throws Exception {
        List<ProfileCatalog> profiles = ProfileCatalog.read(context);
        assertEquals(17, profiles.size());
        for (ProfileCatalog profile : profiles) {
            try (InputStream input = ProfileStore.open(context, profile.file)) { exercise(input); }
        }
        ProfileData template = ProfileStore.template(context, "Profile06.perso", "Runtime test");
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : ProfileData.KEYS) values.put(key, template.get(key));
        values.put("street", "Testweg Nr. 12 8 E");
        ProfileData edited = new ProfileData(values);
        edited.validate();
        String own = "custom-" + UUID.randomUUID() + ".json";
        String imported = "imported-" + UUID.randomUUID() + ".json";
        ProfileStore.save(context, own, edited);
        assertEquals("Testweg Nr. 12 8 E", ProfileStore.read(context, own).get("street"));
        try (InputStream input = ProfileStore.open(context, own)) { exercise(input); }
        ProfileImport prepared;
        try (InputStream input = ProfileStore.open(context, own)) { prepared = ProfileStore.prepareImport(context, input, "test.perso"); }
        ProfileStore.saveImport(context, imported, prepared);
        try (InputStream input = ProfileStore.open(context, imported)) { exercise(input); }
        assertTrue(ProfileStore.isOwned(ProfileCatalog.read(context).get(0).file));
        ProfileStore.delete(context, own);
        ProfileStore.delete(context, imported);
        assertFalse(ProfileStore.exists(context, own));
        assertFalse(ProfileStore.exists(context, imported));
        assertEquals(17, ProfileCatalog.read(context).size());
    }
    public void testGeneratedAuxiliaryDatesDeserializeIndependentlyOfTimezone() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            ProfileData template = ProfileStore.read(context, null);
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : ProfileData.KEYS) values.put(key, template.get(key));
            values.put("birthDate", "29.02.2000");
            values.put("expiryDate", "15.07.2035");
            for (String zone : new String[] {"UTC", "Europe/Berlin", "Asia/Tokyo", "America/Los_Angeles"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                byte[] xml = ProfileStore.document(context, new ProfileData(values));
                Personalization profile = (Personalization) PersonalizationFactory.unmarshal(new ByteArrayInputStream(xml));
                int checked = 0;
                for (de.persosim.simulator.platform.Layer layer : profile.getLayerList()) {
                    if (!(layer instanceof de.persosim.simulator.platform.CommandProcessor)) continue;
                    for (de.persosim.simulator.cardobjects.CardObject child :
                            ((de.persosim.simulator.platform.CommandProcessor) layer).getMasterFile().getChildren()) {
                        if (!(child instanceof de.persosim.simulator.cardobjects.DateAuxObject)) continue;
                        de.persosim.simulator.cardobjects.DateAuxObject date = (de.persosim.simulator.cardobjects.DateAuxObject) child;
                        boolean birth = date.getOid().equals(de.persosim.simulator.protocols.auxVerification.AuxOid.id_DateOfBirth);
                        java.text.SimpleDateFormat utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
                        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
                        assertEquals(zone, birth ? "2000-02-28 23:00" : "2035-07-14 22:00", utc.format(date.getDate()));
                        checked++;
                    }
                }
                assertEquals(2, checked);
            }
        } finally { TimeZone.setDefault(previous); }
    }
    public void testLegacyReaderIdentityNameAndPairingsArePreserved() throws Exception {
        AndroidRemoteConfig original = new AndroidRemoteConfig(context);
        java.security.KeyStore store = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        store.load(null, new char[0]);
        store.setKeyEntry("default", original.getHostPrivateKey(), new char[0],
                new java.security.cert.Certificate[] {original.getHostCertificate()});
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        store.store(bytes, new char[0]);
        org.json.JSONObject pairings = new org.json.JSONObject();
        pairings.put(HexString.encode(original.getHostCertificate().getEncoded()), "Paired reader");
        context.getSharedPreferences("remoteReaderPairings", 0).edit().clear().commit();
        context.getSharedPreferences("persoSimRemoteIfdConfig", 0).edit()
                .putString("remote.ifd.config.keystore.hex", HexString.encode(bytes.toByteArray()))
                .putString("remote.ifd.config.pairedcerts", pairings.toString())
                .putString("remoteIfdDeviceName", "Existing reader").commit();
        AndroidRemoteConfig migrated = new AndroidRemoteConfig(context);
        assertEquals(original.getHostCertificate(), migrated.getHostCertificate());
        assertEquals(original.getHostPrivateKey(), migrated.getHostPrivateKey());
        assertEquals("Existing reader", migrated.getName());
        assertEquals("Paired reader", migrated.getPairedCertificates().get(original.getHostCertificate()));
        AndroidRemoteConfig.forgetDevice(context, AndroidRemoteConfig.pairedDevices(context).keySet().iterator().next());
        assertTrue(new AndroidRemoteConfig(context).getPairedCertificates().isEmpty());
        try {
            migrated.addPairedCertificate(original.getHostCertificate());
            fail("An interrupted pairing was restored after removal");
        } catch (IllegalStateException expected) { }
        assertTrue(new AndroidRemoteConfig(context).getPairedCertificates().isEmpty());
        AndroidRemoteConfig.setDeviceName(context, "Renamed reader");
        assertEquals("Renamed reader", new AndroidRemoteConfig(context).getName());
    }
    public void testRemoteIdentityPersistsAndOldPinCleanupCannotCancelNewRequest() throws Exception {
        AndroidRemoteConfig first = new AndroidRemoteConfig(context);
        AndroidRemoteConfig second = new AndroidRemoteConfig(context);
        assertEquals(first.getHostCertificate(), second.getHostCertificate());
        assertEquals(first.getHostPrivateKey(), second.getHostPrivateKey());
        ReaderUiActivity.PinRequest old = new ReaderUiActivity.PinRequest();
        long oldId = ReaderUiActivity.begin(old);
        ReaderUiActivity.PinRequest next = new ReaderUiActivity.PinRequest();
        long nextId = ReaderUiActivity.begin(next);
        assertNull(old.await());
        ReaderUiActivity.cancel(oldId);
        next.complete(new byte[] {1});
        assertEquals(1, next.await()[0]);
        ReaderUiActivity.cancel(nextId);
    }
    public void testCancelledPairingCannotSaveOrRenameCertificates() throws Exception {
        AndroidRemoteConfig first = new AndroidRemoteConfig(context);
        first.addPairedCertificate(first.getHostCertificate());
        first.updateUdNameForCertificate(first.getHostCertificate(), "Existing reader");
        AndroidRemoteConfig cancelled = new AndroidRemoteConfig(context);
        AndroidRemoteConfig next = new AndroidRemoteConfig(context);
        cancelled.cancelPairing();
        try {
            cancelled.addPairedCertificate(cancelled.getHostCertificate());
            fail("Cancelled handshake persisted a certificate");
        } catch (IllegalStateException expected) { }
        cancelled.updateUdNameForCertificate(cancelled.getHostCertificate(), "Stale name");
        assertEquals("Existing reader", next.getPairedCertificates().get(next.getHostCertificate()));
        next.addPairedCertificate(next.getHostCertificate());
        next.updateUdNameForCertificate(next.getHostCertificate(), "New reader");
        assertEquals("New reader", new AndroidRemoteConfig(context).getPairedCertificates().get(next.getHostCertificate()));
    }
}
