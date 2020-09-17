package com.segihovav.wtfindexer_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.android.volley.BuildConfig
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import java.util.*

// TO DO/Fix
class MainActivity : AppCompatActivity(), OnRefreshListener {
    private var wtfIndexerURL: String? = null
    private val episodeList: MutableList<Episodes> = ArrayList()
    private var searchView: EditText? = null
    private var swipeController: SwipeController? = null
    private var sharedPreferences: SharedPreferences? = null
    private val darkMode = R.style.Theme_AppCompat_DayNight
    private val lightMode = R.style.ThemeOverlay_MaterialComponents

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        this.setTheme(if (sharedPreferences!!.getBoolean("DarkThemeOn", false)) darkMode else lightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set context which is used by SwipeController
        context = applicationContext

        // Internet connection is always required
        if (!isNetworkAvailable(this))
            alert("No Internet connection detected. Internet access is needed to use this app.", true)

        // Init the SwipeController
        val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)
        swipeController = SwipeController(object : SwipeControllerActions() {}, episodeList)

        // init swipe listener
        mSwipeRefreshLayout.setOnRefreshListener(this)
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, android.R.color.holo_green_dark, android.R.color.holo_orange_dark, android.R.color.holo_blue_dark)
        mSwipeRefreshLayout.setOnRefreshListener { loadJSONData() }

        wtfIndexerURL = if (sharedPreferences!!.getString("WTFIndexerURL", "") != "") sharedPreferences!!.getString("WTFIndexerURL", "") + (if (!sharedPreferences!!.getString("WTFIndexerURL", "")!!.endsWith("/")) "/" else "") else ""

        if (wtfIndexerURL == "") {
            alert("Please set the URL to your instance of WTFIndexer in Settings", false)
            return
        }

        searchView = findViewById(R.id.searchView)

        // Hide by default
        searchViewIsVisible(false)

        // Set the searchView icon based on the theme
        if (sharedPreferences!!.getBoolean("DarkThemeOn", false)) searchView!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_white, 0, 0, 0) else searchView!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_black, 0, 0, 0)

        // Search change event
        searchView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    val episodeListFiltered: MutableList<Episodes> = ArrayList()

                    for (i in episodeList.indices) {
                        // If the search term is contained in the name or the episode number
                        if (episodeList[i].name.toLowerCase(Locale.ROOT).contains(s.toString().toLowerCase(Locale.ROOT)) || episodeList[i].episodeNumber.toString().contains(s)) {
                            episodeListFiltered.add(episodeList[i])
                        }
                    }

                    // Call method that reloads the recycler view with the current data
                    initRecyclerView(episodeListFiltered)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)

        // Search search menu icon based on the current theme
        searchMenuItem.setIcon(if (sharedPreferences!!.getBoolean("DarkThemeOn", false)) R.drawable.search_white else R.drawable.search_black)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
       super.onConfigurationChanged(newConfig)

       val recyclerView: RecyclerView = findViewById(R.id.episodeList)

       if (recyclerView.adapter != null) recyclerView.adapter!!.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val episodeListFiltered: MutableList<Episodes>

        // Make search view hidden by default. It will be shown if needed
        searchViewIsVisible(false)

        if (id == R.id.action_settings) { // Settings menu
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.favoritesOnly) { // Favorites menu
            // Clear search field
            searchView!!.setText("")

            // toggle favorites checkbox
            item.isChecked = !item.isChecked

            // If favorites is checked, created filtered list of all episodes where favorites is checked
            if (item.isChecked) {
                episodeListFiltered = ArrayList()

                for (i in episodeList.indices) {
                    if (episodeList[i].favorite == 1) {
                        episodeListFiltered.add(episodeList[i])
                    }
                }

                // Call method that reloads the recycler view with the current data
                initRecyclerView(episodeListFiltered)
            } else { // Favorites is not checked
                initRecyclerView(episodeList) // Load the recyclerview with the full data set
            }
        } else if (id == R.id.updateEpisodes) {
            updateEpisodes()
        } else if (id == R.id.action_search) {
            searchViewIsVisible(true)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() { }

    // Fixes the issue that causes the swipe buttons to disappear when leaving the app
    public override fun onResume() {
        super.onResume()
        val recyclerView: RecyclerView = findViewById(R.id.episodeList)
        if (recyclerView.adapter != null) recyclerView.adapter!!.notifyDataSetChanged()
    }

    // Event when this activity returns from another activity
    public override fun onStart() {
        super.onStart()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Since onCreate() doesn't get called when returning from another activity, we have to set WTFIndexerURL here
        wtfIndexerURL = if (sharedPreferences.getString("WTFIndexerURL", "") != "") sharedPreferences.getString("WTFIndexerURL", "") + (if (!sharedPreferences.getString("WTFIndexerURL", "")!!.endsWith("/")) "/" else "") else ""

        if (wtfIndexerURL != "" && episodeList.size == 0) {
            loadJSONData()
        }
    }

    public override fun onStop() {
        super.onStop()

        // Hide by default
        searchViewIsVisible(false)
    }

    private fun alert(message: String, closeApp: Boolean) {
        // Display dialog
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id -> if (closeApp) finish() }
        val alert = builder.create()

        alert.show()
    }

    private fun initRecyclerView(arrayList: List<Episodes>) {
        val episodeNames: MutableList<String>
        val episodeInfo: MutableList<String>
        val adapter: EpisodeAdapter
        val layoutManager: RecyclerView.LayoutManager
        val mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)

        // Creates names array used as the item label for the RecyclerView
        episodeNames = ArrayList()
        episodeNames.clear()

        episodeInfo = ArrayList()
        episodeInfo.clear()

        for (i in arrayList.indices) {
            if (arrayList[i].imdbLink == null)
                episodeNames.add(arrayList[i].name)
            else
                episodeNames.add(arrayList[i].imdbLink)

            // Set value of episode info
            episodeInfo.add("#" + arrayList[i].episodeNumber + " " + arrayList[i].releaseDate.replace("Â",""))
        }

        // specify an adapter (see also next example)
        adapter = EpisodeAdapter(episodeNames, episodeInfo)
        adapter.notifyDataSetChanged()

        layoutManager = LinearLayoutManager(applicationContext)

        val recyclerView: RecyclerView = findViewById(R.id.episodeList)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()

        swipeController!!.setEpisodeList(arrayList)

        val itemTouchhelper = ItemTouchHelper(swipeController!!)
        itemTouchhelper.attachToRecyclerView(recyclerView)

        recyclerView.adapter = adapter
        registerForContextMenu(recyclerView)

        mSwipeRefreshLayout.isRefreshing = false
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) != null
        } catch (e: Exception) {
            true
        }
    }

    private fun loadJSONData() {
        val getEpisodesEndpoint = "WTF.php?FetchData"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val request = JsonArrayRequest(
                Request.Method.GET,
                wtfIndexerURL + getEpisodesEndpoint,
                null,
                Response.Listener { response ->
                    var jsonarray: JSONArray? = null
                    try {
                        jsonarray = JSONArray(response.toString())
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    // This is needed so that when the user pulls to refresh, all previous items are removed  to avoid duplicates
                    episodeList.clear()

                    if (BuildConfig.DEBUG && jsonarray == null) {
                        error("Assertion failed")
                    }

                    for (i in 0 until jsonarray!!.length()) {
                        try {
                            val jsonobject = jsonarray.getJSONObject(i)

                            episodeList.add(Episodes(jsonobject.getString("EpisodeID").toInt(), jsonobject.getString("Name"), jsonobject.getString("ReleaseDate"), jsonobject.getString("Favorite").toInt(), jsonobject.getString("IMDBLink"), java.lang.Boolean.parseBoolean(jsonobject.getString("IsCheckedOut"))))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    initRecyclerView(episodeList)
                },
                Response.ErrorListener {
                    //System.out.println("****** Error response=" + error.toString());
                })
        requestQueue.add(request)
    }

    // When showing the search EditText, move the entire swipe layout down and then move it back up when hiding the search
    private fun searchViewIsVisible(isHidden: Boolean) {
        val swipeControl = findViewById<SwipeRefreshLayout>(R.id.swipe_container)

        val marginLayoutParams = swipeControl.layoutParams as MarginLayoutParams
        marginLayoutParams.setMargins(marginLayoutParams.marginStart, if (isHidden) 130 else 0, marginLayoutParams.marginEnd, marginLayoutParams.bottomMargin)

        swipeControl.layoutParams = marginLayoutParams

        searchView = findViewById(R.id.searchView)
        searchView!!.visibility = if (!isHidden) View.GONE else View.VISIBLE
        searchView!!.requestFocus()
    }

    private fun updateEpisodes() {
        Toast.makeText(context, "Updating the episodes", Toast.LENGTH_LONG).show()

        val updateEpisodesEndpoint = "WTF.php?ScrapeData"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val request = JsonArrayRequest(
                Request.Method.GET,
                wtfIndexerURL + updateEpisodesEndpoint,
                null,
                Response.Listener { initRecyclerView(episodeList) },
                Response.ErrorListener {
                    //System.out.println("****** Error response=" + error.toString());
                })

        requestQueue.add(request)
    }

    // Supresses warning about the class property
    companion object {
        @JvmField
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }
}
