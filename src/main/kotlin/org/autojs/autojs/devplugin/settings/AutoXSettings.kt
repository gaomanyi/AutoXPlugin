package org.autojs.autojs.devplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "org.autojs.autojs.devplugin.settings.AutoXSettings",
    storages = [Storage("AutoXPluginSettings.xml")]
)
class AutoXSettings : PersistentStateComponent<AutoXSettings.State> {
    private var myState = State()
    
    data class State(
        var port: Int = 9317,
        var autoStartServer: Boolean = false,
        var lastConnectedDevices: List<String> = emptyList()
    )
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    companion object {
        fun getInstance(): AutoXSettings = ApplicationManager.getApplication().service()
    }
} 