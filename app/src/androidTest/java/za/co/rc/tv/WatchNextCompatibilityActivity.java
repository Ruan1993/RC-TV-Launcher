package za.co.rc.tv;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Map;
import java.util.TreeMap;

/** Standalone test-APK UID. Platform Java only: instrumentation dependencies live in the target APK. */
public final class WatchNextCompatibilityActivity extends Activity {
    private static final String PERMISSION = "android.permission.READ_TV_LISTINGS";
    private final CancellationSignal cancellation = new CancellationSignal();
    private TextView report;
    private Thread worker;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32,24,32,24);
        report = new TextView(this);
        report.setText("Read-only Watch Next compatibility check\nThis separate test app requests Android TV-listings permission. It does not change RC TV, Home roles, system packages or watch history. Results contain counts and source packages, not titles or viewing links.");
        layout.addView(report);
        Button check = new Button(this);
        check.setText("Check access once (Android permission required)");
        check.setOnClickListener(v -> {
            check.setEnabled(false);
            if (checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED) inspect();
            else requestPermissions(new String[]{PERMISSION},1);
        });
        layout.addView(check);
        Button back = new Button(this);
        back.setText("Back without changes");
        back.setOnClickListener(v -> finish());
        layout.addView(back);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if (code == 1) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) inspect();
            else report.setText("TV-listings permission was not granted. Cross-app access is not established. No provider query or settings change was made.");
        }
    }

    private void inspect() {
        report.setText("Reading public TV-provider tables with the test app's own permission...");
        worker = new Thread(() -> {
            StringBuilder result = new StringBuilder("Caller: " + getPackageName() + "; READ_TV_LISTINGS granted\n");
            for (String table : new String[]{"watch_next_program","preview_program"}) {
                try {
                    String[] columns = table.equals("watch_next_program")
                        ? new String[]{"_id","package_name","title","intent_uri","poster_art_uri","last_playback_position_millis","duration_millis","watch_next_type"}
                        : new String[]{"_id","package_name","title","intent_uri","poster_art_uri","last_playback_position_millis","duration_millis"};
                    int count=0, continuing=0, titles=0, links=0, artwork=0, progress=0;
                    Map<String,Integer> sources = new TreeMap<>();
                    try (Cursor cursor = getContentResolver().query(Uri.parse("content://android.media.tv/"+table),columns,null,null,null,cancellation)) {
                        if (cursor == null) throw new IllegalStateException("Provider returned no cursor");
                        while (count < 200 && cursor.moveToNext()) {
                            count++;
                            String source = cursor.getString(1);
                            if (source == null) source = "unknown";
                            Integer previous = sources.get(source);
                            sources.put(source,(previous == null ? 0 : previous)+1);
                            if (present(cursor.getString(2))) titles++;
                            if (present(cursor.getString(3))) links++;
                            if (present(cursor.getString(4))) artwork++;
                            if (!cursor.isNull(5) && cursor.getLong(5)>=0 && cursor.getLong(6)>0) progress++;
                            if (table.equals("watch_next_program") && !cursor.isNull(7) && cursor.getInt(7)==0) continuing++;
                        }
                    }
                    result.append("\n").append(table).append(": ").append(count).append(" rows (maximum 200), continue=").append(continuing)
                        .append(", titles=").append(titles).append(", content links=").append(links).append(", artwork=").append(artwork).append(", progress=").append(progress).append("\n");
                    for (Map.Entry<String,Integer> source : sources.entrySet()) result.append(source.getKey()).append(": ").append(source.getValue()).append("\n");
                } catch (Exception e) {
                    String message = String.valueOf(e.getMessage());
                    result.append("\n").append(table).append(": ").append(e.getClass().getSimpleName()).append(": ").append(message.substring(0,Math.min(300,message.length()))).append("\n");
                }
            }
            result.append("\nPreview programs are recommendations, not proof of watch history. Empty results do not prove every installed app lacks integration. No watch history was modified.");
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) { report.setText(result.toString()); Log.i("RcWatchNextCheck",result.toString()); }
            });
        },"RC-watchnext-check");
        worker.start();
    }
    private static boolean present(String value) { return value != null && !value.trim().isEmpty(); }
    @Override public void onDestroy() { cancellation.cancel(); if(worker != null) worker.interrupt(); super.onDestroy(); }
}
