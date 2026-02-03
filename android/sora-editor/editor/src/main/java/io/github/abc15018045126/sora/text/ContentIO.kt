package io.github.abc15018045126.sora.text

import java.io.*
import java.nio.charset.Charset


object ContentIO {

    private const val BUFFER_SIZE = 16384


    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(stream: InputStream): Content {
        return createFrom(stream, Charset.defaultCharset())
    }


    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(stream: InputStream, charset: Charset): Content {
        return createFrom(InputStreamReader(stream, charset))
    }


    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(reader: Reader): Content {
        val content = Content()
        content.isUndoEnabled = false
        val buffer = CharArray(BUFFER_SIZE)
        val wrapper = CharArrayWrapper(buffer, 0)
        var count: Int
        while (reader.read(buffer).also { count = it } != -1) {
            if (count > 0) {
                if (buffer[count - 1] == '\r') {
                    val peek = reader.read()
                    if (peek == '\n'.toInt()) {
                        wrapper.setDataCount(count - 1)
                        var line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), wrapper)
                        line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), "\r\n")
                        continue
                    } else if (peek != -1) {
                        wrapper.setDataCount(count)
                        var line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), wrapper)
                        line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), peek.toChar().toString())
                        continue
                    }
                }
                wrapper.setDataCount(count)
                val line = content.lineCount - 1
                content.insert(line, content.getColumnCount(line), wrapper)
            }
        }
        reader.close()
        content.isUndoEnabled = true
        return content
    }


    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, closeOnSucceed: Boolean) {
        writeTo(text, stream, Charset.defaultCharset(), closeOnSucceed)
    }


    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, charset: Charset, closeOnSucceed: Boolean) {
        writeTo(text, OutputStreamWriter(stream, charset), closeOnSucceed)
    }


    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(text: Content, writer: Writer, closeOnSucceed: Boolean) {

        val buffered = if (writer is BufferedWriter) writer else BufferedWriter(writer, BUFFER_SIZE)
        try {
            text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer2 { _, line, _ ->
                try {

                    buffered.write(line.backingCharArray, 0, line.length)

                    buffered.write(line.lineSeparatorSafe.chars)
                } catch (e: IOException) {

                    throw RuntimeException(e)
                }
            })
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is IOException) {
                throw cause
            } else {
                throw e
            }
        }
        buffered.flush()
        if (closeOnSucceed) {
            buffered.close()
        }
    }

}
