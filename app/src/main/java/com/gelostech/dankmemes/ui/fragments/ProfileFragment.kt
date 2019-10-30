package com.gelostech.dankmemes.ui.fragments


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocosw.bottomsheet.BottomSheet
import com.gelostech.dankmemes.R
import com.gelostech.dankmemes.ui.activities.CommentActivity
import com.gelostech.dankmemes.ui.activities.ViewMemeActivity
import com.gelostech.dankmemes.ui.adapters.MemesAdapter
import com.gelostech.dankmemes.ui.base.BaseFragment
import com.gelostech.dankmemes.utils.Constants
import com.gelostech.dankmemes.utils.AppUtils
import com.gelostech.dankmemes.data.models.Fave
import com.gelostech.dankmemes.data.models.Meme
import com.gelostech.dankmemes.data.models.User
import com.gelostech.dankmemes.ui.callbacks.MemesCallback
import com.gelostech.dankmemes.utils.RecyclerFormatter
import com.gelostech.dankmemes.utils.hideView
import com.gelostech.dankmemes.utils.load
import com.gelostech.dankmemes.utils.showView
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.makeramen.roundedimageview.RoundedDrawable
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.fragment_profile.*
import org.jetbrains.anko.alert
import timber.log.Timber


class ProfileFragment : BaseFragment() {
    private lateinit var memesAdapter: MemesAdapter
    private lateinit var image: Bitmap
    private lateinit var user: User
    private lateinit var profileRef: DatabaseReference
    private lateinit var bs: BottomSheet.Builder
    private lateinit var loadMoreFooter: RelativeLayout
    private var lastDocument: DocumentSnapshot? = null
    private lateinit var query: Query
    private var loading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
//        load(true)

