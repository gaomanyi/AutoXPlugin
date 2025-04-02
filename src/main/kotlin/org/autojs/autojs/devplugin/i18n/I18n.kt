package org.autojs.autojs.devplugin.i18n

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

//object I18n {
//    private val bundle: ResourceBundle by lazy {
//        ResourceBundle.getBundle("messages", Locale.getDefault())
//    }
//
//    fun get(key: String): String = try {
//        bundle.getString(key)
//    } catch (e: MissingResourceException) {
//        "!$key!"
//    }
//}
private const val BUNDLE = "messages.LanguageBundle"


object I18n: DynamicBundle(BUNDLE) {


    fun msg(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

}