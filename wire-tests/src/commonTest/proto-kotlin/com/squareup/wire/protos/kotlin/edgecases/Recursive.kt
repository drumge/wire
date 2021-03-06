// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: edge_cases.proto
package com.squareup.wire.protos.kotlin.edgecases

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class Recursive(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val value: Int? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.protos.kotlin.edgecases.Recursive#ADAPTER"
  )
  val recursive: Recursive? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Recursive, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Recursive) return false
    return unknownFields == other.unknownFields
        && value == other.value
        && recursive == other.recursive
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + value.hashCode()
      result = result * 37 + recursive.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (value != null) result += """value=$value"""
    if (recursive != null) result += """recursive=$recursive"""
    return result.joinToString(prefix = "Recursive{", separator = ", ", postfix = "}")
  }

  fun copy(
    value: Int? = this.value,
    recursive: Recursive? = this.recursive,
    unknownFields: ByteString = this.unknownFields
  ): Recursive = Recursive(value, recursive, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<Recursive> = object : ProtoAdapter<Recursive>(
      FieldEncoding.LENGTH_DELIMITED, 
      Recursive::class
    ) {
      override fun encodedSize(value: Recursive): Int = 
        ProtoAdapter.INT32.encodedSizeWithTag(1, value.value) +
        Recursive.ADAPTER.encodedSizeWithTag(2, value.recursive) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: Recursive) {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.value)
        Recursive.ADAPTER.encodeWithTag(writer, 2, value.recursive)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): Recursive {
        var value: Int? = null
        var recursive: Recursive? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> value = ProtoAdapter.INT32.decode(reader)
            2 -> recursive = Recursive.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return Recursive(
          value = value,
          recursive = recursive,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: Recursive): Recursive = value.copy(
        recursive = value.recursive?.let(Recursive.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
