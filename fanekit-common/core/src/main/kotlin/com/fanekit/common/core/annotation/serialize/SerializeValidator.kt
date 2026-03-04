package com.fanekit.common.core.annotation.serialize

import com.fanekit.common.core.exception.Throw
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.full.memberProperties

/**
 * 序列化逻辑校验工具
 * 校验对象的字段是否符合注解要求
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/24
 * @since 1.0.0
 * @version 1.0.0
 */
object SerializeValidator {
    private val log = LoggerFactory.getLogger("Validator")
    fun <T : Any> validate(obj: T) {
        // 校验字段
        obj::class.memberProperties.forEach { member ->
            val value = member.getter.call(obj)
            member.annotations.forEach { annotation ->
                log.info("annotation: $annotation")
                when (annotation) {
                    is NotNull -> {
                        if (value == null) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is NotEmpty -> {
                        if (value == null) {
                            throw Throw.paramInvalid(annotation.message)
                        } else if (value is String && (value.isEmpty())) {
                            throw Throw.paramInvalid(annotation.message)
                        } else if (value is Collection<*> && value.isEmpty()) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is NotBlank -> {
                        if (value is String && value.isBlank()) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Min -> {
                        if (value is Int && value < annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is Long && value < annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is String && value.length < annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Max -> {
                        if (value is Int && value > annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is Long && value > annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is String && value.length > annotation.value) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Pattern -> {
                        if (value is String && value.length > 0 && !Regex(annotation.regex).matches(value)) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Size -> {
                        if (value is Int && (value < annotation.min || value > annotation.max)) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is Long && (value < annotation.min || value > annotation.max)) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                        else if (value is String && (value.length < annotation.min || value.length > annotation.max)) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Future -> {
                        if (value is Date && value.before(Date())) {
                            throw Throw.paramInvalid(annotation.message)
                        } else if (value is java.time.LocalDateTime && value.isBefore(java.time.LocalDateTime.now())) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                    is Past -> {
                        if (value is Date && value.after(Date())) {
                            throw Throw.paramInvalid(annotation.message)
                        } else if (value is java.time.LocalDateTime && value.isAfter(java.time.LocalDateTime.now())) {
                            throw Throw.paramInvalid(annotation.message)
                        }
                    }
                }
            }
        }
    }
}