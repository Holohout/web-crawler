package app.listeners

import org.slf4j.LoggerFactory
import us.codecraft.webmagic.Request
import us.codecraft.webmagic.SpiderListener

/**
 * A basic listener that can be used to listen to the spider.
 * @see SpiderListener
 */
class BasicListener: SpiderListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onSuccess(request: Request?) {
        logger.info("Successfully processed ${request?.url}")
    }

    override fun onError(request: Request?, e: Exception?) {
        logger.error("Failed to process ${request?.url}")
    }
}