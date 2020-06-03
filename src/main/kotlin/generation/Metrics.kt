package generation

fun precision(tp: Int, fp: Int) = if (tp == 0) 0f else tp.toFloat() / (tp + fp)
fun recall(tp: Int, fn: Int) = if (tp == 0) 0f else tp.toFloat() / (tp + fn)
fun F_05(tp: Int, fn: Int, fp: Int): Float {
    if (tp == 0) return 0f
    return 1.25f * tp / (1.25f * tp + 0.25f * fn + fp)
}
