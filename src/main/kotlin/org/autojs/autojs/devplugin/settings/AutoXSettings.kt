package org.autojs.autojs.devplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
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
        fun getInstance(project: Project): AutoXSettings = project.service()
    }
} 