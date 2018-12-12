package com.bsuirlabs.softwaredesign


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.prof.rssparser.Article
import com.prof.rssparser.Parser
import kotlinx.android.synthetic.main.fragment_rss.*


class RssFragment : Fragment() {
    private var userProfile : UserProfile? = null

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

        val onClickListener: (String) -> Unit = { link ->
            val direction = RssFragmentDirections.actionRssFragmentToRssWebviewFragment(link)
            findNavController().navigate(direction)
        }

        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userProfile = dataSnapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    if (!userProfile!!.rssSource.isNullOrBlank()) {
                        val url = userProfile!!.rssSource!!
                        Parser().apply {
                            execute(url)
                            onFinish(object: Parser.OnTaskCompleted {
                                override fun onTaskCompleted(articles: ArrayList<Article>) {
                                    recyclerView?.adapter = ArticleAdapter(articles, onClickListener)
                                }
                                override fun onError() {
                                    Toast.makeText(context,"Error occurred while trying to load RSS feed", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    } else {
                        findNavController().navigate(R.id.action_rssFragment_to_changeRssSourceFragment)
                    }
                    progressBar?.visibility = View.INVISIBLE
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(ContentValues.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        FirebaseDatabase.getInstance().reference.child(currentUser!!.uid).addValueEventListener(userProfileListener)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.findItem(R.id.action_change_rss)?.isVisible = true
    }
}
