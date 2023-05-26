import com.opencsv.CSVWriter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

fun OkHttpClient.saveCommentsForPostViaApiToCsv(url: String, names: List<String>, writer: CSVWriter) {
    val postId = getPostId(url) ?: return
    val request = Request.Builder()
        .url("https://www.reddit.com/comments/$postId.json")
        .header("User-Agent", "OkHttp Bot 1.0")
        .build()

    println(request.url)

    val response = this.newCall(request).execute()
    val jsonArray = JSONArray(response.body?.string())

    for (i in 1 until jsonArray.length()) {
        val comments = jsonArray.getJSONObject(i).getJSONObject("data").getJSONArray("children")

        for (j in 0 until comments.length()) {
            val comment = comments.getJSONObject(j).getJSONObject("data")
            val row = mutableListOf<String>()

            names.forEach { name ->
                if (comment.has(name)) {
                    when (name) {
                        "depth" -> row.add((comment.getInt("depth") + 1).toString())
                        "id" -> row.add("t1_" + comment.getString("id"))
                        "permalink" -> row.add(
                            "https://www.reddit.com" + comment.get(name).toString().substringBeforeLast("/") + "/"
                        )

                        else -> row.add(comment.get(name).toString())
                    }
                } else {
                    when (name) {
                        "comment" -> row.add("true")
                        "post" -> row.add("false")
                        "major" -> row.add((comment.getInt("depth") == 0).toString())
                        else -> row.add("")
                    }
                }
            }
            writer.writeNext(row.toTypedArray())

            if (comment.has("replies") && comment.get("replies").toString() != "") {
                saveReplies(
                    names,
                    comment.getJSONObject("replies").getJSONObject("data").getJSONArray("children"),
                    writer
                )
            }
        }
    }
}

fun saveReplies(names: List<String>, comments: JSONArray, writer: CSVWriter) {
    for (i in 0 until comments.length()) {
        val comment = comments.getJSONObject(i).getJSONObject("data")
        val row = mutableListOf<String>()

        names.forEach { name ->
            if (comment.has(name)) {
                when(name) {
                    "depth" -> row.add((comment.getInt("depth") + 1).toString())
                    "id" -> row.add("t1_" + comment.getString("id"))
                    "permalink" -> row.add("https://www.reddit.com" + comment.get(name).toString().substringBeforeLast("/") + "/")
                    else -> row.add(comment.get(name).toString())
                }
            } else {
                when (name) {
                    "comment" -> row.add("true")
                    "post" -> row.add("false")
                    "major" -> row.add((comment.getInt("depth") == 0).toString())
                    else -> row.add("")
                }
            }
        }
        writer.writeNext(row.toTypedArray())

        if (comment.has("replies") && comment.get("replies").toString() != "") {
            saveReplies(names, comment.getJSONObject("replies").getJSONObject("data").getJSONArray("children"), writer)
        }
    }
}

fun getDataFromApi(names: List<String>, file: File) {
    val urls = mutableSetOf<String>()
    val client = OkHttpClient()

    file.readLines().forEach { line ->
        val url = line.split(",\"").last().substringBeforeLast("/\"")
        if (url == "url\"") return@forEach
        urls.add(url)
    }

    val csvWriter = CSVWriter(File(file.parentFile.path + "/" + file.nameWithoutExtension + "-api.csv").writer())
    csvWriter.writeNext(names.toTypedArray())
    urls.forEach { url ->
        client.saveCommentsForPostViaApiToCsv(url, names, csvWriter)
    }
    csvWriter.close()
}

fun getPostId(postUrl: String): String? {
    val regex = Regex("/comments/([a-zA-Z0-9]+)")
    val match = regex.find(postUrl)
    return match?.groupValues?.get(1)
}

fun main() {
    val client = OkHttpClient()
    val names = mutableListOf<String>(
        "author",
        "comment",
        "body",
        "countOfComments",
        "score",
        "id",
        "depth",
        "major",
        "post",
        "created",
        "subreddit",
        "title",
        "permalink"
    )

    getDataFromApi(names, File("data/reddit/1685106610292.csv"))
}
