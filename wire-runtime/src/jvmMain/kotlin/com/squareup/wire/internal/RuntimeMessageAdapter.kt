/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.internal

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.Message.Builder
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.WireLog
import java.io.IOException
import java.util.Collections
import java.util.LinkedHashMap
import android.util.Log
import com.squareup.wire.AndroidMessage
import com.squareup.wire.EnumAdapter

class RuntimeMessageAdapter<M : Message<M, B>, B : Builder<M, B>>(
  private val messageType: Class<M>,
  private val builderType: Class<B>,
  val fieldBindings: Map<Int, FieldBinding<M, B>>
) : ProtoAdapter<M>(FieldEncoding.LENGTH_DELIMITED, messageType.kotlin) {

  fun newBuilder(): B = builderType.newInstance()

  override fun encodedSize(value: M): Int {
    val cachedSerializedSize = value.cachedSerializedSize
    if (cachedSerializedSize != 0) return cachedSerializedSize

    var size = 0
    for (fieldBinding in fieldBindings.values) {
      val binding = fieldBinding[value] ?: continue
      val adapter = if (fieldBinding.isMap) {
        fieldBinding.adapter()
      } else {
        fieldBinding.singleAdapter()
      }
      if (!fieldBinding.label.isRepeated && adapter is EnumAdapter) {
        val enumValue = fieldBinding.getEnumValue(value)
        if (enumValue != -1) {
          size += adapter.encodedSizeWithTag(fieldBinding.tag, enumValue)
        }
      } else {
        size += fieldBinding.adapter().encodedSizeWithTag(fieldBinding.tag, binding)
      }
    }
    size += value.unknownFields.size

    value.cachedSerializedSize = size
    return size
  }

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: M) {
    for (fieldBinding in fieldBindings.values) {
      var binding = fieldBinding[value] ?: continue
      if (WireLog.debugLogLevel > 3) {
        Log.d("RuntimeMessageAdapter", "encode messageType: ${messageType}, fieldBinding.name: ${fieldBinding.name}, " +
            "fieldBinding.tag: ${fieldBinding.tag}, fieldBinding.builderType: ${fieldBinding.builderType}" +
            "binding: ${binding}")
      }
      val adapter = if (fieldBinding.isMap) {
        fieldBinding.adapter()
      } else {
        fieldBinding.singleAdapter()
      }
      if (WireLog.debugLogLevel > 3) {
        Log.d("RuntimeMessageAdapter", "encode messageType: ${messageType}," +
            " fieldBinding.name: ${fieldBinding.name}, " +
            "adapter: ${adapter}, " +
            "fieldBinding.redacted: ${fieldBinding.redacted}, " +
            "fieldBinding.label: ${fieldBinding.label}")
      }
      if (!fieldBinding.label.isRepeated && adapter is EnumAdapter) {
        val enumValue = fieldBinding.getEnumValue(value)
        if (enumValue != -1) {
          adapter.encodeWithTag(writer, fieldBinding.tag, enumValue)
        }
      } else {
        fieldBinding.adapter().encodeWithTag(writer, fieldBinding.tag, binding)
      }
//      fieldBinding.adapter().encodeWithTag(writer, fieldBinding.tag, binding)
    }
    writer.writeBytes(value.unknownFields)
  }

  override fun redact(value: M): M {
    val builder = value.newBuilder()
    for (fieldBinding in fieldBindings.values) {
      if (fieldBinding.redacted && fieldBinding.label == WireField.Label.REQUIRED) {
        throw UnsupportedOperationException(
            "Field '${fieldBinding.name}' in ${type?.javaObjectType?.name} is required and " +
                "cannot be redacted.")
      }
      val isMessage = Message::class.java
          .isAssignableFrom(fieldBinding.singleAdapter().type?.javaObjectType)
      if (fieldBinding.redacted || isMessage && !fieldBinding.label.isRepeated) {
        val builderValue = fieldBinding.getFromBuilder(builder)
        if (builderValue != null) {
          val redactedValue = fieldBinding.adapter().redact(builderValue)
          fieldBinding[builder] = redactedValue
        }
      } else if (isMessage && fieldBinding.label.isRepeated) {
        @Suppress("UNCHECKED_CAST")
        val values = fieldBinding.getFromBuilder(builder) as List<Any>
        @Suppress("UNCHECKED_CAST")
        val adapter = fieldBinding.singleAdapter() as ProtoAdapter<Any>
        fieldBinding[builder] = values.redactElements(adapter)
      }
    }
    builder.clearUnknownFields()
    return builder.build()
  }

  override fun equals(other: Any?): Boolean {
    return other is RuntimeMessageAdapter<*, *> && other.messageType == messageType
  }

  override fun hashCode(): Int = messageType.hashCode()

  override fun toString(value: M): String = buildString {
    for (fieldBinding in fieldBindings.values) {
      val binding = fieldBinding[value]
      if (binding != null) {
        append(", ")
        append(fieldBinding.name)
        append('=')
        append(if (fieldBinding.redacted) REDACTED else binding)
      }
    }

    // Replace leading comma with class name and opening brace.
    replace(0, 2, messageType.simpleName + '{')
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): M {
    val builder = newBuilder()
    val token = reader.beginMessage()
    val tags = mutableListOf<Int>()
    var isDefault = true
    loop@ while (true) {
      val tag = reader.nextTag()
      if (tag == -1) break
      val fieldEncoding = reader.peekFieldEncoding()!!
      val fieldBinding = fieldBindings[tag]
      if (WireLog.debugLogLevel > 3) {
        Log.d("RuntimeMessageAdapter", "decode messageType: ${messageType}," +
            " fieldBinding?.name: ${fieldBinding?.name}," +
            " fieldBinding?.tag: ${fieldBinding?.tag}, fieldBinding?.builderType: ${fieldBinding?.builderType}, " +
            "tag: ${tag}, fieldEncoding: ${fieldEncoding}, fieldBindings: ${fieldBindings}")
      }
      try {
        if (fieldBinding != null) {
          isDefault = false
          val adapter = if (fieldBinding.isMap) {
            fieldBinding.adapter()
          } else {
            fieldBinding.singleAdapter()
          }
          var value: Any? = null
          try {
            // TODO tag 一样，但是wireType不一样，会抛异常
            if (adapter is EnumAdapter) {
              val intValue = reader.readVarint32()
              value = adapter.decode(intValue)
              fieldBinding.setEnumValue(builder, intValue)
            } else {
              value = adapter.decode(reader)
            }
          } catch (stateE: ProtocolStateException) {
            if (WireLog.debugLogLevel > 0) {
              Log.e("RuntimeMessageAdapter", "decode 1 ProtocolStateException messageType: ${messageType}, " +
                  "fieldBinding.name: ${fieldBinding?.name}, " +
                  "tag: ${tag}, " +
                  "fieldBinding.label: ${fieldBinding?.label}, adapter.fieldEncoding: ${adapter.fieldEncoding}, " +
                  "value: ${value}, " +
                  "fieldEncoding: ${fieldEncoding}")
            }
            if (WireLog.debugLogLevel > 0) {
              stateE.printStackTrace()
            }
            reader.readUnknownField(tag)
            continue@loop
          } catch (e: Exception) {
            if (WireLog.debugLogLevel > 0) {
              Log.e("RuntimeMessageAdapter", "decode 1 Exception " +
                  "messageType: ${messageType}, " +
                  "fieldBinding.name: ${fieldBinding?.name}," +
                  " fieldBinding?.builderType: ${fieldBinding?.builderType}" +
                  "fieldBinding.label: ${fieldBinding?.label}, " +
                  " tag: ${tag}, " +
                  "fieldEncoding: ${fieldEncoding}")
            }
            if (WireLog.debugLogLevel > 0) {
              e.printStackTrace()
            }
            throw e
          }

          if (value != null) {
            fieldBinding.value(builder, value!!)
            if (value is Message<*, *>) {
//              setDefaultFalse(value, false)
            }
          } else {
            fieldBinding.defaultValue(builder)
          }
          tags.add(tag)
          if (WireLog.debugLogLevel > 3) {
            Log.d("RuntimeMessageAdapter", "decode finish  messageType: ${messageType}, " +
                "fieldBinding.name: ${fieldBinding?.name}, " +
                "tag: ${tag}, " +
                "fieldBinding.label: ${fieldBinding?.label}, adapter.fieldEncoding: ${adapter.fieldEncoding}, " +
                "value: ${value}, " +
                "fieldEncoding: ${fieldEncoding}")
          }
        } else {
          val value = fieldEncoding.rawProtoAdapter().decode(reader)
          builder.addUnknownField(tag, fieldEncoding, value)
        }
      } catch (e: EnumConstantNotFoundException) {
        // An unknown Enum value was encountered, store it as an unknown field.
        if (WireLog.debugLogLevel > 0) {
          Log.e("RuntimeMessageAdapter", "decode 2 EnumConstantNotFoundException " +
              "messageType: ${messageType}, fieldBinding.name: ${fieldBinding?.name}," +
              " fieldBinding?.builderType: ${fieldBinding?.builderType}" +
              "fieldBinding.label: ${fieldBinding?.label}, " +
              " tag: ${tag}, " +
              "fieldEncoding: ${fieldEncoding}")
        }
        if (WireLog.debugLogLevel > 0) {
          e.printStackTrace()
        }
        builder.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
      } catch (e: Exception) {
        if (WireLog.debugLogLevel > 0) {
          Log.e("RuntimeMessageAdapter", "decode 2 Exception " +
              "messageType: ${messageType}, " +
              "fieldBinding.name: ${fieldBinding?.name}," +
              " fieldBinding?.builderType: ${fieldBinding?.builderType}" +
              "fieldBinding.label: ${fieldBinding?.label}, " +
              " tag: ${tag}, " +
              "fieldEncoding: ${fieldEncoding}")
        }
        if (WireLog.debugLogLevel > 0) {
          e.printStackTrace()
        }
        throw e
      }

    }
    reader.endMessageAndGetUnknownFields(token) // Ignore return value

    // start 为空时默认返回值
    fieldBindings.values.forEach { field ->
      if (field.returnDefaultValue && !tags.contains(field.tag)) {
        val binding = field.getFromBuilder(builder)
        if (binding == null) {
          if (WireLog.debugLogLevel > 3) {
            Log.d("RuntimeMessageAdapter", "decode defaultValue  messageType: ${messageType}, " +
                "field: ${field}, " +
                "tag: ${tag}, binding: ${binding}")
          }
          field.defaultValue(builder)
        }
      }
    }
    // end

    val msg = builder.build()
    if (WireLog.debugLogLevel > 2) {
      Log.i("RuntimeMessageAdapter", "messageType = " + messageType + " , isDefault = " + isDefault)
    }
    if (isDefault) {
      setDefaultInstanceFlag(msg, true)
    }
    return msg
  }

  private fun setDefaultInstanceFlag(msg: Message<*, *>, isDefault: Boolean) {
    try {
      val defaultInstanceField = msg::class.java.getDeclaredField("__isDefaultInstance")
      defaultInstanceField.isAccessible = true
      defaultInstanceField.set(msg, isDefault)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  companion object {
    private const val REDACTED = "\u2588\u2588"

    @JvmStatic fun <M : Message<M, B>, B : Builder<M, B>> create(
      messageType: Class<M>
    ): RuntimeMessageAdapter<M, B> {
      val builderType = getBuilderType(messageType)
      val fieldBindings = LinkedHashMap<Int, FieldBinding<M, B>>()

      // Create tag bindings for fields annotated with '@WireField'
      for (messageField in messageType.declaredFields) {
        val wireField = messageField.getAnnotation(WireField::class.java)
        if (wireField != null) {
          fieldBindings[wireField.tag] = FieldBinding(wireField, messageField, builderType)
        }
      }

      return RuntimeMessageAdapter(messageType, builderType,
          Collections.unmodifiableMap(fieldBindings))
    }

    private fun <M : Message<M, B>, B : Builder<M, B>> getBuilderType(
      messageType: Class<M>
    ): Class<B> {
      try {
        return Class.forName("${messageType.name}\$Builder") as Class<B>
      } catch (_: ClassNotFoundException) {
        throw IllegalArgumentException(
            "No builder class found for message type ${messageType.name}")
      }
    }
  }
}
