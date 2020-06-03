package generation

import java.util.*
import kotlin.collections.ArrayList

enum class FeatureType {
    RELATION, POS, CHUNK_POS, CHUNK_TAG, SENT_POS, LEMMA
}

val FEATURE_TRIPLES = ArrayList<List<EnumSet<FeatureType>>>().apply {
    for (features1 in featureGenerator()) {
        for (features2 in featureGenerator()) {
            for (features3 in featureGenerator()) {
                add(listOf(features1, features2, features3))
            }
        }
    }
}

fun featureGenerator() = sequence<EnumSet<FeatureType>> {
    yield(EnumSet.noneOf(FeatureType::class.java))

    val types = FeatureType.values()
    for (feature in types) {
        yield(EnumSet.of(feature))
    }

    for (ind1 in IntRange(0, types.size - 1)) {
        for (ind2 in IntRange(ind1 + 1, types.size - 1)) {
            yield(EnumSet.of(types[ind1], types[ind2]))
        }
    }

    for (ind1 in IntRange(0, types.size - 1)) {
        for (ind2 in IntRange(ind1 + 1, types.size - 1)) {
            for (ind3 in IntRange(ind2 + 1, types.size - 1)) {
                yield(EnumSet.of(types[ind1], types[ind2], types[ind3]))
            }
        }
    }

    for (ind1 in IntRange(0, types.size - 1)) {
        for (ind2 in IntRange(ind1 + 1, types.size - 1)) {
            for (ind3 in IntRange(ind2 + 1, types.size - 1)) {
                for (ind4 in IntRange(ind3 + 1, types.size - 1)) {
                    yield(EnumSet.of(types[ind1], types[ind2], types[ind3], types[ind4]))
                }
            }
        }
    }

    for (ind1 in IntRange(0, types.size - 1)) {
        for (ind2 in IntRange(ind1 + 1, types.size - 1)) {
            for (ind3 in IntRange(ind2 + 1, types.size - 1)) {
                for (ind4 in IntRange(ind3 + 1, types.size - 1)) {
                    for (ind5 in IntRange(ind4 + 1, types.size - 1)) {
                        yield(EnumSet.of(types[ind1], types[ind2], types[ind3], types[ind4], types[ind5]))
                    }
                }
            }
        }
    }

    yield(EnumSet.allOf(FeatureType::class.java))
}