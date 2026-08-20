package com.notificationbridge.app

import android.app.Notification
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BridgeListener : NotificationListenerService() {

 private val client = OkHttpClient.Builder()
  .connectTimeout(15, TimeUnit.SECONDS)
  .readTimeout(20, TimeUnit.SECONDS)
  .build()

 override fun onListenerConnected() {
  super.onListenerConnected()
  log("✓ خدمة قراءة الإشعارات متصلة")
 }

 override fun onNotificationPosted(s: StatusBarNotification) {

  val p = getSharedPreferences("bridge", MODE_PRIVATE)

  val chosen = p.getStringSet("apps", emptySet()) ?: emptySet()

  // تجاهل الإشعارات القادمة من تطبيق غير محدد
  if (!chosen.contains(s.packageName)) return

  val url = p.getString("server", "") ?: ""
  val api = p.getString("key", "") ?: ""

  val extras = s.notification.extras

  val title = extras
   .getCharSequence(Notification.EXTRA_TITLE)
   ?.toString()
   .orEmpty()

  val text = (
          extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
           ?: extras.getCharSequence(Notification.EXTRA_TEXT)
          )
   ?.toString()
   .orEmpty()

  // الحصول على اسم التطبيق الحقيقي
  val appName = try {
   packageManager.getApplicationLabel(
    packageManager.getApplicationInfo(s.packageName, 0)
   ).toString()
  } catch (e: Exception) {
   s.packageName
  }

  // حفظ آخر إشعار تم التقاطه
  p.edit()
   .putString(
    "last_notification",
    "$appName • ${if (title.isNotBlank()) title else text.take(60)}"
   )
   .apply()

  // فحص رابط السيرفر
  if (!url.startsWith("https://")) {
   log("✗ تم التقاط إشعار $appName لكن رابط السيرفر غير صالح")
   return
  }

  // فحص API Key
  if (api.isBlank()) {
   log("✗ تم التقاط إشعار $appName لكن API Key فارغ")
   return
  }

  val deviceId = Settings.Secure.getString(
   contentResolver,
   Settings.Secure.ANDROID_ID
  ) ?: "android"

  // تجهيز البيانات المرسلة للسيرفر
  val json = JSONObject()
   .put("device_id", deviceId)
   .put("app_package", s.packageName)
   .put("app_name", appName)
   .put("title", title)
   .put("message", text)
   .put("notification_key", s.key)
   .put(
    "sent_at",
    SimpleDateFormat(
     "yyyy-MM-dd HH:mm:ss",
     Locale.US
    ).format(Date(s.postTime))
   )

  val body = json.toString()
   .toRequestBody(
    "application/json; charset=utf-8".toMediaType()
   )

  val request = Request.Builder()
   .url(url)
   .header("X-API-Key", api)
   .post(body)
   .build()

  client.newCall(request).enqueue(object : Callback {

   override fun onFailure(call: Call, e: IOException) {
    log(
     "✗ فشل إرسال إشعار $appName: ${
      e.localizedMessage ?: "Network error"
     }"
    )
   }

   override fun onResponse(call: Call, response: Response) {

    val responseText = response.body?.string().orEmpty()

    if (response.isSuccessful) {
     log(
      "✓ تم إرسال إشعار $appName • HTTP ${response.code}"
     )
    } else {
     log(
      "✗ فشل إرسال $appName • HTTP ${response.code} • ${
       responseText.take(100)
      }"
     )
    }

    response.close()
   }
  })
 }

 private fun log(value: String) {
  getSharedPreferences("bridge", MODE_PRIVATE)
   .edit()
   .putString("last_result", value)
   .apply()
 }
}