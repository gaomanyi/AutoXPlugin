package org.autojs.autojs.devplugin.i18n

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.LanguageBundle"

object I18n: DynamicBundle(BUNDLE) {
    fun msg(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}