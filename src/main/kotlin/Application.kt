import app.crawlers.BasicCrawler
import app.downloaders.RedditDownloader
import app.downloaders.VkDownloader
import app.processors.RedditProcessor
import app.processors.VkProcessor

fun main(args: Array<String>) {
    val redditCrawler = BasicCrawler.Builder()
        .setProcessor(RedditProcessor())
        .setDownloader(RedditDownloader("*********", "*********"))
        .setFileDirPath("data/reddit")
        .setThreadCount(12)
        .create()

//    val vkCrawler = BasicCrawler.Builder()
//        .setProcessor(VkProcessor())
//        .setDownloader(VkDownloader("+44 (747) 536-4056", "bebra1vpn"))
//        .setFileDirPath("data/vk")
//        .setThreadCount(2)
//        .create()

    redditCrawler.start(10_000, "https://reddit.com/")

//    println("\b123   \tb123".trim())
}