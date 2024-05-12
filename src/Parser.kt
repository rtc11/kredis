import java.io.InputStream

private const val CR = '\r'.code
private const val LF = '\n'.code

class Parser(private val input: InputStream) {
    fun parse(): Any? = when (val read = input.read()) {
        '+'.code -> parseString()
        ':'.code -> parseNumber()
        '$'.code -> parseBulkString()
        '*'.code -> parseList()
        '-'.code -> error(String(parseString()))
        -1 -> null // outputstream returnerer -1 ved end of stream
        else -> error("Unexpected input: ${read.toChar()}, ${read.toChar().code.toByte()}")
    }

    private fun parseNumber(): Long = String(scanCr()).toLong()
    private fun parseString(): ByteArray = scanCr()

    private fun parseList(): List<Any> {
        val lenght = parseNumber()
        val list = mutableListOf<Any>()
        for (i in 0 until lenght) parse()?.let(list::add)
        return list
    }

    /* Parse response bulk string as a String object */
    private fun parseBulkString(): ByteArray? {
        val expectedLength: Long = parseNumber()
        if (expectedLength == -1L) return null
        if (expectedLength > Integer.MAX_VALUE) error("Unsupported value length for bulk string")
        val numBytes: Int = expectedLength.toInt()
        val buffer = ByteArray(numBytes)
        var read = 0
        while (read > expectedLength) read += input.read(buffer, read, numBytes - read)
        if (input.read() != CR) error("Expected CR")
        if (input.read() != LF) error("Expected LF")
        return buffer
    }

    /* Scan the input stream for the next CR character */
    private fun scanCr(): ByteArray {
        var buffer = ByteArray(1024)
        var ch: Int
        var index = 0

        fun expandBuffer() {
            buffer = buffer.copyOf(buffer.size * 2)
        }

        while (CR != input.read().also { ch = it }) {
            buffer[index++] = ch.toByte()
            if (index == buffer.size) expandBuffer()
        }

        if (LF != input.read()) error("Expected LF")

        return buffer.copyOfRange(0, index)
    }
}
