package za.co.rc.tv.phone

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import org.json.JSONObject

/** Actual phone TLS/client code against the TV; temporary approval is removed in finally. */
object ReconnectReview {
    fun run(test: Instrumentation, args: Bundle) {
        val result = Bundle(); val host=args.getString("host")!!; val port=args.getString("port")!!.toInt()
        var status=Activity.RESULT_CANCELED
        val first=LinkClient(host,port,args.getString("pin")); var second: LinkClient?=null
        try {
            val ticket=first.request(JSONObject().put("op","pair").put("code",args.getString("code")).put("name","RC review")).getString("request")
            for (attempt in 0 until 60) {
                val reply=first.request(JSONObject().put("op","poll").put("request",ticket))
                if(reply.optBoolean("approved")) { first.token=reply.getString("token"); break }
                Thread.sleep(700)
            }
            check(first.token != null) { "TV approval not received" }
            first.resume(); check(first.status().optBoolean("ok"))
            second=LinkClient(host,port,args.getString("pin")).also { it.token=first.token }
            second.resume(); check(second.status().optBoolean("ok"))
            val stale=runCatching { first.status() }.exceptionOrNull()
            check(stale is LinkRejected && stale.reason == "resume")
            second.command("HOME"); Thread.sleep(150); second.command("BACK")
            result.putString("verification","TV approval, pinned Android client, fresh-client reconnect, stale-lease rejection and Home/Back passed; temporary grant removed")
            status=Activity.RESULT_OK
        } catch(failure: Exception) {
            result.putString("failure",failure.javaClass.simpleName + ": " + failure.message)

        } finally { second?.disconnect(); first.disconnect() }
        // Instrumentation.finish can terminate the process, so cleanup must precede it.
        test.finish(status,result)
    }
}
