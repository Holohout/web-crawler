package app.processors

import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.processor.PageProcessor
import java.io.File

class VkProcessor : PageProcessor {

    private val site = Site.me().setRetryTimes(3).setSleepTime(200).setCharset("UTF-8")

    override fun process(page: Page) {
        page.addTargetRequests(page.html.links().regex("https://vk\\.com/\\w+").all())
        page.addTargetRequests(page.html.links().regex("https://vk\\.com/\\w+\\?w=wall\\d+_\\d+").all())

        val posts = page.html.links().regex("https://vk.com/feed?w=wall([0-9_]+)").all()
        page.addTargetRequests(posts)

        val postNames = page.html.xpath("//a[@class='author']/@href")
            .all()
            .map { it.split("/")[1] }
            .distinct()

        page.putField("postNames", postNames)
        page.putField("url", page.url.toString())
        page.putField("text", page.html.xpath("//div[@class='wall_post_text']/tidyText()"))
        File("${site.domain}.html").writeText(page.html.toString())
    }

    override fun getSite(): Site {
        return site
    }

}