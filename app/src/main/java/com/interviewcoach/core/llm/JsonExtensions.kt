package com.interviewcoach.core.llm

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonElement.jsonArrayOfStrings(): List<String> = jsonArray.map { it.jsonPrimitive.content }

fun JsonElement.jsonArrayOfObjects(): List<JsonObject> = jsonArray.map { it.jsonObject }
