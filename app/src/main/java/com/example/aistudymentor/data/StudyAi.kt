package com.example.aistudymentor.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URI

internal object StudyAi {
    suspend fun ask(apiKey: String, context: Context, question: String, image: Uri?, subject: String, level: String, style: String): String =
        withContext(Dispatchers.IO) {
            val prompt = "Môn: $subject. Cấp học: $level. Phong cách: $style. " +
                "Giải đề bài bằng tiếng Việt theo từng bước. Nếu thiếu dữ kiện, hãy nêu rõ.\n" +
                "Đề bài: ${question.ifBlank { "Xem ảnh đính kèm." }}"
            val content = if (image == null) prompt else JSONArray()
                .put(JSONObject().put("type", "text").put("text", prompt))
                .put(JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", encodeImage(context, image))))
            complete(apiKey, content)
        }

    suspend fun quiz(apiKey: String, subject: String, level: String, difficulty: String): String =
        withContext(Dispatchers.IO) {
            val answer = complete(apiKey, "Môn: $subject. Cấp học: $level. Độ khó: $difficulty. " +
                "Tạo một câu trắc nghiệm tiếng Việt. Chỉ trả JSON có question (chuỗi), options " +
                "(4 chuỗi khác nhau), correctIndex (số nguyên từ 0 đến 3), explanation (chuỗi).")
            val quiz = JSONObject(answer.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
            val options = quiz.getJSONArray("options")
            val choices = (0 until options.length()).map { options.get(it) as? String }
            require(quiz.get("question") is String && quiz.getString("question").isNotBlank() &&
                quiz.get("explanation") is String && quiz.getString("explanation").isNotBlank() &&
                choices.size == 4 && choices.none { it.isNullOrBlank() } &&
                choices.map { it?.trim()?.lowercase() }.distinct().size == 4 &&
                quiz.get("correctIndex") is Int && quiz.getInt("correctIndex") in 0..3) {
                "Vilao trả về câu trắc nghiệm chưa hợp lệ. Hãy tạo lại."
            }
            quiz.toString()
        }

    private fun encodeImage(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        require(mime in listOf("image/jpeg", "image/png", "image/webp", "image/gif")) {
            "Hãy chọn ảnh JPG, PNG, WebP hoặc GIF."
        }
        val limit = 4 * 1024 * 1024
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= limit) { "Ảnh phải nhỏ hơn hoặc bằng 4 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        require(bytes != null && bytes.isNotEmpty()) { "Không đọc được ảnh đã chọn." }
        return "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    private fun complete(apiKey: String, content: Any): String {
        require(apiKey.isNotBlank()) { "Nhập khóa Vilao trong tab Cá nhân trước khi sử dụng AI." }
        val payload = JSONObject().put("model", "occ/claude-opus-4-8").put("max_tokens", 1800)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system")
                    .put("content", "Bạn là gia sư học tập. Trả lời chính xác và phù hợp với cấp học."))
                .put(JSONObject().put("role", "user").put("content", content)))
        val connection = URI.create("https://api.vilao.ai/v1/chat/completions")
            .toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 85_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                error(when (status) {
                    401, 403 -> "Vilao từ chối truy cập (HTTP $status). Kiểm tra khóa trong tab Cá nhân."
                    402 -> "Kiểm tra số dư tài khoản Vilao (HTTP 402)."
                    429 -> "Vilao đang giới hạn lượt gọi. Hãy thử lại sau."
                    else -> "Vilao trả về lỗi HTTP $status. Hãy thử lại."
                })
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            val choice = response.getJSONArray("choices").getJSONObject(0)
            check(choice.optString("finish_reason") != "length") { "Lời giải vượt quá độ dài cho phép. Hãy chia nhỏ yêu cầu." }
            val answer = (choice.getJSONObject("message").get("content") as? String)?.trim()
            check(!answer.isNullOrBlank()) { "Vilao trả về nội dung trống. Hãy thử lại." }
            return answer
        } catch (_: JSONException) {
            error("Vilao trả về phản hồi chưa hợp lệ. Hãy thử lại.")
        } finally {
            connection.disconnect()
        }
    }
}
