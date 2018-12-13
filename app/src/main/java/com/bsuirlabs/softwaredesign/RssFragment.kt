package com.bsuirlabs.softwaredesign


import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prof.rssparser.Article
import com.prof.rssparser.Parser
import kotlinx.android.synthetic.main.fragment_rss.*
import java.io.IOException


class RssFragment : Fragment() {
    private val CachedArticlesCount = 10
    private var userProfile : UserProfile? = null

    private val onClickListener: (String) -> Unit = { link ->
        val direction = RssFragmentDirections.actionRssFragmentToRssWebviewFragment(link)
        findNavController().navigate(direction)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rss, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(activity, spanCount)
        recyclerView.setHasFixedSize(true)
        if (isOnline()) {
            loadArticles()
        } else {
            setCachedArticles()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.findItem(R.id.action_change_rss)?.isVisible = true
    }

    private fun setCachedArticles(){
        val articles = activity?.getSharedPreferences("data", MODE_PRIVATE)?.getString("articles", null)
        if (articles.isNullOrBlank()){
            progressBar?.visibility = View.INVISIBLE
            Toast.makeText(activity, getString(R.string.no_internet_connection_and_cached_articles), Toast.LENGTH_LONG).show()
            return
        }
        val array = Gson().fromJson<ArrayList<Article>>(articles, object:TypeToken<ArrayList<Article>>() {}.type)
        recyclerView.adapter = ArticleAdapter(array, onClickListener)
        progressBar?.visibility = View.INVISIBLE
    }

    private fun loadArticles() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userProfile = dataSnapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    if (!userProfile?.rssSource.isNullOrBlank()) {
                        val url = userProfile?.rssSource.toString()
                        parseRSSArticles(url)
                    } else {
                        findNavController().navigate(R.id.action_rssFragment_to_changeRssSourceFragment)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                showError()
            }
        }
        FirebaseDatabase.getInstance().reference.child(currentUser?.uid.toString()).addValueEventListener(userProfileListener)
    }

    private fun isOnline() : Boolean{
        val runtime = Runtime.getRuntime()
        try {
            val ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = ipProcess.waitFor()
            return (exitValue == 0)
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        catch (e:InterruptedException) {
            e.printStackTrace()
        }
        return false
    }

    private fun parseRSSArticles(url : String){
        Parser().apply {
            execute(url)
            onFinish(object : Parser.OnTaskCompleted {
                override fun onTaskCompleted(articles: ArrayList<Article>) {
                    val serializedArticles = Gson().toJson(articles.take(CachedArticlesCount))
                    activity?.getSharedPreferences("data", MODE_PRIVATE)
                            ?.edit()
                            ?.putString("articles", serializedArticles)
                            ?.apply()
                    recyclerView?.adapter = ArticleAdapter(articles, onClickListener)
                    progressBar?.visibility = View.INVISIBLE
                }

                override fun onError() {
                    showError()
                }
            })
        }
    }

    private fun showError(){
        Toast.makeText(activity, "Error occurred while trying to load RSS feed", Toast.LENGTH_SHORT).show()
        progressBar?.visibility = View.INVISIBLE
    }
}
