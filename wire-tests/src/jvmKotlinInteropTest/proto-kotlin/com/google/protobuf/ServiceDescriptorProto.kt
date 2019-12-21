// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.checkElementsNotNull
import com.squareup.wire.internal.redactElements
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * Describes a service.
 */
class ServiceDescriptorProto(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val name: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.google.protobuf.MethodDescriptorProto#ADAPTER",
    label = WireField.Label.REPEATED
  )
  @JvmField
  val method: List<MethodDescriptorProto> = emptyList(),
  @field:WireField(
    tag = 3,
    adapter = "com.google.protobuf.ServiceOptions#ADAPTER"
  )
  @JvmField
  val options: ServiceOptions? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<ServiceDescriptorProto, ServiceDescriptorProto.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.name = name
    builder.method = method
    builder.options = options
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is ServiceDescriptorProto) return false
    return unknownFields == other.unknownFields
        && name == other.name
        && method == other.method
        && options == other.options
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + method.hashCode()
      result = result * 37 + options.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (name != null) result += """name=$name"""
    if (method.isNotEmpty()) result += """method=$method"""
    if (options != null) result += """options=$options"""
    return result.joinToString(prefix = "ServiceDescriptorProto{", separator = ", ", postfix = "}")
  }

  fun copy(
    name: String? = this.name,
    method: List<MethodDescriptorProto> = this.method,
    options: ServiceOptions? = this.options,
    unknownFields: ByteString = this.unknownFields
  ): ServiceDescriptorProto = ServiceDescriptorProto(name, method, options, unknownFields)

  class Builder : Message.Builder<ServiceDescriptorProto, Builder>() {
    @JvmField
    var name: String? = null

    @JvmField
    var method: List<MethodDescriptorProto> = emptyList()

    @JvmField
    var options: ServiceOptions? = null

    fun name(name: String?): Builder {
      this.name = name
      return this
    }

    fun method(method: List<MethodDescriptorProto>): Builder {
      checkElementsNotNull(method)
      this.method = method
      return this
    }

    fun options(options: ServiceOptions?): Builder {
      this.options = options
      return this
    }

    override fun build(): ServiceDescriptorProto = ServiceDescriptorProto(
      name = name,
      method = method,
      options = options,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<ServiceDescriptorProto> = object :
        ProtoAdapter<ServiceDescriptorProto>(
      FieldEncoding.LENGTH_DELIMITED, 
      ServiceDescriptorProto::class
    ) {
      override fun encodedSize(value: ServiceDescriptorProto): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.name) +
        MethodDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(2, value.method) +
        ServiceOptions.ADAPTER.encodedSizeWithTag(3, value.options) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: ServiceDescriptorProto) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        MethodDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.method)
        ServiceOptions.ADAPTER.encodeWithTag(writer, 3, value.options)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): ServiceDescriptorProto {
        var name: String? = null
        val method = mutableListOf<MethodDescriptorProto>()
        var options: ServiceOptions? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> method.add(MethodDescriptorProto.ADAPTER.decode(reader))
            3 -> options = ServiceOptions.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return ServiceDescriptorProto(
          name = name,
          method = method,
          options = options,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: ServiceDescriptorProto): ServiceDescriptorProto = value.copy(
        method = value.method.redactElements(MethodDescriptorProto.ADAPTER),
        options = value.options?.let(ServiceOptions.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
