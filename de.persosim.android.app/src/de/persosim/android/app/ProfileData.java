package de.persosim.android.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ProfileData {
    public static final String[] KEYS = {"title", "documentType", "issuingCountry", "documentNumber", "givenName", "familyName",
            "birthName", "birthDate", "birthPlace", "nationality", "expiryDate", "sex",
            "artisticName", "academicTitle", "street", "city", "postalCode", "country", "communityId", "pin", "can", "puk"};
    public static final String[] LABELS = {"Profiltitel", "Ausweisart", "Ausstellender Staat (D, AUT, …)", "Ausweisnummer (9 Zeichen)", "Vorname", "Nachname",
            "Geburtsname", "Geburtsdatum (TT.MM.JJJJ)", "Geburtsort", "Staatsangehörigkeit (D, AUT, …)",
            "Gültig bis (TT.MM.JJJJ)", "Geschlecht (M, F oder X)", "Ordens- oder Künstlername", "Doktorgrad",
            "Straße und Hausnummer", "Ort", "Postleitzahl", "Land (D, AUT, …)", "Gemeindekennziffer (14 Ziffern)", "PIN (6 Ziffern)",
            "CAN (6 Ziffern)", "PUK (10 Ziffern)"};
    private final Map<String, String> values = new LinkedHashMap<>();

    public ProfileData(Map<String, String> input) {
        for (String key : KEYS) values.put(key, input.containsKey(key) ? input.get(key) : "");
        if (!input.containsKey("documentType")) values.put("documentType", "ID");
        if (!input.containsKey("issuingCountry")) values.put("issuingCountry", "D");
        if (!input.containsKey("communityId")) values.put("communityId", "02760503150000");
    }

    public String get(String key) { return values.get(key); }

    public static final class InvalidField extends IllegalArgumentException {
        public final String key;
        InvalidField(String key, String message) { super(message); this.key = key; }
    }

    public void validate() {
        for (String key : KEYS) {
            String value = get(key).trim();
            values.put(key, value);
            if (value.isEmpty() && !key.equals("birthName") && !key.equals("artisticName") && !key.equals("academicTitle")) {
                throw new InvalidField(key, "Bitte dieses Feld ausfüllen.");
            }
            if (value.length() > 120) throw new InvalidField(key, "Bitte höchstens 120 Zeichen eingeben.");
            for (int i = 0; i < value.length();) {
                int code = value.codePointAt(i);
                if (code < 32 || (code >= 127 && code <= 159) || (code >= 0xD800 && code <= 0xDFFF)
                        || code == 0xFFFE || code == 0xFFFF) {
                    throw new InvalidField(key, "Dieses Zeichen kann nicht gespeichert werden.");
                }
                i += Character.charCount(code);
            }
        }
        for (String key : new String[]{"documentNumber", "nationality", "country", "sex", "documentType", "issuingCountry"}) {
            values.put(key, get(key).toUpperCase(Locale.ROOT));
        }
        pattern("documentNumber", "[A-Z0-9]{9}", "Bitte genau 9 Buchstaben oder Ziffern eingeben.");
        pattern("documentType", "ID|UB|AR|AS|AF", "Bitte eine unterstützte Ausweisart auswählen.");
        pattern("nationality", "[A-Z]{1,3}", "Bitte einen Ländercode mit 1 bis 3 Buchstaben eingeben.");
        pattern("country", "[A-Z]{1,3}", "Bitte einen Ländercode mit 1 bis 3 Buchstaben eingeben.");
        pattern("issuingCountry", "[A-Z]{1,3}", "Bitte einen Ländercode mit 1 bis 3 Buchstaben eingeben.");
        pattern("communityId", "[0-9]{14}", "Bitte genau 14 Ziffern für die Wohnortprüfung eingeben.");
        pattern("sex", "[MFX]", "Bitte M, F oder X eingeben.");
        pattern("postalCode", "[A-Za-z0-9 '()+,./:=?-]{1,20}", "Bitte eine Postleitzahl mit höchstens 20 lateinischen Zeichen eingeben.");
        pattern("pin", "[0-9]{6}", "Die PIN muss genau 6 Ziffern enthalten.");
        pattern("can", "[0-9]{6}", "Die CAN muss genau 6 Ziffern enthalten.");
        pattern("puk", "[0-9]{10}", "Die PUK muss genau 10 Ziffern enthalten.");
        for (String key : new String[]{"birthDate", "expiryDate"}) {
            pattern(key, "[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}", "Bitte ein gültiges Datum als TT.MM.JJJJ eingeben.");
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
            format.setLenient(false);
            try { format.parse(get(key)); }
            catch (ParseException exception) { throw new InvalidField(key, "Dieses Datum gibt es nicht."); }
        }
    }

    private void pattern(String key, String expression, String message) {
        if (!get(key).matches(expression)) throw new InvalidField(key, message);
    }

    public String compactDate(String key) {
        String value = get(key);
        return value.substring(6, 10) + value.substring(3, 5) + value.substring(0, 2);
    }

    public String details() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals("street") || KEYS[i].equals("pin")) result.append('\n');
            String value = KEYS[i].equals("documentType") ? DocumentType.label(get(KEYS[i])) + " (" + get(KEYS[i]) + ")" : get(KEYS[i]);
            result.append(LABELS[i]).append(": ").append(value.isEmpty() ? "Nicht hinterlegt" : value).append('\n');
        }
        return result.toString();
    }
}
