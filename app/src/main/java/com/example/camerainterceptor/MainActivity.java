package com.example.camerainterceptor;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView statusText = findViewById(R.id.status_text);
        statusText.setText("🎯 Camera Network Interceptor\n\n" +
                "📡 Target Endpoint:\n" +
                "• /camera/useTimeCode (any IP/domain)\n" +
                "• HTTP & HTTPS supported ✅\n\n" +
                "🔍 Intercepts & Logs:\n" +
                "• POST/GET requests & responses\n" +
                "• Request headers & JSON data\n" +
                "• Response data & status codes\n" +
                "• Dynamic timing information\n\n" +
                "🔧 Response Modification:\n" +
                "• Updates 'now' to current time\n" +
                "• Sets 'expiredTime' to 2099\n" +
                "• Extends 'remain' time\n" +
                "• Ensures success codes\n\n" +
                "🌐 Supported Libraries:\n" +
                "• HttpURLConnection ✅\n" +
                "• OkHttp ✅\n" +
                "• Retrofit ✅\n" +
                "• Apache HttpClient ✅\n" +
                "• Volley ✅\n" +
                "• WebView ✅\n\n" +
                "⚙️ Module Status: Active\n\n" +
                "📋 Setup Instructions:\n" +
                "1. Enable in LSPosed Manager\n" +
                "2. Add target apps to scope\n" +
                "3. Reboot device\n" +
                "4. Monitor logs for intercepts\n\n" +
                "📊 Log Tags:\n" +
                "• CameraNetworkInterceptor\n" +
                "• Look for 🎯 🚨 📤 📥 🔧 emojis\n\n" +
                "💡 Example Targets:\n" +
                "• https://45.12.52.108/camera/useTimeCode\n" +
                "• http://192.168.1.100/camera/useTimeCode\n" +
                "• https://api.domain.com/camera/useTimeCode\n\n" +
                "🕐 Dynamic Timing:\n" +
                "• 'now': Current Unix timestamp\n" +
                "• 'expiredTime': 4102444800000 (2099)\n" +
                "• 'remain': Extended to 999999\n\n" +
                "🔧 Modification: " + (true ? "ENABLED" : "DISABLED"));
    }
}