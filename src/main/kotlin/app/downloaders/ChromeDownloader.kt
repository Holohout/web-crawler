package app.downloaders

import app.pools.ChromeDriverPool
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Request
import us.codecraft.webmagic.Task
import us.codecraft.webmagic.downloader.AbstractDownloader
import us.codecraft.webmagic.selector.Html
import us.codecraft.webmagic.selector.PlainText
import java.io.Closeable
import java.io.IOException

/**
 * A downloader that uses ChromeDriver to download pages.
 * @param username the username to be used to log in.
 * @param password the password to be used to log in.
 * @see AbstractDownloader
 * @see ChromeDriverPool
 * @see WebDriver
 * @see Closeable
 */
abstract class ChromeDownloader(
    protected val username: String,
    protected val password: String
) : AbstractDownloader(), Closeable {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * The pool of ChromeDriver instances.
     */
    protected var webDriverPool: ChromeDriverPool? = null

    private var sleepTime = 100

    protected var poolSize = 1

    /**
     * The maximum number of threads that can be used to download pages to be it real multi-threaded.
     */
    private val maxThreadsCount = Runtime.getRuntime().availableProcessors() * 2

    /**
     * Initializes chromedriver.
     * @throws IOException if the chromedriver path is null.
     */
    init {
        logger.info("Initializing ChromeDownloader...")
        val chromeDriverPath = Runtime.getRuntime().exec("which chromedriver").inputStream.bufferedReader().readLine()

        if (chromeDriverPath == null) {
            logger.error("ChromeDriver path is null.")
            throw IOException("ChromeDriver path is null.")
        }

        logger.info("ChromeDriver path: $chromeDriverPath")
        System.setProperty("webdriver.chrome.driver", chromeDriverPath)

        createSessions()
    }

    fun setSleepTime(sleepTime: Int): ChromeDownloader {
        this.sleepTime = sleepTime
        return this
    }

    /**
     * Creates sessions to be used to download pages.
     */
    private fun createSessions() {
        logger.info("Creating $maxThreadsCount sessions...")

        for (i in 0 until maxThreadsCount) {
            logger.info("Creating session $i...")
            login(i)
            logger.info("Created session $i.")
        }

        logger.info("Created $maxThreadsCount sessions.")
    }

    /**
     * Logs in to the website.
     */
    protected abstract fun login(sessionId: Int): String

    /**
     * Downloads the page.
     * @param request the request to be downloaded.
     * @param task the task of the request.
     * @return the downloaded page.
     */
    override fun download(request: Request, task: Task): Page? {
        checkInit()
        var webDriver: WebDriver? = null
        val page = Page.fail()
        try {
            webDriver = webDriverPool?.get() ?: throw IOException("no webDriver")
            logger.info("downloading page " + request.url)
            webDriver[request.url]
            try {
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime.toLong())
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val manage = webDriver.manage()
            val site = task.site
            if (site.cookies != null) {
                for ((key, value) in site.cookies) {
                    val cookie = Cookie(
                        key,
                        value
                    )
                    manage.addCookie(cookie)
                }
            }

            val webElement = webDriver.findElement(By.xpath("/html"))
            val content = webElement.getAttribute("outerHTML")
            page.isDownloadSuccess = true
            page.setRawText(content)
            page.html = Html(content, request.url)
            page.url = PlainText(request.url)
            page.request = request
            onSuccess(request, task)
        } catch (e: Exception) {
            logger.warn("download page {} error", request.url)
            onError(request, task, e)
        } finally {
            if (webDriver != null) {
                webDriverPool!!.returnToPool(webDriver)
            }
        }
        return page
    }

    /**
     * Checks if the downloader is initialized. An exception is thrown if it is not.
     */
    protected abstract fun checkInit()

    override fun setThread(thread: Int) {
        poolSize = thread
    }

    @Throws(IOException::class)
    override fun close() {
        webDriverPool!!.closeAll()
    }
}