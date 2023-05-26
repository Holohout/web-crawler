package app.downloaders

import app.pools.ChromeDriverPool
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.util.concurrent.TimeUnit

/**
 * Downloader class for downloading Reddit posts.
 * @see ChromeDownloader
 */
class RedditDownloader(username: String, password: String) : ChromeDownloader(username, password) {

    /**
     * The prefix of the path to the directory where the sessions are stored.
     */
    private val redditSessionsPathPrefix = "/tmp/selenium-reddit"

    override fun login(sessionId: Int): String {
        return ""
        val options = ChromeOptions()
        options.addArguments("user-data-dir=/tmp/selenium-reddit$sessionId")

        logger.info("Logging to Reddit...")
        val webDriver: WebDriver = ChromeDriver(options)
        val redditLoginUrl = "https://www.reddit.com/login/"
        var loggingCookie: Cookie

        try {
            webDriver.get(redditLoginUrl)
            webDriver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS)
            logger.info("Opened the login page.")

            val emailInput = webDriver.findElement(By.id("loginUsername"))
            val passwordInput = webDriver.findElement(By.id("loginPassword"))
            val loginButton = webDriver.findElement(By.className("AnimatedForm__submitButton"))

            emailInput.clear()
            passwordInput.clear()
            logger.info("Cleared the email and password inputs.")

            emailInput.sendKeys(username)
            passwordInput.sendKeys(password)
            logger.info("Entered the email and password.")

            loginButton.click()
            logger.info("Clicked the login button.")
            loggingCookie = webDriver.manage().getCookieNamed("session")

            logger.info("Got the session cookie $loggingCookie.")
        } catch (e: Exception) {
            loggingCookie = Cookie("session", "null")
            logger.warn("You are already logged in.")
        } finally {
            Thread.sleep(1000)
            webDriver.quit()
            logger.info("Closed the web driver.")
        }


        return loggingCookie.toString()
    }

    override fun checkInit() {
        if (webDriverPool == null) {
            synchronized(this) { webDriverPool = ChromeDriverPool(poolSize, redditSessionsPathPrefix) }
        }
    }
}