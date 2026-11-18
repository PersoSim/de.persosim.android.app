package de.persosim.build;

import de.persosim.simulator.PersoSim;
import de.persosim.simulator.perso.Personalization;
import de.persosim.simulator.perso.PersonalizationFactory;
import de.persosim.simulator.utils.HexString;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import org.globaltester.cryptoprovider.Crypto;
import org.globaltester.cryptoprovider.sc.ProviderSc;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalProfilesTest {
    @Test public void originalsLoadAndAnswerEidCommands() throws Exception {
        org.globaltester.logging.BasicLogger.addLogListener(message -> { System.out.println(message.getMessageContent()); for (org.globaltester.logging.tags.LogTag tag : message.getLogTags()) if (tag.getId().equals(org.globaltester.logging.BasicLogger.EXCEPTION_STACK_TAG_ID)) System.out.println(tag); });
        Crypto.setCryptoProvider(new ProviderSc().getCryptoProviderObject());
        File[] profiles = new File(System.getProperty("profilesDir")).listFiles((dir, name) -> name.endsWith(".perso"));
        assertNotNull(profiles);
        assertEquals(17, profiles.length);
        for (File file : profiles) {
            PersoSim simulator = new PersoSim();
            try (FileInputStream input = new FileInputStream(file)) {
                assertTrue(file.getName(), simulator.loadPersonalization((Personalization) PersonalizationFactory.unmarshal(input)));
            }
            assertTrue(file.getName(), simulator.isRunning());
            simulator.cardReset();
            assertSuccess(file, simulator.processCommand(HexString.toByteArray("00A4040C09E80704007F00070302")));
            simulator.cardReset();
            assertSuccess(file, simulator.processCommand(HexString.toByteArray("00A4020C02011C")));
            byte[] cardAccess = simulator.processCommand(HexString.toByteArray("00B0000000"));
            assertSuccess(file, cardAccess);
            assertTrue(file.getName(), cardAccess.length > 20);
            assertSuccess(file, simulator.processCommand(HexString.toByteArray("0022C1A412800A04007F0007020204020283010284010D")));
            simulator.stopSimulator();
        }
    }

    @Test public void secureMessagingStateCanBeCopied() {
        Crypto.setCryptoProvider(new ProviderSc().getCryptoProviderObject());
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(new byte[16], "AES");
        javax.crypto.spec.SecretKeySpec copy = de.persosim.simulator.utils.Serializer.deepCopy(key);
        assertEquals(key, copy);
        assertNotSame(key, copy);
    }

    private static void assertSuccess(File file, byte[] response) {
        assertTrue(file.getName(), response.length >= 2);
        assertArrayEquals(file.getName() + ": " + HexString.encode(response), new byte[] {(byte) 0x90, 0}, Arrays.copyOfRange(response, response.length - 2, response.length));
    }
}
