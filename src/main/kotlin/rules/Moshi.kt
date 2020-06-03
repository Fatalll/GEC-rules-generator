package rules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val MOSHI: Moshi = Moshi.Builder()
    .add(PolymorphicJsonAdapterFactory.of(TokenCondition::class.java, "type")
            .withSubtype(AnyCondition::class.java, TokenCondition.Type.ANY.name)
            .withSubtype(ORCondition::class.java, TokenCondition.Type.OR.name)
            .withSubtype(ANDCondition::class.java, TokenCondition.Type.AND.name)
            .withSubtype(NOTCondition::class.java, TokenCondition.Type.NOT.name)
            .withSubtype(TextCondition::class.java, TokenCondition.Type.TEXT.name)
            .withSubtype(LemmaCondition::class.java, TokenCondition.Type.LEMMA.name)
            .withSubtype(POSCondition::class.java, TokenCondition.Type.POS.name)
            .withSubtype(ChunkTagCondition::class.java, TokenCondition.Type.CHUNK_TAG.name)
            .withSubtype(ChunkPosCondition::class.java, TokenCondition.Type.CHUNK_POS.name)
            .withSubtype(PositionInSentenceCondition::class.java, TokenCondition.Type.SENT_POS.name)
            .withSubtype(RelationCondition::class.java, TokenCondition.Type.RELATION.name))
    .add(PolymorphicJsonAdapterFactory.of(ReplaceModification::class.java, "type")
            .withSubtype(Replace::class.java, TokenModification.Type.REPLACE.name)
            .withSubtype(Inflect::class.java, TokenModification.Type.INFLECT.name)
            .withSubtype(Casing::class.java, TokenModification.Type.CASING.name))
    .add(PolymorphicJsonAdapterFactory.of(InsertModification::class.java, "type")
        .withSubtype(Insert::class.java, TokenModification.Type.INSERT.name))
    .add(PolymorphicJsonAdapterFactory.of(DeleteModification::class.java, "type")
        .withSubtype(Delete::class.java, TokenModification.Type.DELETE.name))
    .add(KotlinJsonAdapterFactory())
    .build()