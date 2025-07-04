package com.example.camerainterceptor;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONException;

public class XposedModule implements IXposedHookLoadPackage {
    private static final String TAG = "CameraNetworkInterceptor";
    private static final String TARGET_ENDPOINT = "/camera/useTimeCode";
    
    // Configuration flags
    private static final boolean MODIFY_RESPONSES = true; // Set to false to only log
    private static final long FUTURE_EXPIRED_TIME = 4102444800000L; // Jan 1, 2099 in milliseconds
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Skip system apps and our own module
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") ||
            lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            return;
        }
        
        Log.d(TAG, "🔍 Monitoring network calls in: " + lpparam.packageName);
        XposedBridge.log("CameraInterceptor: 🔍 Monitoring " + lpparam.packageName + " for /camera/useTimeCode calls");
        
        // Hook all major HTTP libraries
        hookHttpURLConnection(lpparam);
        hookOkHttp(lpparam);
        hookRetrofit(lpparam);
        hookApacheHttpClient(lpparam);
        hookVolley(lpparam);
        hookWebView(lpparam);
    }
    
    private void hookHttpURLConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook URL constructor to catch all URL creations
            XposedHelpers.findAndHookConstructor("java.net.URL", lpparam.classLoader,
                String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String urlString = (String) param.args[0];
                        if (urlString != null && urlString.contains(TARGET_ENDPOINT)) {
                            Log.e(TAG, "🎯 TARGET URL DETECTED!");
                            Log.e(TAG, "📍 URL: " + urlString);
                            Log.e(TAG, "📱 Package: " + lpparam.packageName);
                            Log.e(TAG, "🕐 Timestamp: " + System.currentTimeMillis());
                            Log.e(TAG, "🌐 Protocol: " + (urlString.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                            XposedBridge.log("CameraInterceptor: 🚨 FOUND TARGET! " + urlString + " in " + lpparam.packageName);
                        }
                    }
                });
            
            // Hook HttpURLConnection.connect()
            XposedHelpers.findAndHookMethod("java.net.HttpURLConnection", lpparam.classLoader,
                "connect", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                        URL url = connection.getURL();
                        String urlString = url.toString();
                        
                        if (urlString.contains(TARGET_ENDPOINT)) {
                            Log.e(TAG, "🚨 INTERCEPTING CONNECTION!");
                            Log.e(TAG, "📍 Full URL: " + urlString);
                            Log.e(TAG, "🌐 Host: " + url.getHost());
                            Log.e(TAG, "🔢 Port: " + (url.getPort() == -1 ? url.getDefaultPort() : url.getPort()));
                            Log.e(TAG, "📱 Package: " + lpparam.packageName);
                            Log.e(TAG, "🔧 Method: " + connection.getRequestMethod());
                            Log.e(TAG, "📋 Headers: " + getConnectionHeaders(connection));
                            Log.e(TAG, "🔐 Protocol: " + url.getProtocol().toUpperCase());
                            Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                            
                            XposedBridge.log("CameraInterceptor: 🎯 CONNECTING TO: " + urlString + " [" + connection.getRequestMethod() + "]");
                        }
                    }
                });
            
            // Hook getOutputStream to intercept POST data
            XposedHelpers.findAndHookMethod("java.net.HttpURLConnection", lpparam.classLoader,
                "getOutputStream", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                        URL url = connection.getURL();
                        String urlString = url.toString();
                        
                        if (urlString.contains(TARGET_ENDPOINT)) {
                            OutputStream originalStream = (OutputStream) param.getResult();
                            Log.e(TAG, "📤 INTERCEPTING REQUEST DATA!");
                            Log.e(TAG, "📍 URL: " + urlString);
                            Log.e(TAG, "🔧 Method: " + connection.getRequestMethod());
                            
                            // Wrap the output stream to capture data
                            OutputStream wrappedStream = new OutputStreamWrapper(originalStream, urlString, lpparam.packageName);
                            param.setResult(wrappedStream);
                            
                            XposedBridge.log("CameraInterceptor: 📤 Capturing POST data to: " + urlString);
                        }
                    }
                });
            
            // Hook getInputStream to intercept responses
            XposedHelpers.findAndHookMethod("java.net.HttpURLConnection", lpparam.classLoader,
                "getInputStream", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                        URL url = connection.getURL();
                        String urlString = url.toString();
                        
                        if (urlString.contains(TARGET_ENDPOINT)) {
                            InputStream originalStream = (InputStream) param.getResult();
                            Log.e(TAG, "📥 INTERCEPTING RESPONSE!");
                            Log.e(TAG, "📍 URL: " + urlString);
                            Log.e(TAG, "📊 Response Code: " + connection.getResponseCode());
                            Log.e(TAG, "📋 Response Headers: " + connection.getHeaderFields());
                            Log.e(TAG, "📏 Content Length: " + connection.getContentLength());
                            Log.e(TAG, "📄 Content Type: " + connection.getContentType());
                            
                            // Wrap the input stream to capture and potentially modify response data
                            InputStream wrappedStream = new InputStreamWrapper(originalStream, urlString, lpparam.packageName);
                            param.setResult(wrappedStream);
                            
                            XposedBridge.log("CameraInterceptor: 📥 Capturing response from: " + urlString + " [" + connection.getResponseCode() + "]");
                        }
                    }
                });
                
        } catch (Throwable t) {
            Log.e(TAG, "Error hooking HttpURLConnection: " + t.getMessage());
        }
    }
    
    private void hookOkHttp(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook OkHttp Request.Builder.url()
            Class<?> requestBuilderClass = XposedHelpers.findClass("okhttp3.Request$Builder", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(requestBuilderClass, "url", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String url = (String) param.args[0];
                    if (url != null && url.contains(TARGET_ENDPOINT)) {
                        Log.e(TAG, "🚨 OKHTTP TARGET DETECTED!");
                        Log.e(TAG, "📍 URL: " + url);
                        Log.e(TAG, "📱 Package: " + lpparam.packageName);
                        Log.e(TAG, "🌐 Protocol: " + (url.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                        Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                        XposedBridge.log("CameraInterceptor: 🎯 OkHttp building request to: " + url);
                    }
                }
            });
            
            // Hook OkHttp Call.execute()
            Class<?> callClass = XposedHelpers.findClass("okhttp3.Call", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(callClass, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object call = param.thisObject;
                    Object request = XposedHelpers.callMethod(call, "request");
                    Object url = XposedHelpers.callMethod(request, "url");
                    String urlString = url.toString();
                    
                    if (urlString.contains(TARGET_ENDPOINT)) {
                        Object method = XposedHelpers.callMethod(request, "method");
                        Object headers = XposedHelpers.callMethod(request, "headers");
                        
                        Log.e(TAG, "🚨 OKHTTP EXECUTE!");
                        Log.e(TAG, "📍 URL: " + urlString);
                        Log.e(TAG, "🔧 Method: " + method);
                        Log.e(TAG, "📋 Headers: " + headers);
                        Log.e(TAG, "📱 Package: " + lpparam.packageName);
                        Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                        XposedBridge.log("CameraInterceptor: 🎯 OkHttp executing: " + method + " " + urlString);
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object call = param.thisObject;
                    Object request = XposedHelpers.callMethod(call, "request");
                    Object url = XposedHelpers.callMethod(request, "url");
                    String urlString = url.toString();
                    
                    if (urlString.contains(TARGET_ENDPOINT)) {
                        Object response = param.getResult();
                        Object code = XposedHelpers.callMethod(response, "code");
                        Object headers = XposedHelpers.callMethod(response, "headers");
                        
                        Log.e(TAG, "📥 OKHTTP RESPONSE!");
                        Log.e(TAG, "📍 URL: " + urlString);
                        Log.e(TAG, "📊 Response Code: " + code);
                        Log.e(TAG, "📋 Response Headers: " + headers);
                        XposedBridge.log("CameraInterceptor: 📥 OkHttp response: " + code + " from " + urlString);
                        
                        // Try to modify OkHttp response if enabled
                        if (MODIFY_RESPONSES) {
                            try {
                                Object responseBody = XposedHelpers.callMethod(response, "body");
                                if (responseBody != null) {
                                    String bodyString = (String) XposedHelpers.callMethod(responseBody, "string");
                                    String modifiedBody = modifyJsonResponse(bodyString, urlString);
                                    if (!bodyString.equals(modifiedBody)) {
                                        Log.e(TAG, "🔧 MODIFIED OKHTTP RESPONSE!");
                                        Log.e(TAG, "📍 URL: " + urlString);
                                        Log.e(TAG, "📝 Modified JSON: " + modifiedBody);
                                        XposedBridge.log("CameraInterceptor: 🔧 Modified OkHttp response for: " + urlString);
                                    }
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "Could not modify OkHttp response: " + e.getMessage());
                            }
                        }
                    }
                }
            });
            
        } catch (Throwable t) {
            Log.d(TAG, "OkHttp not found in " + lpparam.packageName);
        }
    }
    
    private void hookRetrofit(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Retrofit service method calls
            Class<?> serviceMethodClass = XposedHelpers.findClass("retrofit2.ServiceMethod", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(serviceMethodClass, "invoke", Object[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "🔧 Retrofit service method called in " + lpparam.packageName);
                    Log.d(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                    XposedBridge.log("CameraInterceptor: Retrofit call detected in " + lpparam.packageName);
                }
            });
        } catch (Throwable t) {
            Log.d(TAG, "Retrofit not found in " + lpparam.packageName);
        }
    }
    
    private void hookApacheHttpClient(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Apache HttpClient
            Class<?> httpClientClass = XposedHelpers.findClass("org.apache.http.impl.client.DefaultHttpClient", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(httpClientClass, "execute", "org.apache.http.client.methods.HttpUriRequest", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object request = param.args[0];
                    Object uri = XposedHelpers.callMethod(request, "getURI");
                    String uriString = uri.toString();
                    
                    if (uriString.contains(TARGET_ENDPOINT)) {
                        Object method = XposedHelpers.callMethod(request, "getMethod");
                        Log.e(TAG, "🚨 APACHE HTTP CLIENT TARGET!");
                        Log.e(TAG, "📍 URI: " + uriString);
                        Log.e(TAG, "🔧 Method: " + method);
                        Log.e(TAG, "📱 Package: " + lpparam.packageName);
                        Log.e(TAG, "🌐 Protocol: " + (uriString.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                        Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                        XposedBridge.log("CameraInterceptor: 🎯 Apache HTTP: " + method + " " + uriString);
                    }
                }
            });
        } catch (Throwable t) {
            Log.d(TAG, "Apache HttpClient not found in " + lpparam.packageName);
        }
    }
    
    private void hookVolley(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Volley Request
            Class<?> requestClass = XposedHelpers.findClass("com.android.volley.Request", lpparam.classLoader);
            XposedHelpers.findAndHookConstructor(requestClass, int.class, String.class, "com.android.volley.Response$ErrorListener", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String url = (String) param.args[1];
                    int method = (Integer) param.args[0];
                    if (url != null && url.contains(TARGET_ENDPOINT)) {
                        String methodName = getVolleyMethodName(method);
                        Log.e(TAG, "🚨 VOLLEY TARGET DETECTED!");
                        Log.e(TAG, "📍 URL: " + url);
                        Log.e(TAG, "🔧 Method: " + methodName + " (" + method + ")");
                        Log.e(TAG, "📱 Package: " + lpparam.packageName);
                        Log.e(TAG, "🌐 Protocol: " + (url.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                        Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                        XposedBridge.log("CameraInterceptor: 🎯 Volley request: " + methodName + " " + url);
                    }
                }
            });
        } catch (Throwable t) {
            Log.d(TAG, "Volley not found in " + lpparam.packageName);
        }
    }
    
    private void hookWebView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook WebView loadUrl
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "loadUrl", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String url = (String) param.args[0];
                        if (url != null && url.contains(TARGET_ENDPOINT)) {
                            Log.e(TAG, "🚨 WEBVIEW TARGET DETECTED!");
                            Log.e(TAG, "📍 URL: " + url);
                            Log.e(TAG, "📱 Package: " + lpparam.packageName);
                            Log.e(TAG, "🌐 Protocol: " + (url.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                            Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                            XposedBridge.log("CameraInterceptor: 🎯 WebView loading: " + url);
                        }
                    }
                });
                
            // Hook WebView postUrl for POST requests
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "postUrl", String.class, byte[].class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String url = (String) param.args[0];
                        byte[] postData = (byte[]) param.args[1];
                        if (url != null && url.contains(TARGET_ENDPOINT)) {
                            String postDataString = new String(postData, StandardCharsets.UTF_8);
                            Log.e(TAG, "🚨 WEBVIEW POST TARGET!");
                            Log.e(TAG, "📍 URL: " + url);
                            Log.e(TAG, "📤 POST Data: " + postDataString);
                            Log.e(TAG, "📱 Package: " + lpparam.packageName);
                            Log.e(TAG, "🌐 Protocol: " + (url.startsWith("https") ? "HTTPS ✅" : "HTTP ⚠️"));
                            Log.e(TAG, "🕐 Current Time (seconds): " + (System.currentTimeMillis() / 1000));
                            XposedBridge.log("CameraInterceptor: 🎯 WebView POST: " + url);
                        }
                    }
                });
        } catch (Throwable t) {
            Log.e(TAG, "Error hooking WebView: " + t.getMessage());
        }
    }
    
    private String getConnectionHeaders(HttpURLConnection connection) {
        try {
            Map<String, List<String>> headers = connection.getRequestProperties();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Unable to read headers";
        }
    }
    
    private String getVolleyMethodName(int method) {
        switch (method) {
            case 0: return "GET";
            case 1: return "POST";
            case 2: return "PUT";
            case 3: return "DELETE";
            case 4: return "HEAD";
            case 5: return "OPTIONS";
            case 6: return "TRACE";
            case 7: return "PATCH";
            default: return "UNKNOWN";
        }
    }
    
    // Method to modify JSON response with dynamic timing
    private String modifyJsonResponse(String originalJson, String url) {
        try {
            JSONObject json = new JSONObject(originalJson);
            
            // Check if this looks like a camera/useTimeCode response
            if (json.has("code") || json.has("now") || json.has("expiredTime") || json.has("token")) {
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                
                Log.e(TAG, "🔧 MODIFYING JSON RESPONSE!");
                Log.e(TAG, "📍 URL: " + url);
                Log.e(TAG, "📝 Original: " + originalJson);
                
                // Update timing fields with current time and future expiration
                if (json.has("now")) {
                    json.put("now", currentTimeSeconds);
                    Log.e(TAG, "🕐 Updated 'now' to: " + currentTimeSeconds);
                }
                
                if (json.has("expiredTime")) {
                    json.put("expiredTime", FUTURE_EXPIRED_TIME);
                    Log.e(TAG, "⏰ Updated 'expiredTime' to: " + FUTURE_EXPIRED_TIME + " (2099)");
                }
                
                // Keep remain time high if it exists
                if (json.has("remain")) {
                    json.put("remain", 999999);
                    Log.e(TAG, "⏳ Updated 'remain' to: 999999");
                }
                
                // Ensure success code
                if (json.has("code")) {
                    json.put("code", 200);
                    Log.e(TAG, "✅ Updated 'code' to: 200");
                }
                
                String modifiedJson = json.toString();
                Log.e(TAG, "📝 Modified: " + modifiedJson);
                XposedBridge.log("CameraInterceptor: 🔧 Modified response timing for: " + url);
                
                return modifiedJson;
            }
        } catch (JSONException e) {
            Log.d(TAG, "Not a JSON response or parsing error: " + e.getMessage());
        }
        
        return originalJson; // Return original if not modifiable
    }
    
    // Wrapper class to capture POST data
    private static class OutputStreamWrapper extends OutputStream {
        private final OutputStream originalStream;
        private final ByteArrayOutputStream captureStream;
        private final String url;
        private final String packageName;
        
        public OutputStreamWrapper(OutputStream original, String url, String packageName) {
            this.originalStream = original;
            this.captureStream = new ByteArrayOutputStream();
            this.url = url;
            this.packageName = packageName;
        }
        
        @Override
        public void write(int b) throws IOException {
            originalStream.write(b);
            captureStream.write(b);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            originalStream.write(b);
            captureStream.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            originalStream.write(b, off, len);
            captureStream.write(b, off, len);
        }
        
        @Override
        public void close() throws IOException {
            String postData = captureStream.toString(StandardCharsets.UTF_8.name());
            Log.e(TAG, "📤 CAPTURED POST DATA!");
            Log.e(TAG, "📍 URL: " + url);
            Log.e(TAG, "📱 Package: " + packageName);
            Log.e(TAG, "📄 Data: " + postData);
            Log.e(TAG, "🕐 Timestamp: " + (System.currentTimeMillis() / 1000));
            
            // Try to parse and log JSON structure
            try {
                JSONObject json = new JSONObject(postData);
                Log.e(TAG, "📋 JSON Structure: " + json.toString(2));
            } catch (JSONException e) {
                Log.d(TAG, "POST data is not JSON: " + e.getMessage());
            }
            
            XposedBridge.log("CameraInterceptor: 📤 POST Data captured: " + postData.length() + " bytes to " + url);
            
            originalStream.close();
            captureStream.close();
        }
        
        @Override
        public void flush() throws IOException {
            originalStream.flush();
        }
    }
    
    // Wrapper class to capture and modify response data
    private static class InputStreamWrapper extends InputStream {
        private final InputStream originalStream;
        private final ByteArrayOutputStream captureStream;
        private final String url;
        private final String packageName;
        private boolean captured = false;
        private byte[] modifiedData = null;
        private int modifiedDataIndex = 0;
        
        public InputStreamWrapper(InputStream original, String url, String packageName) {
            this.originalStream = original;
            this.captureStream = new ByteArrayOutputStream();
            this.url = url;
            this.packageName = packageName;
        }
        
        @Override
        public int read() throws IOException {
            // If we have modified data, serve it first
            if (modifiedData != null) {
                if (modifiedDataIndex < modifiedData.length) {
                    return modifiedData[modifiedDataIndex++] & 0xFF;
                } else {
                    return -1; // End of modified data
                }
            }
            
            int b = originalStream.read();
            if (b != -1) {
                captureStream.write(b);
            } else if (!captured) {
                processAndModifyResponse();
            }
            return b;
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            // If we have modified data, serve it
            if (modifiedData != null) {
                int remaining = modifiedData.length - modifiedDataIndex;
                if (remaining <= 0) return -1;
                
                int toRead = Math.min(b.length, remaining);
                System.arraycopy(modifiedData, modifiedDataIndex, b, 0, toRead);
                modifiedDataIndex += toRead;
                return toRead;
            }
            
            int bytesRead = originalStream.read(b);
            if (bytesRead > 0) {
                captureStream.write(b, 0, bytesRead);
            } else if (!captured) {
                processAndModifyResponse();
            }
            return bytesRead;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // If we have modified data, serve it
            if (modifiedData != null) {
                int remaining = modifiedData.length - modifiedDataIndex;
                if (remaining <= 0) return -1;
                
                int toRead = Math.min(len, remaining);
                System.arraycopy(modifiedData, modifiedDataIndex, b, off, toRead);
                modifiedDataIndex += toRead;
                return toRead;
            }
            
            int bytesRead = originalStream.read(b, off, len);
            if (bytesRead > 0) {
                captureStream.write(b, off, bytesRead);
            } else if (!captured) {
                processAndModifyResponse();
            }
            return bytesRead;
        }
        
        @Override
        public void close() throws IOException {
            if (!captured) {
                processAndModifyResponse();
            }
            originalStream.close();
            captureStream.close();
        }
        
        private void processAndModifyResponse() {
            if (captured) return;
            captured = true;
            
            String responseData = captureStream.toString();
            Log.e(TAG, "📥 CAPTURED RESPONSE DATA!");
            Log.e(TAG, "📍 URL: " + url);
            Log.e(TAG, "📱 Package: " + packageName);
            Log.e(TAG, "📄 Response: " + responseData);
            Log.e(TAG, "🕐 Timestamp: " + (System.currentTimeMillis() / 1000));
            
            // Try to parse and log JSON structure
            try {
                JSONObject json = new JSONObject(responseData);
                Log.e(TAG, "📋 JSON Structure: " + json.toString(2));
                
                // Log specific fields if they exist
                if (json.has("code")) Log.e(TAG, "📊 Response Code: " + json.get("code"));
                if (json.has("now")) Log.e(TAG, "🕐 Server Time: " + json.get("now"));
                if (json.has("expiredTime")) Log.e(TAG, "⏰ Expires: " + json.get("expiredTime"));
                if (json.has("token")) Log.e(TAG, "🔑 Token: " + json.get("token"));
                if (json.has("remain")) Log.e(TAG, "⏳ Remaining: " + json.get("remain"));
                
            } catch (JSONException e) {
                Log.d(TAG, "Response is not JSON: " + e.getMessage());
            }
            
            XposedBridge.log("CameraInterceptor: 📥 Response captured: " + responseData.length() + " bytes from " + url);
            
            // Modify response if enabled
            if (MODIFY_RESPONSES) {
                String modifiedResponse = modifyJsonResponse(responseData, url);
                if (!responseData.equals(modifiedResponse)) {
                    modifiedData = modifiedResponse.getBytes(StandardCharsets.UTF_8);
                    modifiedDataIndex = 0;
                    Log.e(TAG, "🔧 RESPONSE WILL BE MODIFIED!");
                    Log.e(TAG, "📝 Modified Response: " + modifiedResponse);
                    XposedBridge.log("CameraInterceptor: 🔧 Response modified for: " + url);
                }
            }
        }
        
        @Override
        public int available() throws IOException {
            if (modifiedData != null) {
                return modifiedData.length - modifiedDataIndex;
            }
            return originalStream.available();
        }
    }
}