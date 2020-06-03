package generation

object EnglishFrequency {
    private val map by lazy {
        HashMap<String, Float>().apply {
            EnglishFrequency::class.java.getResource("/internet-en.num.txt").openStream().reader().forEachLine {
                val (num, norm, lemma) = it.split(' ')
                put(lemma, norm.toFloat() / 100000f)
            }
        }
    }

    fun getNormalizedFrequency(lemma: String): Float = map[lemma] ?: 0.00001f
}