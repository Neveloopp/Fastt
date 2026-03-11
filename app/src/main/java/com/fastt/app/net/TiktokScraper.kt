package com.fastt.app.net

import com.fastt.app.model.TiktokData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class TiktokScraper {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val ua =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

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
            val id = extractVideoId(tiktokUrl)
                ?: resolveShortUrl(tiktokUrl)
                ?: throw IllegalArgumentException("Could not extract video ID")

            val meta = getMetadata(id) ?: throw IllegalStateException("Could not get metadata")
            val videoUrl = getVideoUrl(meta) ?: throw IllegalStateException("Could not resolve video URL")

            Result.success(toData(id, meta, videoUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractVideoId(input: String): String? {
        val s = input.trim()
        if (s.matches(Regex("^\\d{15,25}$"))) return s
        Regex("/video/(\\d+)").find(s)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("/photo/(\\d+)").find(s)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    private fun resolveShortUrl(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .build()

            client.newCall(req).execute().use { res ->
                val loc = res.header("location").orEmpty()
                Regex("/video/(\\d+)").find(loc)?.groupValues?.getOrNull(1)?.let { return it }
                if (loc.isNotBlank()) {
                    val req2 = Request.Builder()
                        .url(loc)
                        .header("User-Agent", ua)
                        .build()
                    client.newCall(req2).execute().use { res2 ->
                        val loc2 = res2.header("location") ?: res2.request.url.toString()
                        Regex("/video/(\\d+)").find(loc2)?.groupValues?.getOrNull(1)?.let { return it }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildUrl(base: String, params: Map<String, String>): String {
        val sb = StringBuilder(base)
        if (!base.contains("?")) sb.append("?") else sb.append("&")
        sb.append(
            params.entries.joinToString("&") {
                val k = URLEncoder.encode(it.key, StandardCharsets.UTF_8.toString())
                val v = URLEncoder.encode(it.value, StandardCharsets.UTF_8.toString())
                "$k=$v"
            }
        )
        return sb.toString()
    }

    private fun getMetadata(videoId: String): Map<String, Any?>? {
        val url = buildUrl(
            "https://www.tiktok.com/api/item/detail/",
            paramsBase + mapOf(
                "itemId" to videoId,
                "coverFormat" to "2",
                "video_encoding" to "mp4"
            )
        )

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://www.tiktok.com/")
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                val obj = gson.fromJson(body, Map::class.java) as? Map<*, *> ?: return null
                val itemInfo = obj["itemInfo"] as? Map<*, *> ?: return null
                val itemStruct = itemInfo["itemStruct"] as? Map<String, Any?>
                if (itemStruct != null) return itemStruct
            }
        } catch (_: Exception) {
        }

        try {
            val req = Request.Builder()
                .url("https://www.tiktok.com/@_/video/$videoId")
                .header("User-Agent", ua)
                .header("Accept", "text/html")
                .build()

            client.newCall(req).execute().use { res ->
                val html = res.body?.string().orEmpty()
                val re = Regex(
                    "<script\\s+id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>(.*?)</script>",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                )
                val m = re.find(html) ?: return null
                val rawJson = m.groupValues[1]
                val parsed = gson.fromJson(rawJson, Map::class.java) as? Map<*, *> ?: return null
                val defaultScope = parsed["__DEFAULT_SCOPE__"] as? Map<*, *> ?: return null
                val detail = defaultScope["webapp.video-detail"] as? Map<*, *> ?: return null
                val itemInfo = detail["itemInfo"] as? Map<*, *> ?: return null
                val itemStruct = itemInfo["itemStruct"] as? Map<String, Any?>
                if (itemStruct != null) return itemStruct
            }
        } catch (_: Exception) {
        }

        return null
    }

    private fun getVideoUrl(meta: Map<String, Any?>): String? {
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
                .sortedByDescending {
                    (it["Bitrate"] as? Number)?.toLong()
                        ?: (it["bitrate"] as? Number)?.toLong()
                        ?: 0L
                }
            for (br in sorted) {
                addUrlList(br["PlayAddr"] ?: br["playAddr"], urls)
            }
        }

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
                v >= 1_000_000L -> {
                    val s = String.format(Locale.US, "%.1f", v / 1_000_000.0).removeSuffix(".0")
                    "${s}M"
                }
                v >= 1_000L -> {
                    val s = String.format(Locale.US, "%.1f", v / 1_000.0).removeSuffix(".0")
                    "${s}K"
                }
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

    private operator fun Map<String, String>.plus(other: Map<String, String>): Map<String, String> {
        val m = LinkedHashMap<String, String>(this.size + other.size)
        m.putAll(this)
        m.putAll(other)
        return m
    }
}
