package com.example.firealarm
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvMainStatus: TextView
    private lateinit var valueTemp: TextView
    private lateinit var valueHum: TextView
    private lateinit var valueFlame: TextView
    private lateinit var valueMQ135: TextView
    private lateinit var valueMQ2: TextView
    private lateinit var valueMQ6: TextView
    private lateinit var valueLDR: TextView

    private lateinit var statusTemp: TextView
    private lateinit var statusHum: TextView
    private lateinit var statusFlame: TextView
    private lateinit var statusMQ135: TextView
    private lateinit var statusMQ2: TextView
    private lateinit var statusMQ6: TextView
    private lateinit var statusLDR: TextView

    private var hasNotified = false
    private val CHANNEL_ID = "alert_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        requestNotificationPermission()

        tvMainStatus = findViewById(R.id.tvMainStatus)

        valueTemp = findViewById(R.id.valueTemp)
        valueHum = findViewById(R.id.valueHum)
        valueFlame = findViewById(R.id.valueFlame)
        valueMQ135 = findViewById(R.id.valueMQ135)
        valueMQ2 = findViewById(R.id.valueMQ2)
        valueMQ6 = findViewById(R.id.valueMQ6)
        valueLDR = findViewById(R.id.valueLDR)

        statusTemp = findViewById(R.id.statusTemp)
        statusHum = findViewById(R.id.statusHum)
        statusFlame = findViewById(R.id.statusFlame)
        statusMQ135 = findViewById(R.id.statusMQ135)
        statusMQ2 = findViewById(R.id.statusMQ2)
        statusMQ6 = findViewById(R.id.statusMQ6)
        statusLDR = findViewById(R.id.statusLDR)

        val rootRef = FirebaseDatabase.getInstance().reference

        rootRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val monitoring = snapshot.child("monitoring")
                val threshold = snapshot.child("threshold")

                val temp = monitoring.child("suhu").getValue(Int::class.java) ?: 0
                val hum = monitoring.child("humidity").getValue(Int::class.java) ?: 0
                val flame = monitoring.child("flame").getValue(Int::class.java) ?: 1
                val mq135 = monitoring.child("mq135").getValue(Int::class.java) ?: 0
                val mq2 = monitoring.child("mq2").getValue(Int::class.java) ?: 0
                val mq6 = monitoring.child("mq6").getValue(Int::class.java) ?: 0
                val ldr = monitoring.child("ldr").getValue(Int::class.java) ?: 0

                val thTemp = threshold.child("suhu").getValue(Int::class.java) ?: 50
                val thHum = threshold.child("humidity").getValue(Int::class.java) ?: 85
                val thFlame = threshold.child("flame").getValue(Int::class.java) ?: 1
                val thMQ135 = threshold.child("mq135").getValue(Int::class.java) ?: 300
                val thMQ2 = threshold.child("mq2").getValue(Int::class.java) ?: 300
                val thMQ6 = threshold.child("mq6").getValue(Int::class.java) ?: 300
                val thLDR = threshold.child("ldr").getValue(Int::class.java) ?: 1

                setSensorValue(valueTemp, statusTemp, temp, thTemp)
                setSensorValue(valueHum, statusHum, hum, thHum)
                setSensorValue(valueFlame, statusFlame, flame, thFlame, isFlameOrLdr = true)
                setSensorValue(valueMQ135, statusMQ135, mq135, thMQ135)
                setSensorValue(valueMQ2, statusMQ2, mq2, thMQ2)
                setSensorValue(valueMQ6, statusMQ6, mq6, thMQ6)
                setSensorValue(valueLDR, statusLDR, ldr, thLDR, isFlameOrLdr = true)

                val isDanger = temp > thTemp || hum > thHum || flame <= thFlame || mq135 > thMQ135 || mq2 > thMQ2 || mq6 > thMQ6
                if (isDanger) {
                    tvMainStatus.text = "Status: Bahaya"
                    tvMainStatus.setBackgroundColor(Color.RED)
                    if (!hasNotified) {
                        showNotification("Peringatan Kebakaran!", "Status sensor menunjukkan bahaya.")
                        hasNotified = true
                    }
                } else {
                    tvMainStatus.text = "Status: Aman"
                    tvMainStatus.setBackgroundColor(Color.parseColor("#2E7D32"))
                    hasNotified = false
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setSensorValue(valueView: TextView, statusView: TextView, value: Int, threshold: Int, isFlameOrLdr: Boolean = false) {
        valueView.text = value.toString()
        val isDanger = if (isFlameOrLdr) (value <= threshold) else (value > threshold)
        statusView.text = if (isDanger) "Bahaya" else "Normal"
        statusView.setTextColor(if (isDanger) Color.RED else Color.parseColor("#2E7D32"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alert Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk kebakaran atau bahaya"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable._b5300a8b03638984b26cc5662fb691a)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                notify(1, builder.build())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        }
    }
}
