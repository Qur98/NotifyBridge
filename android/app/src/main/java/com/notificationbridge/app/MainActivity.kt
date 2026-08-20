package com.notificationbridge.app

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var appsBox: LinearLayout
    private lateinit var countView: TextView
    private lateinit var statusView: TextView
    private lateinit var lastResult: TextView

    private val rows = mutableListOf<Pair<String, View>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val preferences = getSharedPreferences("bridge", MODE_PRIVATE)

        val server = findViewById<EditText>(R.id.server)
        val key = findViewById<EditText>(R.id.key)

        appsBox = findViewById(R.id.apps)
        countView = findViewById(R.id.selectedCount)
        statusView = findViewById(R.id.status)
        lastResult = findViewById(R.id.lastResult)

        // تحميل الإعدادات المحفوظة
        server.setText(
            preferences.getString("server", "")
        )

        key.setText(
            preferences.getString("key", "")
        )

        // حفظ إعدادات السيرفر
        findViewById<Button>(R.id.save).setOnClickListener {

            preferences.edit()
                .putString(
                    "server",
                    server.text.toString().trim()
                )
                .putString(
                    "key",
                    key.text.toString().trim()
                )
                .apply()

            Toast.makeText(
                this,
                "تم حفظ إعدادات الاتصال",
                Toast.LENGTH_SHORT
            ).show()

            refreshStatus()
        }

        // فتح إعدادات صلاحية قراءة الإشعارات
        findViewById<Button>(R.id.access).setOnClickListener {

            startActivity(
                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )
        }

        // اختبار الاتصال بالسيرفر
        findViewById<Button>(R.id.test).setOnClickListener {

            testServer(
                server.text.toString().trim(),
                key.text.toString().trim()
            )
        }

        // البحث في التطبيقات
        findViewById<EditText>(R.id.search)
            .addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        filter(
                            s?.toString().orEmpty()
                        )
                    }

                    override fun afterTextChanged(
                        editable: Editable?
                    ) {
                    }
                }
            )

        // رابط Facebook
        findViewById<ImageButton>(R.id.facebook)
            .setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/elqdes")))
            }

        findViewById<ImageButton>(R.id.instagram)
            .setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/elqdes/")))
            }

        loadApps()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()

        if (::statusView.isInitialized) {
            refreshStatus()
        }
    }

    /**
     * فحص صلاحية Notification Listener
     */
    private fun enabled(): Boolean {

        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return flat
            .split(":")
            .any {

                ComponentName
                    .unflattenFromString(it)
                    ?.packageName == packageName
            }
    }

    /**
     * تحديث حالة التطبيق
     */
    private fun refreshStatus() {

        val preferences =
            getSharedPreferences(
                "bridge",
                MODE_PRIVATE
            )

        val permissionEnabled = enabled()

        statusView.text =
            if (permissionEnabled) {
                "✓ صلاحية قراءة الإشعارات مفعّلة"
            } else {
                "⚠ صلاحية قراءة الإشعارات غير مفعّلة"
            }

        statusView.setTextColor(
            Color.parseColor(
                if (permissionEnabled) {
                    "#1DBD78"
                } else {
                    "#E14C64"
                }
            )
        )

        val lastSendResult =
            preferences.getString(
                "last_result",
                "لا توجد عملية إرسال مسجلة بعد"
            ) ?: ""

        val lastNotification =
            preferences.getString(
                "last_notification",
                ""
            ) ?: ""

        lastResult.text =
            if (lastNotification.isBlank()) {

                lastSendResult

            } else {

                "$lastSendResult\nآخر إشعار: $lastNotification"
            }

        updateCount()
    }

    /**
     * تحميل التطبيقات المثبتة
     */
    private fun loadApps() {

        val preferences =
            getSharedPreferences(
                "bridge",
                MODE_PRIVATE
            )

        val selected =
            preferences
                .getStringSet(
                    "apps",
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        val installedApps =
            packageManager
                .getInstalledApplications(0)
                .filter {

                    packageManager
                        .getLaunchIntentForPackage(
                            it.packageName
                        ) != null &&
                            it.packageName != packageName
                }
                .sortedBy {

                    packageManager
                        .getApplicationLabel(it)
                        .toString()
                        .lowercase()
                }

        installedApps.forEach { appInfo ->

            val appName =
                packageManager
                    .getApplicationLabel(appInfo)
                    .toString()

            val row =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        10,
                        12,
                        10,
                        12
                    )

                    background =
                        getDrawable(
                            R.drawable.bg_panel
                        )
                }

            val icon =
                ImageView(this).apply {

                    setImageDrawable(
                        packageManager
                            .getApplicationIcon(
                                appInfo
                            )
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            52,
                            52
                        ).apply {
                            marginEnd = 14
                        }
                }

            val label =
                TextView(this).apply {

                    text = appName

                    textSize = 16f

                    setTextColor(
                        Color.parseColor(
                            "#163A54"
                        )
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            val checkBox =
                CheckBox(this).apply {

                    isChecked =
                        selected.contains(
                            appInfo.packageName
                        )

                    buttonTintList =
                        android.content.res.ColorStateList
                            .valueOf(
                                Color.parseColor(
                                    "#19AEE8"
                                )
                            )
                }

            checkBox.setOnCheckedChangeListener { _, checked ->

                val apps =
                    preferences
                        .getStringSet(
                            "apps",
                            emptySet()
                        )
                        ?.toMutableSet()
                        ?: mutableSetOf()

                if (checked) {

                    apps.add(
                        appInfo.packageName
                    )

                } else {

                    apps.remove(
                        appInfo.packageName
                    )
                }

                preferences
                    .edit()
                    .putStringSet(
                        "apps",
                        apps
                    )
                    .apply()

                updateCount()
            }

            row.addView(icon)
            row.addView(label)
            row.addView(checkBox)

            appsBox.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            )

            rows.add(
                appName.lowercase() to row
            )
        }

        updateCount()
    }

    /**
     * فلترة التطبيقات
     */
    private fun filter(query: String) {

        val search =
            query
                .trim()
                .lowercase()

        rows.forEach {

            it.second.visibility =
                if (
                    search.isBlank() ||
                    it.first.contains(search)
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    /**
     * تحديث عدد التطبيقات المحددة
     */
    private fun updateCount() {

        if (!::countView.isInitialized) {
            return
        }

        val count =
            getSharedPreferences(
                "bridge",
                MODE_PRIVATE
            )
                .getStringSet(
                    "apps",
                    emptySet()
                )
                ?.size
                ?: 0

        countView.text =
            "$count تطبيق محدد"
    }

    /**
     * اختبار الاتصال بالسيرفر
     */
    private fun testServer(
        url: String,
        apiKey: String
    ) {

        if (
            !url.startsWith("https://") ||
            apiKey.isBlank()
        ) {

            Toast.makeText(
                this,
                "أدخل رابط HTTPS ومفتاح API أولاً",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        lastResult.text =
            "جاري اختبار الاتصال..."

        val deviceId =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "android"

        val json =
            JSONObject()
                .put(
                    "device_id",
                    deviceId
                )
                .put(
                    "app_package",
                    packageName
                )
                .put(
                    "app_name",
                    "NotifyBridge"
                )
                .put(
                    "title",
                    "اختبار اتصال"
                )
                .put(
                    "message",
                    "تم إرسال اختبار من التطبيق"
                )
                .put(
                    "notification_key",
                    "test-${System.currentTimeMillis()}"
                )
                .put(
                    "sent_at",
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                    ).format(Date())
                )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaType()
                )

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "X-API-Key",
                    apiKey
                )
                .post(body)
                .build()

        client
            .newCall(request)
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        saveResult(
                            "✗ فشل الاتصال: ${
                                e.localizedMessage
                                    ?: "Network error"
                            }"
                        )
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val responseText =
                            response.body
                                ?.string()
                                .orEmpty()

                        // OkHttp 4.x:
                        // OkHttp 4.x response code property
                        val httpCode =
                            response.code

                        if (response.isSuccessful) {

                            saveResult(
                                "✓ اختبار ناجح • HTTP $httpCode"
                            )

                        } else {

                            saveResult(
                                "✗ السيرفر رفض الطلب • HTTP $httpCode • ${
                                    responseText.take(200)
                                }"
                            )
                        }

                        response.close()
                    }
                }
            )
    }

    /**
     * حفظ وإظهار آخر نتيجة
     */
    private fun saveResult(
        value: String
    ) {

        getSharedPreferences(
            "bridge",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "last_result",
                value
            )
            .apply()

        runOnUiThread {

            lastResult.text =
                value

            Toast.makeText(
                this,
                value,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}