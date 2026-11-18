package de.persosim.android.app;

public final class DocumentType {
    public static final String[] EDITOR_CODES = {"ID", "UB", "AR", "AS", "AF"};
    private DocumentType() {}

    public static String label(String code) {
        if (code == null || code.trim().isEmpty()) return "Ausweisart nicht hinterlegt";
        switch (code.trim()) {
            case "ID": return "Personalausweis";
            case "AR":
            case "AS":
            case "AF": return "Aufenthaltstitel";
            case "UB": return "eID-Karte für Unionsbürger";
            case "OA": return "Smart-eID";
            default: return "Ausweisart: " + code;
        }
    }
}
