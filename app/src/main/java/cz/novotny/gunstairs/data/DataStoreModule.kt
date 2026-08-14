package cz.novotny.gunstairs.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// A single small preferences file backs both best score and settings: at this
// scale (one int, one bool) a second DataStore file would add nothing. Room is
// deliberately not used here either — see README for that decision.
internal val Context.gunStairsDataStore by preferencesDataStore(name = "gunstairs_prefs")
