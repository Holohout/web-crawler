package app.processors

import app.model.RedditComment
import app.model.RedditPost
import org.json.JSONObject
import org.slf4j.LoggerFactory
import us.codecraft.webmagic.*
import us.codecraft.webmagic.processor.PageProcessor
import us.codecraft.webmagic.selector.Html
import java.io.File

/**
 * Reddit page processor.
 * @see PageProcessor
 */
class RedditProcessor : PageProcessor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    var savingDirPath: String = ""

    private val site: Site = Site.me()
        .setRetryTimes(3).setSleepTime(200).setCharset("UTF-8")

    private val redditUrl = site.domain

    private val subredditRegex = Regex("^(https://)((www)|([\\w-]+).|)(reddit.com)/r/([\\w\\d-_)]+)/")

    private val commentsRegex = Regex("^(https://)((www)|([\\w-]+).|)(reddit.com)/r/([\\w\\d-_)]+)/([\\w-_\\d./#)]+)")

    override fun process(page: Page) {
        val links = page.html.links().all()
        val comments = links.filter { commentsRegex.matches(it) }
        val subreddits = links.filter { subredditRegex.matches(it) }

        comments.forEach {
            page.addTargetRequest(it)
        }

        subreddits.forEach {
            page.addTargetRequest(it)
        }

        if (subredditRegex.matches(page.url.toString())) {
            logger.info("Saving all posts for subreddit ${page.url}")
            saveAllSubredditPosts(page, subredditRegex.find(page.url.toString())!!.groupValues[6])
            return
        }

        if (commentsRegex.matches(page.url.toString())) {
            logger.info("Saving all comments for post ${page.url}")
            saveAllComments(page, subredditRegex.find(page.url.toString())!!.groupValues[6])

        }
    }

    private fun saveAllComments(page: Page, subreddit: String) {
        val commentsDivs = page.html.xpath("//div[@class='Comment']").all()
        logger.info("Found ${commentsDivs.size} comments")

        commentsDivs.forEach { commentDiv ->
            val html = Html(commentDiv)
            val id = "t1_${html.xpath("//div/@class").regex("t1_(.*)").get().split(" ")[0]}"

            val author = html.xpath("//a[@data-testid='comment_author_link']/text()").get()

            val publishedDate = html.xpath("//a[@data-testid='comment_timestamp']/text()").get()

            val div = html.xpath("//div[@data-testid='comment']//div[@class='RichTextJSON-root']")
                .xpath("//p/text()").all()
            var content = ""
            div.forEach { content += it }

            val countOfLikes = (html.xpath("//div[@id='vote-arrows-$id']//div").get()
                ?.substringAfter(">\n ")?.substringBefore("\n<") ?: "0").fromRedditCount()

            val level = commentDiv.substringAfter("level ").substringBefore("</").toInt()

            val redditComment = RedditComment(
                id = id,
                author = author,
                publishedDate = publishedDate,
                content = content,
                countOfLikes = countOfLikes,
                isPost = false,
                isComment = true,
                isMajor = level == 1,
                url = page.url.toString(),
                level = level,
                subreddit = subreddit
            )

            File("$savingDirPath/$id.json").serializeToJsonFile(JSONObject(redditComment))
        }

    }

    private val wordsToContain = listOf(
        "global warming",
        "climate change",
        "Global warming",
        "Climate change",
        "Global Warming",
        "Climate Change"
    )

    private fun saveAllSubredditPosts(page: Page, subreddit: String) {
        val postsDivs = page.html.xpath("//div[@class='Post']").all()
        logger.info("Found ${postsDivs.size} posts")

        postsDivs.forEach { postDiv ->
            val html = Html(postDiv)
            val id = "t3_${html.xpath("//div/@id").regex("t3_(.*)").get()}"

            val title = html.xpath("//h3/text()").get().trim()
            val author = html.xpath("//a[@data-click-id='user']/text()").get().substringAfter("u/").trim()

            val publishedDate = html.xpath("//span[@data-click-id='timestamp']/text()").get() ?: return@forEach

            val div = html.xpath("//div[@data-click-id='text']//div[@class='RichTextJSON-root']").xpath(
                "//p/text()"
            ).all()
            var content = ""
            div.forEach { content += it }

            val url =
                "https://www.reddit.com${html.xpath("//div[@data-adclicklocation='title]").xpath("//a/@href").get()}"

            val countOfLikes = html.xpath("//div[@id='vote-arrows-$id']//div")
                .get().substringAfter("</span>").substringBefore("</div>").trim()
                .fromRedditCount()
            val countOfComments = html.xpath("//a[@data-click-id='comments']//span/text()")
                .get().fromRedditCount()

            val redditPost = RedditPost(
                id = id,
                title = title,
                author = author,
                publishedDate = publishedDate,
                content = content,
                url = url,
                subreddit = subreddit,
                countOfLikes = countOfLikes,
                countOfComments = countOfComments,
                isPost = true,
                isComment = false
            )

            File("$savingDirPath/$id.json").serializeToJsonFile(JSONObject(redditPost))
        }
    }

    private fun File.serializeToJsonFile(json: JSONObject) {
        this.parentFile.mkdirs()
        this.createNewFile()
        this.writeText(json.toString(2))
    }

    private fun String.fromRedditCount(): Int {
        val number = this.split(" ")[0]
        if (number.last() == 'k') {
            return (number.substring(0, number.length - 1).toDouble() * 1000).toInt()
        }
        if (!number[0].isDigit()) {
            return 0
        }
        return number.toInt()
    }

    override fun getSite(): Site {
        return site
    }
}