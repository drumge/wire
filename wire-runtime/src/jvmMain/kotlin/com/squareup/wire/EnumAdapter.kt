/*
 * Copyright 2016 Square Inc.
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
package com.squareup.wire

import java.io.IOException
import kotlin.reflect.KClass
import android.util.Log

/**
 * An abstract [ProtoAdapter] that converts values of an enum to and from integers.
 */
actual abstract class EnumAdapter<E : WireEnum> protected actual constructor(
  type: KClass<E>
) : ProtoAdapter<E>(FieldEncoding.VARINT, type) {
  constructor(type: Class<E>) : this(type.kotlin)

  actual override fun encodedSize(value: E): Int = commonEncodedSize(value)

  fun encodedSize(value: Int): Int = commonEncodedSize(value)

  fun encodedSizeWithTag(tag: Int, value: Int?): Int {
    if (value == null) return 0
    var size = encodedSize(value)
    size += tagSize(tag)
//    Log.i("EnumAdapter", "encodedSizeWithTag " + size)
    return size
  }

  @Throws(IOException::class)
  actual override fun encode(writer: ProtoWriter, value: E) {
    commonEncode(writer, value)
  }

  fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Int?) {
    if (value == null) return
    writer.writeTag(tag, fieldEncoding)
    writer.writeVarint32(value)
  }

  @Throws(IOException::class)
  actual override fun decode(reader: ProtoReader): E = commonDecode(reader, this::fromValue)

  fun decode(value: Int): E = commonDecode(value, this::fromValue)

  actual override fun redact(value: E): E = commonRedact(value)

  /**
   * Converts an integer to an enum.
   * Returns null if there is no corresponding enum.
   */
  protected actual abstract fun fromValue(value: Int): E?
}
