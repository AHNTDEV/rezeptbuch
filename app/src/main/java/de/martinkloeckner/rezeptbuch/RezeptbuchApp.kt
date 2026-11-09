package de.martinkloeckner.rezeptbuch

import android.app.Application
import de.martinkloeckner.rezeptbuch.data.AppDatabase
import de.martinkloeckner.rezeptbuch.data.PhotoStore
import de.martinkloeckner.rezeptbuch.data.RecipeRepository

/** Manual dependency container – deliberately no DI framework. */
class AppContainer(app: Application) {
    val photos = PhotoStore(app)
    val repository = RecipeRepository(app, AppDatabase.create(app), photos)
}

class RezeptbuchApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
