package app.collectors

import app.exceptions.FileIsNotDirectoryException
import com.opencsv.CSVWriter
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Collector class for exporting JSON files into CSV file. Can be created with [CSVCollector.Builder.create]
 * @see CSVCollector.Builder
 * @see CSVCollector.collect
 */
class CSVCollector {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Exports all JSON files in a directory into a single CSV file
     * @param fromDir directory to export from
     * @param toFile file to export to
     * @throws FileIsNotDirectoryException if fromDir is not a directory
     * @see FileIsNotDirectoryException
     * @see File
     */
    fun collect(fromDir: String, toFile: String) {
        Thread.sleep(10_000)
        logger.info("Collecting from $fromDir to $toFile")

        val fileTo = File(toFile)
        val dir = File(fromDir)
        val csvWriter = CSVWriter(fileTo.writer())

        val headersList = dir.getHeaders()
        csvWriter.writeNext(headersList.toTypedArray())

        logger.info("Wrote headers")

        val valuesList = dir.getValues(headersList)
        valuesList.forEach {
            csvWriter.writeNext(it.toTypedArray())
        }
        csvWriter.close()
        logger.info("Wrote values")

        dir.parentFile.deleteRecursively()
        logger.info("Deleted directory")
        logger.info("Finished collecting")
    }

    /**
     * Builder for CSVCollector
     * @see CSVCollector
     */
    class Builder {

        /**
         * CSVCollector to be built
         * @see CSVCollector
         */
        private val collector = CSVCollector()

        /**
         * Create a CSVCollector
         * @return CSVCollector
         * @see CSVCollector
         */
        fun create(): CSVCollector {
            return collector
        }
    }

    /**
     * Get headers from a directory of JSON files
     * @return List<String> of headers
     * @throws FileIsNotDirectoryException if the file is not a directory
     * @see File
     * @see FileIsNotDirectoryException
     * @see JSONObject
     */
    private fun File.getHeaders(): List<String> {
        if (!this.isDirectory) throw FileIsNotDirectoryException(this)

        val headers = mutableSetOf<String>()
        this.listFiles()!!.forEach { file ->
            val json = JSONObject(file.readText())
            val keys = json.keys()

            keys.forEach { key ->
                headers.add(key.trim())
            }
        }

        return headers.sorted()
    }

    /**
     * Get values from a directory of JSON files
     * @param headers List<String> of headers
     * @return List<String> of values
     * @throws FileIsNotDirectoryException if the file is not a directory
     * @see File
     * @see FileIsNotDirectoryException
     * @see JSONObject
     */
    private fun File.getValues(headers: List<String>): List<List<String>> {
        if (!this.isDirectory) throw FileIsNotDirectoryException(this)

        val listValues = mutableListOf<List<String>>()

        this.listFiles()!!.forEach { file ->
            val json = JSONObject(file.readText())
            val values = mutableListOf<String>()

            headers.forEach { header ->
                if (json.has(header)) {
                    values.add(json.get(header).toString().trim())
                } else {
                    values.add("")
                }
            }

            if (values.all { it == "" }) return@forEach

            listValues.add(values)
        }

        return listValues
    }
}