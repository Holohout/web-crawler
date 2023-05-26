package app.pools;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool of ChromeDriver instances.
 * @see ChromeDriver
 */
public class ChromeDriverPool {
    private static final String DEFAULT_SESSIONS_PATH_PREFIX = "/tmp/selenium";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static int DEFAULT_CAPACITY = 5;

    private final int capacity;

    private final String sessionsPathPrefix;

    private final static int STATUS_RUNNING = 1;

    private final static int STATUS_CLOSED = 2;

    /**
     * List of sessions available to use.
     */
    private final List<AtomicInteger> sessions = Collections.synchronizedList(new ArrayList<>(16));

    public final AtomicInteger status = new AtomicInteger(STATUS_RUNNING);

    private WebDriver mDriver = null;
    private final boolean mAutoQuitDriver = true;

    private static final String DEFAULT_CONFIG_FILE = "/data/webmagic/webmagic-selenium/config.ini";
    private static final String DRIVER_FIREFOX = "firefox";
    private static final String DRIVER_CHROME = "chrome";
    private static final String DRIVER_PHANTOMJS = "phantomjs";

    protected static Properties sConfig;
    protected static DesiredCapabilities sCaps;

    /**
     * Configures the pool with default capacity and ChromeDriver.
     */
    public void configure() throws IOException {
        String pwd = System.getenv("PWD");
        System.setProperty("selenuim_config", pwd + "/src/main/resources/config.ini");

        sConfig = new Properties();
        String configFile = DEFAULT_CONFIG_FILE;
        if (System.getProperty("selenuim_config") != null) {
            configFile = System.getProperty("selenuim_config");
        }
        sConfig.load(new FileReader(configFile));

        sCaps = new DesiredCapabilities();
        sCaps.setJavascriptEnabled(true);
        sCaps.setCapability("takesScreenshot", false);

        String driver = sConfig.getProperty("driver", DRIVER_PHANTOMJS);

        if (driver.equals(DRIVER_PHANTOMJS)) {
            if (sConfig.getProperty("phantomjs_exec_path") != null) {
                sCaps.setCapability(
                        PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                        sConfig.getProperty("phantomjs_exec_path"));
            } else {
                throw new IOException(
                        String.format(
                                "Property '%s' not set!",
                                PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY));
            }

            if (sConfig.getProperty("phantomjs_driver_path") != null) {
                System.out.println("Test will use an external GhostDriver");
                sCaps.setCapability(
                        PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY,
                        sConfig.getProperty("phantomjs_driver_path"));
            } else {
                System.out
                        .println("Test will use PhantomJS internal GhostDriver");
            }
        }

        ArrayList<String> cliArgsCap = new ArrayList<String>();
        cliArgsCap.add("--web-security=false");
        cliArgsCap.add("--ssl-protocol=any");
        cliArgsCap.add("--ignore-ssl-errors=true");
        sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
                cliArgsCap);

        sCaps.setCapability(
                PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
                new String[]{"--logLevel="
                        + (sConfig.getProperty("phantomjs_driver_loglevel") != null ? sConfig
                        .getProperty("phantomjs_driver_loglevel")
                        : "INFO")});

        if (isUrl(driver)) {
            sCaps.setBrowserName("phantomjs");
            mDriver = new RemoteWebDriver(new URL(driver), sCaps);
        } else if (driver.equals(DRIVER_FIREFOX)) {
            mDriver = new FirefoxDriver(sCaps);
        } else if (driver.equals(DRIVER_CHROME)) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("websecurity=false");
            options.addArguments("ssl-protocol=any");
            options.addArguments("ignore-ssl-errors=true");

            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).compareAndSet(0, 1)) {
                    logger.info("Thread" + Thread.currentThread().getId() + ": Creating chrome driver, try " + i);
                    options.addArguments("user-data-dir=" + sessionsPathPrefix + i);
                    mDriver = new ChromeDriver(options);
                    logger.info("Thread" + Thread.currentThread().getId() + ": Chrome driver created");
                    break;
                }

                logger.warn("Thread" + Thread.currentThread().getId() + ": Chrome driver " + i + " is in use, try next");
            }
        } else if (driver.equals(DRIVER_PHANTOMJS)) {
            mDriver = new PhantomJSDriver(sCaps);
        }
    }

    /**
     * check whether input is a valid URL
     *
     * @param urlString urlString
     * @return true means yes, otherwise no.
     * @author bob.li.0718@gmail.com
     */
    private boolean isUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    /**
     * store webDrivers created
     */
    private final List<WebDriver> webDriverList = Collections
            .synchronizedList(new ArrayList<>());

    /**
     * store webDrivers available
     */
    private final BlockingDeque<WebDriver> innerQueue = new LinkedBlockingDeque<>();

    /**
     * Initialises pool
     * @param capacity
     * @param sessionsPathPrefix
     */
    public ChromeDriverPool(int capacity, String sessionsPathPrefix) {
        this.capacity = capacity;
        this.sessionsPathPrefix = sessionsPathPrefix;

        logger.info("Initializing sessions...");
        for (int i = 0; i < 16; i++) {
            logger.info("Initializing session " + i);
            sessions.add(new AtomicInteger(0));
        }
        logger.info("Sessions initialized");
    }

    public ChromeDriverPool() {
        this(DEFAULT_CAPACITY, DEFAULT_SESSIONS_PATH_PREFIX);
    }


    public WebDriver get() throws InterruptedException {
        if (isClosed()) {
            return null;
        }
        WebDriver poll = innerQueue.poll();
        if (poll != null) {
            return poll;
        }
        if (webDriverList.size() < capacity) {
            synchronized (webDriverList) {
                if (webDriverList.size() < capacity) {
                    try {
                        configure();
                        innerQueue.add(mDriver);
                        webDriverList.add(mDriver);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return innerQueue.take();
    }

    /**
     * Return webDriver.
     * @param webDriver
     */
    public void returnToPool(WebDriver webDriver) {
        innerQueue.add(webDriver);
    }

    /**
     * Checks if pool is closed.
     * @return
     */
    protected boolean isClosed() {
        if (!status.compareAndSet(STATUS_RUNNING, STATUS_RUNNING)) {
            logger.warn("Already closed!");
            return true;
        }
        return false;
    }

    /**
     * Closes pool.
     */
    public void closeAll() {
        boolean b = status.compareAndSet(STATUS_RUNNING, STATUS_CLOSED);
        if (!b) {
            throw new IllegalStateException("Already closed!");
        }
        for (WebDriver webDriver : webDriverList) {
            logger.info("Quit webDriver" + webDriver);
            webDriver.quit();
            webDriver = null;
        }
    }

}
