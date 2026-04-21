package ai.offgridmobile

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class MnnTextModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    init {
        try {
            // Android requires loading the base dependencies first
            System.loadLibrary("MNN")
            System.loadLibrary("MNN_Express")
            System.loadLibrary("llm")
            // Finally, load our custom engine
            System.loadLibrary("mnn_engine_native") 
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("MNN_BRIDGE", "CRITICAL: MNN Engine failed to load! Are the .so files missing? Error: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("MNN_BRIDGE", "Unknown error loading MNN: ${e.message}")
        }
    }

    override fun getName(): String {
        return "MnnTextModule"
    }

    private external fun initMnnEngine(configPath: String): Boolean
    private external fun startGeneration(prompt: String)
    private external fun stopEngineGeneration()

    @ReactMethod
    fun loadModel(modelDir: String, promise: Promise) {
        val configPath = "$modelDir/llm_config.json"
        
        if (initMnnEngine(configPath)) {
            promise.resolve(true)
        } else {
            promise.reject("MNN_ERROR", "Failed to load MNN model in C++")
        }
    }

    @ReactMethod
    fun generateText(prompt: String, promise: Promise) {
        Thread {
            startGeneration(prompt)
            promise.resolve(null)
        }.start()
    }

    @ReactMethod
    fun stopGeneration() {
        stopEngineGeneration()
    }

    fun onToken(token: String) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onMnnToken", token)
    }
}
