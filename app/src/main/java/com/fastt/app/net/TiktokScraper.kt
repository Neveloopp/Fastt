package com.fastt.app.net

import com.fastt.app.model.TiktokData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt

class TiktokScraper {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val paramsBase = mapOf(
        "aid" to "1988",
        "app_name" to "tiktok_web",
        "device_platform" to "web_pc",
        "region" to "US",
        "language" to "en",
        "cookie_enabled" to "true",
        "browser_language" to "en-US",
        "browser_name" to "Mozilla",
        "browser_online" to "true",
        "browser_platform" to "Win32",
        "os" to "windows",
        "screen_width" to "1920",
        "screen_height" to "1080",
    )

    fun fetch(tiktokUrl: String): Result<TiktokData> {
        return try {
            val id = extractVideoId(tiktokUrl) ?: resolveShortUrl(tiktokUrl)
                ?: return Result.failure(RuntimeException("Could not extract video ID"))

            val meta = getMetadata(id)
                ?: return Result.failure(RuntimeException("Could not get metadata"))

            val videoUrl = getVideoUrl(meta)
                ?: return Result.failure(RuntimeException("Could not get video url"))

            Result.success(toData(id, meta, videoUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^\\d{15,25}$"))) return trimmed
        Regex("/video/(\\d+)").find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("/photo/(\\d+)").find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    private fun resolveShortUrl(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .build()

            client.newCall(req).execute().use { res ->
                val loc = res.header("location") ?: res.header("Location")
                val target = loc ?: res.request.url.toString()
                Regex("/video/(\\d+)").find(target)?.groupValues?.getOrNull(1)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildUrl(base: String, params: Map<String, String>): String {
        val q = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, StandardCharsets.UTF_8.toString())}=${URLEncoder.encode(v, StandardCharsets.UTF_8.toString())}"
        }
        return "$base?$q"
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMetadata(videoId: String): Map<String, Any?>? {
        // 1) JSON endpoint
        try {
            val url = buildUrl(
                "https://www.tiktok.com/api/item/detail/",
                paramsBase + mapOf(
                    "itemId" to videoId,
                    "coverFormat" to "2",
                    "video_encoding" to "mp4",
                )
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://www.tiktok.com/")
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (res.isSuccessful && body.isNotBlank()) {
                    val parsed = gson.fromJson(body, Map::class.java) as Map<String, Any?>
                    val itemInfo = parsed["itemInfo"] as? Map<String, Any?>
                    val itemStruct = itemInfo?.get("itemStruct") as? Map<String, Any?>
                    if (itemStruct != null) return itemStruct
                }
            }
        } catch (_: Exception) {
        }

        // 2) HTML fallback (__UNIVERSAL_DATA_FOR_REHYDRATION__)
        try {
            val req = Request.Builder()
                .url("https://www.tiktok.com/@_/video/$videoId")
                .header("User-Agent", ua)
                .header("Accept", "text/html")
                .build()

            client.newCall(req).execute().use { res ->
                val html = res.body?.string().orEmpty()
                val m = Regex("<script\\s+id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>(.*?)</script>", setOf(RegexOption.DOT_MATCHES_ALL)).find(html)
                val rawJson = m?.groupValues?.getOrNull(1) ?: return null
                val parsed = gson.fromJson(rawJson, Map::class.java) as Map<String, Any?>
                val defaultScope = parsed["__DEFAULT_SCOPE__"] as? Map<String, Any?>
                val detail = defaultScope?.get("webapp.video-detail") as? Map<String, Any?>
                val itemInfo = detail?.get("itemInfo") as? Map<String, Any?>
                val itemStruct = itemInfo?.get("itemStruct") as? Map<String, Any?>
                if (itemStruct != null) return itemStruct
            }
        } catch (_: Exception) {
        }

        return null
    }

    private fun getVideoUrl(meta: Map<String, Any?>): String? {
        // Try to extract a list of candidates (playAddr/downloadAddr/urlList)
        val video = meta["video"] as? Map<*, *> ?: return null

        fun addUrlList(any: Any?, out: MutableList<String>) {
            when (any) {
                is String -> out.add(any)
                is Map<*, *> -> {
                    val list = any["urlList"] ?: any["UrlList"]
                    if (list is List<*>) list.forEach { if (it is String) out.add(it) }
                }
            }
        }

        val urls = mutableListOf<String>()
        addUrlList(video["playAddr"], urls)
        addUrlList(video["downloadAddr"], urls)

        val bitrateInfo = video["bitrateInfo"]
        if (bitrateInfo is List<*>) {
            val sorted = bitrateInfo
                .mapNotNull { it as? Map<*, *> }
                .sortedByDescending { (it["Bitrate"] as? Number)?.toLong() ?: (it["bitrate"] as? Number)?.toLong() ?: 0L }
            for (br in sorted) {
                addUrlList(br["PlayAddr"] ?: br["playAddr"], urls)
            }
        }

        // Validate candidates by HEAD
        for (u in urls.distinct()) {
            try {
                val req = Request.Builder()
                    .url(u)
                    .head()
                    .header("User-Agent", ua)
                    .header("Referer", "https://www.tiktok.com/")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) return u
                }
            } catch (_: Exception) {
            }
        }
        return urls.firstOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun toData(id: String, meta: Map<String, Any?>, videoUrl: String): TiktokData {
        val author = meta["author"] as? Map<String, Any?>
        val stats = meta["stats"] as? Map<String, Any?>
        val video = meta["video"] as? Map<String, Any?>
        val music = meta["music"] as? Map<String, Any?>

        val desc = (meta["desc"] as? String).orEmpty()
        val width = (video?.get("width") as? Number)?.toInt()
        val height = (video?.get("height") as? Number)?.toInt()

        val cover = (video?.get("cover") as? String)
            ?: (video?.get("dynamicCover") as? String)
            ?: (video?.get("originCover") as? String)

        fun fmt(n: Number?): String {
            val v = n?.toLong() ?: 0L
            return when {
                v >= 1_000_000 -> ((v / 1_000_000.0) * 10.0).roundToInt() / 10.0
                    .toString().removeSuffix(".0") + "M"
                v >= 1_000 -> ((v / 1_000.0) * 10.0).roundToInt() / 10.0
                    .toString().removeSuffix(".0") + "K"
                else -> v.toString()
            }
        }

        return TiktokData(
            url = "https://www.tiktok.com/@_/video/$id",
            id = id,
            author = author?.get("uniqueId") as? String,
            name = author?.get("nickname") as? String,
            avatar = author?.get("avatarLarger") as? String
                ?: author?.get("avatarMedium") as? String
                ?: author?.get("avatarThumb") as? String,
            bio = author?.get("signature") as? String,
            verified = author?.get("verified") as? Boolean,
            description = desc,
            date = null,
            resolution = if (width != null && height != null) "${width}x${height}" else null,
            quality = video?.get("definition") as? String,
            likes_formatted = fmt(stats?.get("diggCount") as? Number),
            comments_formatted = fmt(stats?.get("commentCount") as? Number),
            shares_formatted = fmt(stats?.get("shareCount") as? Number),
            views_formatted = fmt(stats?.get("playCount") as? Number),
            duration = (video?.get("duration") as? Number)?.toInt(),
            music = music?.get("title") as? String,
            artist = music?.get("authorName") as? String,
            hashtags = null,
            videoUrl = videoUrl,
            thumbnail = cover
        )
    }
}
