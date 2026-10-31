package za.co.rc.tv.phone

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import org.json.JSONObject

/** Exercises the actual phone client's Android TLS stack in a separate test process, no phone UI launch. */
class LinkClientCheck : Instrumentation() {
    private lateinit var args: Bundle
    override fun onCreate(arguments: Bundle?) { args = arguments!!; super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val client = LinkClient(args.getString("host")!!, args.getString("port")!!.toInt())
        try {
            check(client.probe() == args.getString("pin"))
            val request = client.request(JSONObject().put("op", "pair").put("code", args.getString("code")).put("name", "Android client check")).getString("request")
            repeat(45) {
                if (client.token == null) {
                    Thread.sleep(1000)
                    val response = client.request(JSONObject().put("op", "poll").put("request", request))
                    if (response.optBoolean("approved")) client.token = response.getString("token")
                }
            }
            check(client.token != null)
            var ready = false
            repeat(15) { if (!ready) { Thread.sleep(1000); ready = runCatching { client.command("HOME"); true }.getOrDefault(false) } }
            check(ready)
            Thread.sleep(200); client.command("SEARCH_TEXT", "YouTube")
            Thread.sleep(200); client.command("BACK")
            client.disconnect()
            result.putString("verification", "Android pinned TLS, TV approval, Home/Search/Back and disconnect passed")
            finish(Activity.RESULT_OK, result)
        } catch (failure: Exception) {
            result.putString("failure", failure.javaClass.simpleName)
            finish(Activity.RESULT_CANCELED, result)
        } finally { client.disconnect() }
    }
}
