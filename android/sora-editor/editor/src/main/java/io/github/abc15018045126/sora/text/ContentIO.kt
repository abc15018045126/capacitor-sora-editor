package io.github.abc15018045126.sora.text

import java.io.*
import java.nio.charset.Charset

object ContentIO {
    private const val BUFFER_SIZE = 16384

    @JvmStatic @Throws(IOException::class)
    fun createFrom(stream: InputStream) = createFrom(stream, Charset.defaultCharset())

    @JvmStatic @Throws(IOException::class)
    fun createFrom(stream: InputStream, charset: Charset) = createFrom(InputStreamReader(stream, charset))

    @JvmStatic @Throws(IOException::class)
    fun createFrom(reader: Reader) = reader.use { r ->
        Content().apply {
            isUndoEnabled = false
            val buf = CharArray(BUFFER_SIZE)
            val wrap = CharArrayWrapper(buf, 0)
            var n: Int
            while (r.read(buf).also { n = it } != -1) {
                if (n <= 0) continue
                if (buf[n - 1] == '\r') {
                    val p = r.read()
                    if (p == '\n'.toInt()) {
                        wrap.setDataCount(n - 1)
                        insert(lineCount - 1, getColumnCount(lineCount - 1), wrap)
                        insert(lineCount - 1, getColumnCount(lineCount - 1), "\r\n")
                        continue
                    } else if (p != -1) {
                        wrap.setDataCount(n)
                        insert(lineCount - 1, getColumnCount(lineCount - 1), wrap)
                        insert(lineCount - 1, getColumnCount(lineCount - 1), p.toChar().toString())
                        continue
                    }
                }
                wrap.setDataCount(n)
                insert(lineCount - 1, getColumnCount(lineCount - 1), wrap)
            }
            isUndoEnabled = true
        }
    }

    @JvmStatic @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, close: Boolean) = writeTo(text, stream, Charset.defaultCharset(), close)

    @JvmStatic @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, charset: Charset, close: Boolean) = writeTo(text, OutputStreamWriter(stream, charset), close)

    @JvmStatic @Throws(IOException::class)
    fun writeTo(text: Content, writer: Writer, close: Boolean) {
        val bw = (writer as? BufferedWriter) ?: BufferedWriter(writer, BUFFER_SIZE)
        try {
            text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer2 { _, line, _ ->
                try {
                    bw.write(line.backingCharArray, 0, line.length)
                    bw.write(line.lineSeparatorSafe.chars)
                } catch (e: IOException) { throw RuntimeException(e) }
            })
            bw.flush()
        } catch (e: RuntimeException) {
            (e.cause as? IOException)?.let { throw it } ?: throw e
        } finally { if (close) bw.close() }
    }
}
