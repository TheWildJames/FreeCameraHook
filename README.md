# 🎯 Camera Network Interceptor - LSPosed Module

An advanced LSPosed module that intercepts and logs network calls to `/camera/useTimeCode` endpoints from any IP address or domain, with optional response modification capabilities.

## 🚀 Features

- **🔍 Universal Network Monitoring**: Intercepts HTTP/HTTPS calls to `/camera/useTimeCode` from any IP/domain
- **📡 Complete Request/Response Capture**: 
  - Request headers, method, and POST data
  - Response headers, status codes, and JSON data
  - Timing information and package identification
- **🔧 Dynamic Response Modification**: 
  - Updates `now` field to current Unix timestamp
  - Sets `expiredTime` to year 2099 (4102444800000ms)
  - Extends `remain` time to 999999
  - Ensures success response codes
- **🌐 Multi-Protocol Support**: 
  - HttpURLConnection (Java standard)
  - OkHttp (modern Android HTTP client)
  - Retrofit (REST API client)
  - Apache HttpClient (legacy)
  - Volley (Google's HTTP library)
  - WebView requests (in-app browsers)
- **📱 Selective App Monitoring**: Only monitors apps you select in LSPosed scope
- **📋 Comprehensive Logging**: Detailed request/response logging with emojis for easy identification
- **🛡️ Configurable Operation**: Enable/disable response modification

## 📦 Installation

### Automatic Build (GitHub Actions)
1. Download the latest APK from [Releases](../../releases)
2. Install the APK on your rooted device
3. Enable the module in LSPosed Manager
4. Select target apps in the module scope
5. Reboot your device

### Manual Build
```bash
git clone https://github.com/yourusername/camera-interceptor
cd camera-interceptor
./gradlew assembleRelease
```

## ⚙️ Configuration

1. **Install LSPosed Framework** on your rooted device
2. **Install this module** APK
3. **Enable the module** in LSPosed Manager
4. **Select target apps** in the module scope (apps you want to monitor)
5. **Reboot** your device
6. **Check logs** in LSPosed Manager or via ADB logcat

### Response Modification Settings
In `XposedModule.java`, you can configure:
```java
private static final boolean MODIFY_RESPONSES = true; // Enable/disable modification
private static final long FUTURE_EXPIRED_TIME = 4102444800000L; // 2099 timestamp
```

## 📊 Monitoring & Logs

The module provides comprehensive logging for all intercepted calls:

### Log Categories
- 🎯 **Target Detection**: When `/camera/useTimeCode` URLs are found
- 🚨 **Connection Intercepts**: When connections are established to target endpoints
- 📤 **Request Data**: POST data and headers being sent
- 📥 **Response Data**: JSON responses and status codes
- 🔧 **Response Modification**: When responses are dynamically modified

### Example Log Output
```
CameraInterceptor: 🚨 FOUND TARGET! https://45.12.52.108/camera/useTimeCode in com.example.app
CameraInterceptor: 🎯 CONNECTING TO: https://45.12.52.108/camera/useTimeCode [POST]
CameraInterceptor: 📤 POST Data captured: 156 bytes
CameraInterceptor: 📥 Response captured: 245 bytes
CameraInterceptor: 🔧 Modified response timing for: https://45.12.52.108/camera/useTimeCode
```

### Detailed Response Modification
```
📥 CAPTURED RESPONSE DATA!
📍 URL: https://45.12.52.108/camera/useTimeCode
📄 Response: {"code":200,"ns":100,"remain":1799,"now":79041735,"expiredTime":1751505935158,"token":"Q3QA35NU6XA"}
🔧 MODIFYING JSON RESPONSE!
🕐 Updated 'now' to: 1704267735
⏰ Updated 'expiredTime' to: 4102444800000 (2099)
⏳ Updated 'remain' to: 999999
✅ Updated 'code' to: 200
📝 Modified: {"code":200,"ns":100,"remain":999999,"now":1704267735,"expiredTime":4102444800000,"token":"Q3QA35NU6XA"}
```

### Viewing Logs
```bash
# Via ADB
adb logcat | grep CameraNetworkInterceptor

# Or in LSPosed Manager
LSPosed Manager > Logs > Search for "CameraInterceptor"
```

## 🔧 Supported Libraries & Detection

| Library | Status | Hook Points | Detection Method |
|---------|--------|-------------|------------------|
| HttpURLConnection | ✅ | URL creation, connect(), streams | URL constructor, connection methods |
| OkHttp | ✅ | Request builder, call execution | Request.Builder.url(), Call.execute() |
| Retrofit | ✅ | Service method calls | ServiceMethod.invoke() |
| Apache HttpClient | ✅ | Request execution | DefaultHttpClient.execute() |
| Volley | ✅ | Request creation | Request constructor |
| WebView | ✅ | loadUrl(), postUrl() | WebView URL loading methods |

## 📱 Target Endpoint Examples

The module will intercept calls to `/camera/useTimeCode` on any domain or IP:

- ✅ `https://45.12.52.108/camera/useTimeCode`
- ✅ `http://192.168.1.100/camera/useTimeCode`
- ✅ `https://api.example.com/camera/useTimeCode`
- ✅ `http://10.0.0.50:8080/camera/useTimeCode`
- ✅ `https://subdomain.domain.com/camera/useTimeCode`

## 🔧 Response Modification Details

When enabled, the module automatically modifies JSON responses containing timing fields:

### Original Response Example
```json
{
  "code": 200,
  "ns": 100,
  "remain": 1799,
  "now": 79041735,
  "expiredTime": 1751505935158,
  "token": "Q3QA35NU6XA"
}
```

### Modified Response
```json
{
  "code": 200,
  "ns": 100,
  "remain": 999999,
  "now": 1704267735,
  "expiredTime": 4102444800000,
  "token": "Q3QA35NU6XA"
}
```

### Modification Rules
- **`now`**: Updated to current Unix timestamp (seconds)
- **`expiredTime`**: Set to 4102444800000 (January 1, 2099)
- **`remain`**: Extended to 999999 for maximum time
- **`code`**: Ensured to be 200 for success
- **Other fields**: Preserved unchanged

## 🛠️ Development

### Building
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
```

### Testing
1. Install on device with LSPosed
2. Enable module and select test app
3. Trigger network calls in test app to `/camera/useTimeCode`
4. Check logcat for intercepts and modifications

### GitHub Actions
The repository includes automated building:
- Builds on push to main/master
- Creates releases with APK artifacts
- Supports both debug and release builds

## ⚠️ Important Notes

- **Root Required**: Needs LSPosed framework installed
- **Scope Selection**: Only monitors apps you explicitly add to module scope
- **Performance**: Minimal impact, hooks are only active when target endpoint is detected
- **Privacy**: All data stays on device, no external transmission
- **Legal**: Use only on apps you own or have permission to monitor
- **Modification**: Response modification can be disabled by setting `MODIFY_RESPONSES = false`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is for educational and security research purposes. Use responsibly and in accordance with applicable laws.

## 🔗 Links

- [LSPosed Framework](https://github.com/LSPosed/LSPosed)
- [Xposed API Documentation](https://api.xposed.info/)
- [Android Security Research](https://source.android.com/security)