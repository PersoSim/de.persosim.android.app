package de.persosim.android.app;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ProfileCatalog {
    public final String file;
    public final String title;
    public final String subtitle;
    public final String details;

    ProfileCatalog(String file, String title, String subtitle, String details) {
        this.file = file;
        this.title = title;
        this.subtitle = subtitle;
        this.details = details;
    }

    private ProfileCatalog(JSONObject value) throws JSONException {
        file = value.getString("file");
        title = value.getString("title");
        subtitle = DocumentType.label(value.optString("documentType")) + " · " + value.getString("subtitle");
        details = value.getString("details");
    }

    public static List<ProfileCatalog> read(Context context) throws IOException, JSONException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = context.getAssets().open("profiles.json")) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        JSONArray values = new JSONArray(output.toString("UTF-8"));
        List<ProfileCatalog> profiles = new ArrayList<>(ProfileStore.catalog(context));
        for (int i = 0; i < values.length(); i++) profiles.add(new ProfileCatalog(values.getJSONObject(i)));
        return profiles;
    }
}
