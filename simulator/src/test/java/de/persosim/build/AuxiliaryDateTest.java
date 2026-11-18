package de.persosim.build;

import de.persosim.simulator.cardobjects.*;
import de.persosim.simulator.protocols.auxVerification.AuxOid;
import de.persosim.simulator.protocols.ta.*;
import de.persosim.simulator.secstatus.*;
import de.persosim.simulator.tlv.*;
import de.persosim.simulator.utils.Utils;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuxiliaryDateTest {
    private DateAuxObject auxiliary(de.persosim.simulator.protocols.Oid oid, Date date) throws Exception {
        DateAuxObject value = new DateAuxObject(new OidIdentifier(oid), date);
        value.setSecStatus(new SecStatus() {
            @Override public Collection<SecMechanism> getCurrentMechanisms(SecContext context, Collection<Class<? extends SecMechanism>> types) {
                return Arrays.asList(new TerminalAuthenticationMechanism(new byte[0], TerminalType.IS,
                        Collections.emptyList(), null, null, null, Collections.emptyList()),
                        new EffectiveAuthorizationMechanism(new AuthorizationStore()));
            }
        });
        return value;
    }
    private boolean verify(DateAuxObject object, de.persosim.simulator.protocols.Oid oid, String date) throws Exception {
        return object.verify(new AuthenticatedAuxiliaryData(oid, date.getBytes(StandardCharsets.US_ASCII)));
    }
    @Test public void birthAndExpiryBoundaryAndUpdatedDateIgnoreDeviceTimezone() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            for (String[] dates : new String[][] {{"20000229", "20000228", "20000301"}, {"20350715", "20350714", "20350716"}}) {
            Date boundary = Utils.getDate(dates[0], (byte) 0, TimeZone.getTimeZone("Europe/Berlin"));
            for (String zone : new String[] {"UTC", "Europe/Berlin", "Asia/Tokyo", "America/Los_Angeles"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                DateAuxObject birth = auxiliary(AuxOid.id_DateOfBirth, boundary);
                DateAuxObject expiry = auxiliary(AuxOid.id_DateOfExpiry, boundary);
                assertTrue(zone, verify(birth, AuxOid.id_DateOfBirth, dates[0]));
                assertFalse(zone, verify(birth, AuxOid.id_DateOfBirth, dates[1]));
                assertTrue(zone, verify(expiry, AuxOid.id_DateOfExpiry, dates[0]));
                assertFalse(zone, verify(expiry, AuxOid.id_DateOfExpiry, dates[2]));
                birth.setDate(new PrimitiveTlvDataObject(TlvConstants.TAG_53, ("00" + dates[0]).getBytes(StandardCharsets.US_ASCII)));
                assertEquals(zone, boundary, birth.getDate());
            }
            }
        } finally { TimeZone.setDefault(previous); }
    }
}
