package app.crawlers

import app.collectors.CSVCollector
import app.downloaders.RedditDownloader
import app.listeners.BasicListener
import app.pipelines.ConsolePipeline
import app.processors.RedditProcessor

import org.slf4j.LoggerFactory
import us.codecraft.webmagic.Spider
import us.codecraft.webmagic.downloader.Downloader
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader
import us.codecraft.webmagic.pipeline.JsonFilePipeline
import us.codecraft.webmagic.pipeline.Pipeline
import us.codecraft.webmagic.processor.PageProcessor
import us.codecraft.webmagic.processor.SimplePageProcessor
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover
import us.codecraft.webmagic.scheduler.QueueScheduler
import java.io.File
import kotlin.properties.Delegates

/**
 * A basic crawler that can be used to crawl a website.
 * @see Spider
 */
class BasicCrawler private constructor() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * The directory path where the crawled files will be saved. By default, it is "data".
     */
    private var fileDirPath: String = "data"

    /**
     * The directory where the crawled files will be saved. By default, it is "data".
     */
    private var dir: File = File(fileDirPath)

    /**
     * The page processor that will be used to process the pages. By default, it is a [SimplePageProcessor].
     * @see PageProcessor
     * @see SimplePageProcessor
     */
    private lateinit var processor: PageProcessor

    /**
     * The downloader that will be used to download the pages.
     * @see Downloader
     * @see VkDownloader
     * @see RedditDownloader
     * @see SeleniumDownloader
     */
    private lateinit var downloader: Downloader

    /**
     * The start time of the crawler.
     */
    private var startTime by Delegates.notNull<Long>()

    /**
     * The directory where the crawled files will be saved. By default, it is "data/data.csv".
     */
    private var savingDir: String = "$fileDirPath/$fileDirPath.csv"

    /**
     * The pipeline that will be used to save the results. By default, it is a [ConsolePipeline].
     * @see Pipeline
     * @see JsonFilePipeline
     * @see ConsolePipeline
     */
    private var pipeline: Pipeline = ConsolePipeline()

    /**
     * The number of threads that will be used to crawl the website. By default, it is 5.
     */
    private var threadCount = 5

    /**
     * Starts the crawling process.
     * @param limit The limit of the pages that will be crawled.
     * @see Spider
     */
    fun start(limit: Int, startUrl: String) {
        startTime = System.currentTimeMillis()

        if (this::processor.isInitialized.not()) {
            throw ExceptionInInitializerError("The processor is not initialized.")
        }


        if (this::downloader.isInitialized.not()) {
            throw ExceptionInInitializerError("The downloader is not initialized.")
        }

        val spider = initSpider(startTime, startUrl)

        savingDir = "$fileDirPath/$startTime/${spider.site.domain}"
        if (processor is RedditProcessor) {
            (processor as RedditProcessor).savingDirPath = savingDir
        }

        logger.info("Starting crawler with startUrl: $startUrl")
        logger.info("Page limit: $limit")

        logger.info("Downloader: ${downloader::class.java.name}")
        logger.info("Processor: ${processor::class.java.name}")
        logger.info("Pipeline: ${pipeline::class.java.name}")
        logger.info("Thread count: $threadCount")

        logger.info("Saving dir: $savingDir")

        spider.runAsync()

        while (spider.pageCount < limit && spider.status != Spider.Status.Stopped) {
            logger.info("Processed ${spider.pageCount} pages.")
            Thread.sleep(2000)
        }

        spider.stop()

        val totalTimeSeconds: Double = (System.currentTimeMillis() - startTime) / 1000.0
        val totalTimeMinutes: Double = totalTimeSeconds / 60
        logger.info("Crawler finished. Processed ${spider.pageCount} pages.")

        logger.info("Thread count: $threadCount")
        logger.info("Page count:   ${spider.pageCount}")

        logger.info("Total time, secs: ${totalTimeSeconds.format(3)}")
        logger.info("Total time, mins: ${totalTimeMinutes.format(3)}")
        logger.info("Pages per second: ${(spider.pageCount / totalTimeSeconds).format(3)}")
        logger.info("Pages per minute: ${(spider.pageCount / totalTimeMinutes).format(3)}")

        logger.info("Saving to $fileDirPath/$startTime.csv")
        collectDataToCSV()
    }


    /**
     * Initializes spider with the given time.
     * @param time Start time of the spider.
     * @see Spider
     */
    private fun initSpider(time: Long, startUrl: String): Spider {
        val spider = Spider.create(processor)
        val scheduler = QueueScheduler().setDuplicateRemover(BloomFilterDuplicateRemover(100000))

        if (pipeline !is ConsolePipeline) run {
            pipeline::class.java.declaredFields.forEach {
                it.isAccessible = true
                if (it.name == "path") {
                    it.set(pipeline, "$fileDirPath/$time")
                }
                it.isAccessible = false
            }
        }

        return spider.thread(threadCount)
            .addUrl(startUrl)
            .addPipeline(JsonFilePipeline("$fileDirPath/$startTime"))
            .setScheduler(scheduler)
            .setDownloader(downloader)
            .setExitWhenComplete(true)
            .setSpiderListeners(listOf(BasicListener()))
    }

    /**
     * Collects the data from the crawled files and saves it to a csv file.
     * @see CSVCollector
     */
    private fun collectDataToCSV() {
        val csvCollector = CSVCollector()

        csvCollector.collect(savingDir, "$fileDirPath/$startTime.csv")
    }

    /**
     * A builder class for the [BasicCrawler].
     * @see BasicCrawler
     * @see Builder
     * @see create
     */
    class Builder {

        private val crawler = BasicCrawler()

        /**
         * Creates a new [BasicCrawler].
         * @return A new [BasicCrawler].
         */
        fun create(): BasicCrawler {
            return crawler
        }

        /**
         * Sets the directory path where the crawled files will be saved.
         * @param fileDirPath The directory path where the crawled files will be saved.
         * @return The [Builder] instance.
         * @see BasicCrawler.fileDirPath
         */
        fun setFileDirPath(fileDirPath: String): Builder {
            crawler.fileDirPath = fileDirPath
            crawler.dir = File(fileDirPath)
            return this
        }

        /**
         * Sets the page processor that will be used to process the pages.
         * @param processor The page processor that will be used to process the pages.
         * @return The [Builder] instance.
         * @see BasicCrawler.processor
         */
        fun setProcessor(processor: PageProcessor): Builder {
            crawler.processor = processor
            return this
        }

        /**
         * Sets the number of threads that will be used to crawl the website.
         * @param threadCount The number of threads that will be used to crawl the website.
         * @return The [Builder] instance.
         * @see BasicCrawler.threadCount
         */
        fun setThreadCount(threadCount: Int): Builder {
            crawler.threadCount = threadCount
            return this
        }

        /**
         * Sets the pipeline that will be used to save the results.
         * @param pipeline The pipeline that will be used to save the results.
         * @return The [Builder] instance.
         * @see BasicCrawler.pipeline
         * @see Pipeline
         */
        fun setPipeline(pipeline: Pipeline): Builder {
            if (pipeline is ConsolePipeline) {
                crawler.pipeline = pipeline
            } else run {
                pipeline::class.java.fields.forEach {
                    if (it.name == "path") {
                        it.isAccessible = true
                        it.set(pipeline, crawler.fileDirPath)
                    }
                }
                crawler.pipeline = pipeline
            }

            return this
        }

        /**
         * Sets the downloader that will be used to download the pages.
         * @param downloader The downloader that will be used to download the pages.
         * @return The [Builder] instance.
         * @see BasicCrawler.downloader
         * @see Downloader
         */
        fun setDownloader(downloader: Downloader): Builder {
            crawler.downloader = downloader
            return this
        }
    }

    /**
     * Formats a [Double] number to a string with the given precision.
     * @param precision The precision of the number.
     * @return The formatted number.
     * @see String.format
     */
    private fun Double.format(precision: Int): String {
        return String.format("%.${precision}f", this)
    }
}
