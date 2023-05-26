package app.pipelines

import us.codecraft.webmagic.ResultItems
import us.codecraft.webmagic.Task
import us.codecraft.webmagic.pipeline.Pipeline

/**
 * A pipeline that prints the results to the console.
 * @see Pipeline
 */
class ConsolePipeline : Pipeline {

    /**
     * Prints the results to the console.
     * @param resultItems The results to be printed.
     * @param task The task that is being executed.
     * @see ResultItems
     * @see Task
     */
    override fun process(resultItems: ResultItems, task: Task?) {
        println("Page: " + resultItems.request.url)

        for ((key, value) in resultItems.all) {
            println("$key:\t$value")
        }
    }
}