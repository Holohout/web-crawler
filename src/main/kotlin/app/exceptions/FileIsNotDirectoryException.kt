package app.exceptions

import java.io.File

/**
 * Thrown when a file is not a directory.
 * @param file The file that is not a directory.
 * @param message The message to be displayed. Defaults to "File $file is not a directory."
 * @see File
 * @see FileSystemException
 */
class FileIsNotDirectoryException(file: File, message: String = "File $file is not a directory.") :
    FileSystemException(file, other = null, message)