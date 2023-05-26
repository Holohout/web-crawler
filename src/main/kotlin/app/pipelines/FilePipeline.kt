package app.pipelines

import com.opencsv.CSVWriter
import org.slf4j.LoggerFactory
import us.codecraft.webmagic.ResultItems
import us.codecraft.webmagic.Task
import us.codecraft.webmagic.pipeline.Pipeline
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A pipeline that writes the results to a file.
 * @param path The path to the file.
 * @see Pipeline
 */
class FilePipeline(path: String) : Pipeline {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * A flag that indicates whether the headers have been written to the file.
     * @see AtomicBoolean
     */
    private val wasWritten: AtomicBoolean = AtomicBoolean(false)

    /**
     * The headers of the CSV file.
     */
    private lateinit var headers: List<String>

    /**
     * The CSV writer.
     * @see CSVWriter
     */
    private var writer: CSVWriter = CSVWriter(FileWriter(path))

    /**
     * Writes the results to a file.
     * @param resultItems The results to be written.
     * @param task The task that is being executed.
     * @see ResultItems
     * @see Task
     * @see CSVWriter
     */
    override fun process(resultItems: ResultItems, task: Task?) {
        if (wasWritten.compareAndSet(false, true)) {
            headers = resultItems.all.keys.toList()
            writer.writeNext(headers.toTypedArray())
        }

        val values = mutableListOf<String>()
        val resultKeys = resultItems.all.keys

        headers.forEach { header ->
            if (resultKeys.contains(header)) {
                values.add(resultItems.all[header].toString())
            } else {
                values.add("")
            }
        }
    }
}