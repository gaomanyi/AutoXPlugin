package org.autojs.autojs.devplugin.message

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Gson配置类，提供自定义配置的Gson实例
 */
object GsonConfig {
    
    /**
     * 创建配置了自定义枚举适配器的Gson实例
     * 默认情况下，Gson不会序列化null值字段
     * @return 配置好的Gson实例
     */
    fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(TotalType::class.java, TotalTypeAdapter())
            .registerTypeAdapter(CommandType::class.java, CommandTypeAdapter())
            .create()
    }
    
    /**
     * 创建配置了自定义枚举适配器的Gson实例，并序列化null值
     * @return 配置好的Gson实例
     */
    fun createGsonWithNulls(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(TotalType::class.java, TotalTypeAdapter())
            .registerTypeAdapter(CommandType::class.java, CommandTypeAdapter())
            .serializeNulls()  // 序列化null值
            .create()
    }
    
    /**
     * TotalType枚举的自定义适配器，序列化时使用value属性值
     */
    private class TotalTypeAdapter : TypeAdapter<TotalType>() {
        override fun write(out: JsonWriter, value: TotalType?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value.value)
            }
        }
        
        override fun read(reader: JsonReader): TotalType? {
            if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            
            val stringValue = reader.nextString()
            return TotalType.values().find { it.value == stringValue }
        }
    }
    
    /**
     * CommandType枚举的自定义适配器，序列化时使用value属性值
     */
    private class CommandTypeAdapter : TypeAdapter<CommandType>() {
        override fun write(out: JsonWriter, value: CommandType?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value.value)
            }
        }
        
        override fun read(reader: JsonReader): CommandType? {
            if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            
            val stringValue = reader.nextString()
            return CommandType.values().find { it.value == stringValue }
        }
    }
} 