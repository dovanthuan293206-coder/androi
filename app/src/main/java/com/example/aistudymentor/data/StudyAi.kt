package com.example.aistudymentor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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
    suspend fun illustrate(apiKey: String, answer: String): Bitmap = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("model", "imx/gpt-image-2")
            .put("prompt", "Tạo một ảnh minh họa học tập bằng tiếng Việt từ lời giải sau. " +
                "Ưu tiên sơ đồ và hình vẽ rõ ràng, giữ đúng các ký hiệu và kết quả, hạn chế chữ dài.\n$answer")
        try {
            val image = post(apiKey, "images/generations", payload, readTimeout = 180_000)
                .getJSONArray("data").getJSONObject(0)
            val base64 = image.optString("b64_json", "")
            val bitmap = if (base64.isNotBlank()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                val url = URI(image.getString("url"))
                require(url.scheme.equals("https", ignoreCase = true) && url.host != null && url.userInfo == null) {
                    "Vilao trả về địa chỉ ảnh không hợp lệ."
                }
                val download = url.toURL().openConnection() as HttpURLConnection
                try {
                    download.instanceFollowRedirects = false
                    download.connectTimeout = 10_000
                    download.readTimeout = 30_000
                    download.inputStream.use { BitmapFactory.decodeStream(it) }
                } finally {
                    download.disconnect()
                }
            }
            checkNotNull(bitmap) { "Vilao trả về ảnh không đọc được." }
        } catch (_: JSONException) {
            error("Vilao trả về dữ liệu ảnh chưa hợp lệ.")
        } catch (_: IllegalArgumentException) {
            error("Vilao trả về định dạng ảnh chưa hợp lệ.")
        }
    }

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

    suspend fun quiz(apiKey: String, subject: String, level: String, topic: String, count: Int): JSONArray =
        withContext(Dispatchers.IO) {
            require(count > 0) { "Số câu hỏi phải lớn hơn 0." }
            if (count > 5) {
                val combined = JSONArray()
                var remaining = count
                while (remaining > 0) {
                    val batch = minOf(5, remaining)
                    val questions = quiz(apiKey, subject, level, "$topic. Phần gồm các câu mới, lượt ${combined.length() / 5 + 1}", batch)
                    repeat(questions.length()) { combined.put(questions.getJSONObject(it)) }
                    remaining -= batch
                }
                return@withContext combined
            }
            val answer = complete(apiKey, "Môn: $subject. Cấp học: $level. Chủ đề: ${topic.ifBlank { "ôn tập tổng hợp" }}. " +
                "Tạo đúng $count câu trắc nghiệm khác nhau bằng tiếng Việt. Chỉ trả mảng JSON. " +
                "Mỗi phần tử có question (chuỗi), options (4 chuỗi khác nhau), " +
                "correctIndex (số nguyên từ 0 đến 3), explanation (lời giải ngắn).", maxTokens = 4800)
            try {
                val quiz = JSONArray(answer.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                require(quiz.length() == count) { "AI chưa tạo đủ $count câu. Hãy thử lại." }
                repeat(quiz.length()) { index ->
                    val item = quiz.getJSONObject(index)
                    val options = item.getJSONArray("options")
                    val choices = (0 until options.length()).map { options.get(it) as? String }
                    require(item.get("question") is String && item.getString("question").isNotBlank() &&
                        item.get("explanation") is String && item.getString("explanation").isNotBlank() &&
                        choices.size == 4 && choices.none { it.isNullOrBlank() } &&
                        choices.map { it?.trim()?.lowercase() }.distinct().size == 4 &&
                        item.get("correctIndex") is Int && item.getInt("correctIndex") in 0..3) {
                        "AI trả về câu trắc nghiệm chưa hợp lệ. Hãy tạo lại."
                    }
                }
                quiz
            } catch (_: JSONException) {
                error("AI trả về bộ câu hỏi chưa hợp lệ. Hãy tạo lại.")
            }
        }

    private fun encodeImage(context: Context, uri: Uri): String {
        val bitmap = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = minOf(1f, 1800f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: java.io.IOException) {
            error("Không đọc được ảnh đề bài. Hãy chọn ảnh khác.")
        }
        val output = java.io.ByteArrayOutputStream()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) { "Không chuẩn bị được ảnh đề bài." }
            return "data:image/jpeg;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}"
        } finally {
            bitmap.recycle()
        }
    }

    private fun complete(apiKey: String, content: Any, maxTokens: Int = 1800): String {
        require(apiKey.isNotBlank()) { "Nhập khóa Vilao trong tab Cá nhân trước khi sử dụng AI." }
        val payload = JSONObject().put("model", "occ/claude-opus-4-8").put("max_tokens", maxTokens)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system")
                    .put("content", "Bạn là gia sư học tập. Trả lời chính xác và phù hợp với cấp học."))
                .put(JSONObject().put("role", "user").put("content", content)))
        try {
            val response = post(apiKey, "chat/completions", payload)
            val choice = response.getJSONArray("choices").getJSONObject(0)
            check(choice.optString("finish_reason") != "length") { "Lời giải vượt quá độ dài cho phép. Hãy chia nhỏ yêu cầu." }
            val answer = (choice.getJSONObject("message").get("content") as? String)?.trim()
            check(!answer.isNullOrBlank()) { "Vilao trả về nội dung trống. Hãy thử lại." }
            return answer
        } catch (_: JSONException) {
            error("Vilao trả về phản hồi chưa hợp lệ. Hãy thử lại.")
        }
    }

    private fun post(apiKey: String, path: String, payload: JSONObject, readTimeout: Int = 85_000): JSONObject {
        check(apiKey.isNotBlank()) { "Nhập khóa Vilao trong tab Cá nhân trước khi sử dụng AI." }
        val connection = URI.create("https://api.vilao.ai/v1/$path")
            .toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = readTimeout
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                error(when (status) {
                    401, 403 -> "Vilao từ chối truy cập (HTTP $status). Kiểm tra khóa và quyền dùng model."
                    402 -> "Kiểm tra số dư tài khoản Vilao (HTTP 402)."
                    429 -> "Vilao đang giới hạn lượt gọi. Hãy thử lại sau."
                    else -> "Vilao trả về lỗi HTTP $status. Hãy thử lại."
                })
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }
}
