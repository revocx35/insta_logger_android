package io.github.revocx35.instalogger

import android.app.Application
import io.github.revocx35.instalogger.data.AppGraph
import io.github.revocx35.instalogger.data.Diagnostics
import io.github.revocx35.instalogger.media.AutoSaver
import io.github.revocx35.instalogger.media.MediaOps

class InstaLoggerApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val diagnostics = Diagnostics(this).also { it.install() }
        MediaOps.cleanup(this)
        graph = AppGraph(this, diagnostics)
        // Re-registers the periodic job if it was lost (e.g. after an app update); KEEP leaves a scheduled one alone.
        if (graph.prefs.autoSave) AutoSaver.schedule(this, true)
    }
}
