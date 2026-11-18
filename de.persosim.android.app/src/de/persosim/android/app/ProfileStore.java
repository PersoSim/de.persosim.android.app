package de.persosim.android.app;

import android.content.Context;
import android.util.AtomicFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

public final class ProfileStore {
    public static boolean isCustom(String file) {
        return file != null && file.matches("custom-[0-9a-f-]{36}\\.json");
    }

    public static boolean isImported(String file) {
        return file != null && file.matches("imported-[0-9a-f-]{36}\\.json");
    }

    public static boolean isOwned(String file) { return isCustom(file) || isImported(file); }

    private static AtomicFile storage(Context context, String file) throws IOException {
        if (!isOwned(file)) throw new IOException("Ungültiger Profilname");
        File directory = new File(context.getFilesDir(), "profiles");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Profilordner konnte nicht erstellt werden");
        return new AtomicFile(new File(directory, file));
    }

    public static ProfileData decode(String json) throws Exception {
        JSONObject object = new JSONObject(json);
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : ProfileData.KEYS) {
            boolean addedField = key.equals("documentType") || key.equals("issuingCountry") || key.equals("communityId");
            if (!addedField || object.has(key)) values.put(key, object.getString(key));
        }
        return new ProfileData(values);
    }

    public static String encode(ProfileData data) throws Exception {
        JSONObject object = new JSONObject();
        for (String key : ProfileData.KEYS) object.put(key, data.get(key));
        return object.toString();
    }

    public static ProfileData read(Context context, String file) throws Exception {
        try (InputStream input = file == null ? context.getAssets().open("editor-template.json") : storage(context, file).openRead()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            ProfileData data = decode(output.toString("UTF-8"));
            data.validate();
            return data;
        }
    }

    public static ProfileData template(Context context, String file, String title) throws Exception {
        ProfileData source;
        if (isCustom(file)) source = read(context, file);
        else {
            try (InputStream input = open(context, file)) {
                source = prepareImport(context, input, file).editorData;
            }
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : ProfileData.KEYS) values.put(key, source.get(key));
        String label = title == null ? source.get("title") : title;
        if (label.length() > 110) label = label.substring(0, Character.isHighSurrogate(label.charAt(109)) ? 109 : 110);
        values.put("title", label + " (Kopie)");
        return new ProfileData(values);
    }

    public static byte[] document(Context context, ProfileData data) throws Exception {
        try (InputStream input = context.getAssets().open("Profile01.perso")) {
            return ProfileDocument.create(input, data);
        }
    }

    public static InputStream open(Context context, String file) throws Exception {
        if (isImported(file)) {
            JSONObject imported = stored(context, file);
            return new ByteArrayInputStream(android.util.Base64.decode(imported.getString("xml"), android.util.Base64.DEFAULT));
        }
        return isCustom(file) ? new ByteArrayInputStream(document(context, read(context, file))) : context.getAssets().open(file);
    }

    public static void save(Context context, String file, ProfileData data) throws Exception {
        if (!isCustom(file)) throw new IOException("Dieses Profil kann nicht bearbeitet werden.");
        write(context, file, encode(data));
    }

    private static void write(Context context, String file, String json) throws Exception {
        byte[] content = json.getBytes("UTF-8");
        AtomicFile target = storage(context, file);
        FileOutputStream output = target.startWrite();
        try {
            output.write(content);
            output.getFD().sync();
            target.finishWrite(output);
        } catch (Exception exception) {
            target.failWrite(output);
            throw exception;
        }
    }

    private static JSONObject stored(Context context, String file) throws Exception {
        try (InputStream input = storage(context, file).openRead()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONObject value = new JSONObject(output.toString("UTF-8"));
            if (value.optBoolean("deleted", false)) throw new IOException("Profil wurde gelöscht.");
            return value;
        }
    }

    public static boolean exists(Context context, String file) {
        if (!isOwned(file)) return false;
        try { stored(context, file); return true; }
        catch (Exception exception) { return false; }
    }

    public static void delete(Context context, String file) throws Exception {
        if (!isOwned(file)) throw new IOException("Originalprofile können nicht gelöscht werden.");
        // The tombstone prevents recovery of a deleted profile after an interrupted cleanup.
        write(context, file, "{\"deleted\":true}");
        storage(context, file).delete();
    }

    public static ProfileImport prepareImport(Context context, InputStream input, String filename) throws Exception {
        Set<String> schema = new HashSet<>();
        try (InputStream allowed = context.getAssets().open("import-schema.txt")) {
            schema.addAll(Arrays.asList(new String(ProfileImport.readLimited(allowed), "UTF-8").split("\\n")));
        }
        return ProfileImport.parse(ProfileImport.readLimited(input), filename, schema);
    }

    public static void saveImport(Context context, String file, ProfileImport imported) throws Exception {
        if (!isImported(file)) throw new IOException("Ungültiger Importname.");
        JSONObject value = new JSONObject();
        value.put("title", imported.title);
        value.put("details", imported.details);
        value.put("documentType", imported.documentType);
        value.put("xml", android.util.Base64.encodeToString(imported.xml, android.util.Base64.NO_WRAP));
        write(context, file, value.toString());
    }

    public static List<ProfileCatalog> catalog(Context context) {
        List<ProfileCatalog> result = new ArrayList<>();
        String[] files = new File(context.getFilesDir(), "profiles").list();
        if (files == null) return result;
        Arrays.sort(files);
        List<String> names = new ArrayList<>();
        for (String file : files) {
            // AtomicFile on older Android releases can leave only the backup after interruption.
            String name = file.endsWith(".bak") ? file.substring(0, file.length() - 4) : file;
            if (isOwned(name) && !names.contains(name)) names.add(name);
        }
        for (String file : names) {
            try {
                JSONObject value = stored(context, file);
                if (isImported(file)) {
                    String type = value.optString("documentType");
                    if (!value.has("documentType")) {
                        try (InputStream input = new ByteArrayInputStream(android.util.Base64.decode(value.getString("xml"), android.util.Base64.DEFAULT))) {
                            type = prepareImport(context, input, file).documentType;
                        } catch (Exception exception) {
                            Diagnostics.failure("Ausweisart konnte nicht gelesen werden: " + file, exception);
                            type = "";
                        }
                    }
                    result.add(new ProfileCatalog(file, value.getString("title"), DocumentType.label(type) + " · Importiert", value.getString("details")));
                } else {
                    ProfileData data = decode(value.toString());
                    data.validate();
                    result.add(new ProfileCatalog(file, data.get("title"), DocumentType.label(data.get("documentType")) + " · " + data.get("birthDate") + " · " + data.get("city"), data.details()));
                }
            } catch (Exception exception) {
                Diagnostics.failure("Eigenes Profil konnte nicht gelesen werden: " + file, exception);
            }
        }
        return result;
    }
}
