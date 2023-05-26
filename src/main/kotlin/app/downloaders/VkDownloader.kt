package app.downloaders

import app.pools.ChromeDriverPool

/**
 * Downloader class for downloading data from VK.
 * @see ChromeDownloader
 */
class VkDownloader(username: String, password: String) : ChromeDownloader(username, password) {

    private val vkSessionsPathPrefix = "/tmp/selenium-vk"

    override fun login(sessionId: Int): String {
        logger.info("Already logged in session $sessionId")
        return sessionId.toString()
    }

    override fun checkInit() {
        if (webDriverPool == null) {
            synchronized(this) { webDriverPool = ChromeDriverPool(poolSize, vkSessionsPathPrefix) }
        }
    }
}