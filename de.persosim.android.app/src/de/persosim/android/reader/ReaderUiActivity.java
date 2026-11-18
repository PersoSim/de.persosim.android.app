package de.persosim.android.reader;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ReaderUiActivity extends Activity {
    private static final AtomicLong sequence = new AtomicLong();
    private static volatile PinRequest pending;
    private static volatile long pendingId;
    private long requestId;

    public static final class PinRequest {
        private final CountDownLatch latch = new CountDownLatch(1);
        private byte[] value;
        public synchronized void complete(byte[] result) {
            if (latch.getCount() == 0) return;
            value = result;
            latch.countDown();
        }
        public byte[] await() throws InterruptedException {
            return latch.await(90, TimeUnit.SECONDS) ? value : null;
        }
    }

    public static synchronized long begin(PinRequest result) {
        cancelPending();
        pendingId = sequence.incrementAndGet();
        pending = result;
        return pendingId;
    }
    public static synchronized void cancelPending() {
        if (pending != null) pending.complete(null);
        pending = null;
    }
    public static synchronized void cancel(long id) {
        if (id == pendingId) cancelPending();
    }
    private static synchronized void complete(long id, byte[] value) {
        if (id == pendingId) cancelWith(value);
    }
    private static void cancelWith(byte[] value) {
        if (pending != null) pending.complete(value);
        pending = null;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestId = getIntent().getLongExtra("requestId", -1);
        if (pending == null || requestId != pendingId) { finish(); return; }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText("WLAN-Kartenleser\n" + getIntent().getStringExtra("prompt"));
        title.setTextSize(20);
        layout.addView(title);
        EditText input = new EditText(this);
        input.setHint("Test-PIN / CAN / PUK");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(input);
        Button submit = new Button(this);
        submit.setText("Bestätigen");
        submit.setOnClickListener(view -> {
            complete(requestId, input.getText().toString().getBytes(StandardCharsets.US_ASCII));
            finish();
        });
        layout.addView(submit);
        Button cancel = new Button(this);
        cancel.setText("Abbrechen");
        cancel.setOnClickListener(view -> finish());
        layout.addView(cancel);
        setContentView(layout);
    }
    @Override protected void onDestroy() {
        if (!isChangingConfigurations()) complete(requestId, null);
        super.onDestroy();
    }
}
