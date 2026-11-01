package za.co.rc.tv

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Standalone test-APK UID, not instrumentation/ADB shell identity. No writes to the TV provider. */
class WatchNextCompatibilityActivity : Activity() {
    private lateinit var report: TextView
    private val cancellation = CancellationSignal()
    private var worker: Thread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,24,32,24) }
        report = TextView(this).apply { text="Read-only Watch Next compatibility check\nThis separate test app requests Android TV-listings permission. It does not change RC TV, Home roles, system packages or watch history. Results contain counts and source packages, not titles or viewing links." }
        layout.addView(report)
        layout.addView(Button(this).apply {
            text="Check access once (Android permission required)"
            setOnClickListener {
                isEnabled=false
                if (checkSelfPermission(PERMISSION)==PackageManager.PERMISSION_GRANTED) inspect()
                else requestPermissions(arrayOf(PERMISSION),1)
            }
        })
        layout.addView(Button(this).apply { text="Back without changes"; setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==1) {
            if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) inspect()
            else report.text="TV-listings permission was not granted. Cross-app access is not established. No provider query or settings change was made."
        }
    }
    private fun inspect() {
        report.text="Reading public TV-provider tables with the test app’s own permission…"
        worker=Thread({
            val result=StringBuilder("Caller: $packageName; READ_TV_LISTINGS granted\n")
            for(table in listOf("watch_next_program","preview_program")) {
                try {
                    val columns=listOf("_id","package_name","title","intent_uri","poster_art_uri","last_playback_position_millis","duration_millis") +
                        if(table=="watch_next_program") listOf("watch_next_type") else emptyList()
                    var count=0; var continuing=0; var titles=0; var links=0; var artwork=0; var progress=0
                    val sources=sortedMapOf<String,Int>()
                    contentResolver.query(Uri.parse("content://android.media.tv/$table"),columns.toTypedArray(),null,null,null,cancellation)?.use { cursor ->
                        while(count<200 && cursor.moveToNext()) {
                            count++
                            val source=cursor.getString(1).orEmpty()
                            sources[source]=(sources[source] ?: 0)+1
                            if(!cursor.getString(2).isNullOrBlank()) titles++
                            if(!cursor.getString(3).isNullOrBlank()) links++
                            if(!cursor.getString(4).isNullOrBlank()) artwork++
                            if(!cursor.isNull(5) && cursor.getLong(5)>=0 && cursor.getLong(6)>0) progress++
                            if(table=="watch_next_program" && !cursor.isNull(7) && cursor.getInt(7)==0) continuing++
                        }
                    } ?: error("Provider returned no cursor")
                    result.append("\n$table: $count rows (maximum 200), continue=$continuing, titles=$titles, content links=$links, artwork=$artwork, progress=$progress\n")
                    sources.forEach { (source,n) -> result.append("$source: $n\n") }
                } catch(e: Exception) { result.append("\n$table: ${e.javaClass.simpleName}: ${e.message?.take(300)}\n") }
            }
            result.append("\nPreview programs are recommendations, not proof of watch history. Empty results do not prove every installed app lacks integration. No watch history was modified.")
            runOnUiThread { if(!isFinishing && !isDestroyed) { report.text=result; android.util.Log.i("RcWatchNextCheck",result.toString()) } }
        },"RC-watchnext-check").also { it.start() }
    }
    override fun onDestroy() { cancellation.cancel(); worker?.interrupt(); super.onDestroy() }
    companion object { private const val PERMISSION="android.permission.READ_TV_LISTINGS" }
}