        profileRef = getDatabaseReference().child("users").child(getUid())
        profileRef.addValueEventListener(profileListener)
    }

    private fun initViews() {
        memesAdapter = MemesAdapter(memesCallback)

        profileRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(RecyclerFormatter.DoubleDividerItemDecoration(activity!!))
            itemAnimator = DefaultItemAnimator()
            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
            loadMoreFooterView as RelativeLayout
            adapter = memesAdapter
        }

        loadMoreFooter = profileRv.loadMoreFooterView as RelativeLayout
        profileRv.setOnLoadMoreListener {
            if (!loading) {
                loadMoreFooter.showView()
//                load(false)
            }
        }
    }

    private val profileListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Timber.e("Error loading profile: ${p0.message}")
        }

        override fun onDataChange(p0: DataSnapshot) {
            user = p0.getValue(User::class.java)!!

            profileName.text = user.userName
            profileBio.text = user.userBio
            profileImage.load(user.userAvatar!!, R.drawable.person)

            profileImage.setOnClickListener {
                temporarilySaveImage()

                val i = Intent(activity, ViewMemeActivity::class.java)
                i.putExtra(Constants.PIC_URL, user.userAvatar!!)
                AppUtils.fadeIn(activity!!)
            }
        }
    }

    private fun load(initial: Boolean) {
        query = if (lastDocument == null) {
            getFirestore().collection(Constants.MEMES)
                    .whereEqualTo(Constants.POSTER_ID, getUid())
                    .orderBy(Constants.TIME, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(15)
        } else {
            loading = true

            getFirestore().collection(Constants.MEMES)
                    .whereEqualTo(Constants.POSTER_ID, getUid())
                    .orderBy(Constants.TIME, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .startAfter(lastDocument!!)
                    .limit(15)
        }

        query.addSnapshotListener { p0, p1 ->
            hasPosts()
            loading = false

            if (p1 != null) {
                Timber.e( "Error loading initial memes: $p1")
            }

            if (p0 == null || p0.isEmpty) {
                if (initial) noPosts()
            } else {
                lastDocument = p0.documents[p0.size()-1]

                for (change: DocumentChange in p0.documentChanges) {
                    Timber.e("Loading changed document")

                    when(change.type) {
                        DocumentChange.Type.ADDED -> {
                            val meme = change.document.toObject(Meme::class.java)
                            memesAdapter.addMeme(meme)

                            Timber.e("Changed document: ADDED")
                        }

                        DocumentChange.Type.MODIFIED -> {
                            val meme = change.document.toObject(Meme::class.java)
                            memesAdapter.updateMeme(meme)

                            Timber.e("Changed document: MODIFIED")
                        }

                        DocumentChange.Type.REMOVED -> {
                            val meme = change.document.toObject(Meme::class.java)
                            memesAdapter.removeMeme(meme)
                        }

                    }
                }

            }

        }
    }

    private fun temporarilySaveImage() {
        image = (profileImage.drawable as BitmapDrawable).bitmap
        AppUtils.saveTemporaryImage(activity!!, image)
    }

    fun getUser() : User = user

    private val memesCallback = object : MemesCallback {
        override fun onMemeClicked(view: View, meme: Meme) {
            val memeId = meme.id!!

            // Get bitmap of shown meme
            val imageBitmap = when(view) {
                is RoundedImageView -> (view.drawable as RoundedDrawable).sourceBitmap
                else -> null
            }

            when(view.id) {
                R.id.memeMore -> showBottomSheet(meme, imageBitmap!!)
                R.id.memeLike -> likePost(memeId)
                R.id.memeComment -> showComments(memeId)
                R.id.memeFave -> favePost(memeId)
                else -> showMeme(meme, imageBitmap!!)
            }
        }
    }

    private fun showMeme(meme: Meme, image: Bitmap) {
        AppUtils.saveTemporaryImage(activity!!, image)

        val i = Intent(activity, ViewMemeActivity::class.java)
        i.putExtra(Constants.PIC_URL, meme.imageUrl)
        i.putExtra("caption", meme.caption)
        startActivity(i)
        AppUtils.fadeIn(activity!!)
    }

    private fun showBottomSheet(meme: Meme, image: Bitmap) {
        bs = BottomSheet.Builder(activity!!).sheet(R.menu.main_bottomsheet_me)

        bs.listener { _, which ->

            when(which) {
                R.id.bs_share -> AppUtils.shareImage(activity!!, image)
                R.id.bs_delete -> deletePost(meme)
                R.id.bs_save -> {
                    if (storagePermissionGranted()) {
                        AppUtils.saveImage(activity!!, image)
                    } else requestStoragePermission()
                }
            }

        }.show()

    }

    private fun showComments(memeId: String) {
        val i = Intent(activity, CommentActivity::class.java)
        i.putExtra("memeId", memeId)
        startActivity(i)
        activity?.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
    }

    private fun deletePost(meme: Meme) {
        activity!!.alert("Delete this meme?") {
            positiveButton("DELETE") {
                getFirestore().collection(Constants.MEMES).document(meme.id!!).delete()
            }
            negativeButton("CANCEL"){}
        }.show()
    }

    private fun likePost(id: String) {
        val docRef = getFirestore().collection(Constants.MEMES).document(id)

        getFirestore().runTransaction {

            val meme =  it[docRef].toObject(Meme::class.java)
            val likes = meme!!.likes
            var likesCount = meme.likesCount

            if (likes.containsKey(getUid())) {
                likesCount -= 1
                likes.remove(getUid())

            } else  {
                likesCount += 1
                likes[getUid()] = true
            }

            it.update(docRef, Constants.LIKES, likes)
            it.update(docRef, Constants.LIKES_COUNT, likesCount)

            return@runTransaction null
        }.addOnSuccessListener {
            Timber.e("Meme liked")
        }.addOnFailureListener {
            Timber.e("Error liking meme")
        }
    }

    private fun favePost(id: String) {
        val docRef = getFirestore().collection(Constants.MEMES).document(id)

        getFirestore().runTransaction {

            val meme =  it[docRef].toObject(Meme::class.java)
            val faves = meme!!.faves

            if (faves.containsKey(getUid())) {
                faves.remove(getUid())

                getFirestore().collection(Constants.FAVORITES).document(getUid()).collection(Constants.USER_FAVES).document(meme.id!!).delete()
            } else  {
                faves[getUid()] = true

                val fave = Fave()
                fave.id = meme.id!!
                fave.imageUrl = meme.imageUrl!!
                fave.time = meme.time!!

                getFirestore().collection(Constants.FAVORITES).document(getUid()).collection(Constants.USER_FAVES).document(meme.id!!).set(fave)
            }

            it.update(docRef, Constants.FAVES, faves)

            return@runTransaction null
        }.addOnSuccessListener {
            Timber.e("Meme faved")
        }.addOnFailureListener {
            Timber.e("Error faving meme")
        }
    }

    private fun hasPosts() {
        profileEmptyState?.hideView()
    }

    private fun noPosts() {
        profileEmptyState?.showView()
    }

    override fun onDestroy() {
        profileRef.removeEventListener(profileListener)
        super.onDestroy()
    }


}
