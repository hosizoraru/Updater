package top.yukonga.update.logic.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import top.yukonga.update.logic.utils.CryptoUtils.miuiDecrypt
import top.yukonga.update.logic.utils.CryptoUtils.miuiEncrypt
import top.yukonga.update.logic.utils.FileUtils.readFile

object InfoUtils {

    private const val miuiUrl = "https://update.miui.com/updates/miotaV3.php"
    private var securityKey = "miuiotavalided11"

    private fun generateJson(device: String, version: String, android: String, userId: String): String {
        val data = mutableMapOf<String, Any>()
        data["id"] = userId
        data["c"] = android
        data["d"] = device
        data["f"] = "1"
        data["ov"] = version
        data["l"] = if (!device.contains("_global")) "zh_CN" else "en_US"
        data["r"] = if (!device.contains("_global")) "CN" else "GL"
        data["v"] = "miui-${version.replace("OS1", "V816")}"
        data["options"] = mutableMapOf<String, Any>().also {
            it["ab"] = "1"
            it["cv"] = version.replace("OS1", "V816")
        }
        return Gson().toJson(data)
    }

    suspend fun getRomInfo(context: Context, codename: String, romVersion: String, androidVersion: String): String {
        var userId = ""
        var securityKey = securityKey.toByteArray(Charsets.UTF_8)
        var serviceToken = ""
        var port = "1"
        val cookiesFile = readFile(context, "cookies.json")
//        if (cookiesFile.isNotEmpty()) {
//            val cookies = Gson().fromJson(cookiesFile, Map::class.java)
//            userId = cookies["userId"] as String
//            securityKey = Base64.getMimeDecoder().decode((cookies["ssecurity"] as String))
//            serviceToken = cookies["serviceToken"] as String
//            port = "2"
//        }
//        Log.d("InfoUtils", "userId: $userId")
//        Log.d("InfoUtils", "securityKey: $securityKey")
//        Log.d("InfoUtils", "serviceToken: $serviceToken")
        val jsonData = generateJson(codename, romVersion, androidVersion, userId)
        val encryptedText = miuiEncrypt(jsonData, securityKey)
        val postData = "q=${encryptedText}&t=${serviceToken}&s=${port}"
        Log.d("InfoUtils", "postData: $postData")
        val requestedEncryptedText = request(postData)
        Log.d("InfoUtils", "requestedEncryptedText: $requestedEncryptedText")
        return miuiDecrypt(requestedEncryptedText, securityKey)
    }

    private fun request(jsonStr: String): String {
        val okHttpClient = OkHttpClient()
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        Log.d("InfoUtils", "jsonStr: $jsonStr")
        val body: RequestBody = jsonStr.toRequestBody(mediaType)
        val request = Request.Builder().url(miuiUrl).post(body).build()
        val response = okHttpClient.newCall(request).execute()
        return response.body?.string() ?: ""
    }
}
