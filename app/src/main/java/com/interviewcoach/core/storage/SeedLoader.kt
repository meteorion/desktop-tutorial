package com.interviewcoach.core.storage

import androidx.room.withTransaction
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun loadSeedIfEmpty(db: AppDatabase, rawJson: String) {
    if (db.positionDao().getAllPositions().isNotEmpty()) return

    val positions = Json.parseToJsonElement(rawJson).jsonArray
    db.withTransaction {
        for (positionJson in positions) {
            val p = positionJson.jsonObject
            val positionId = p["id"]!!.jsonPrimitive.content
            db.positionDao().upsertPosition(PositionEntity(id = positionId, name = p["name"]!!.jsonPrimitive.content))
            for (kpJson in p["knowledgePoints"]!!.jsonArray) {
                val kp = kpJson.jsonObject
                db.positionDao().upsertKnowledgePoint(
                    KnowledgePointEntity(
                        id = kp["id"]!!.jsonPrimitive.content,
                        positionId = positionId,
                        name = kp["name"]!!.jsonPrimitive.content,
                        weight = kp["weight"]!!.jsonPrimitive.content.toInt(),
                        difficulty = kp["difficulty"]!!.jsonPrimitive.content.toInt(),
                        isCore = kp["isCore"]!!.jsonPrimitive.content.toBoolean(),
                    ),
                )
            }
        }
    }
}
