package app.processors

import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.processor.PageProcessor

/**
 * A processor that extracts information from GitHub.
 * @see PageProcessor
 */
class GithubProcessor : PageProcessor {

    /**
     * The site to be crawled.
     * @see Site
     */
    private val site: Site = Site.me().setRetryTimes(3).setSleepTime(200)

    /**
     * Processes the page.
     * @param page The page to be processed.
     * @see Page
     */
    override fun process(page: Page) {
        val links = page.html.links().regex("(https://github\\.com/\\w+/\\w+)").all()
        val author = page.url.regex("https://github\\.com/(\\w+)/.*").toString()
        val readme = page.html.xpath("//div[@id='readme']/tidyText()")
        val title = page.html.xpath("//title/text()").toString()
        val url = page.url.toString()
        val text = page.html.xpath("//div[@id='readme']/tidyText()")
        val comments = page.html.xpath("//div[@id='readme']/tidyText()")

        page.addTargetRequests(links)
        page.putField("author", author)
        page.putField("readme", readme)
        page.putField("title", title)
        page.putField("url", url)
        page.putField("text", text)
        page.putField("comments", comments)
    }

    override fun getSite(): Site {
        return site
    }
}