package com.gelostech.dankmemes.fragments


import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gelostech.dankmemes.R
import com.gelostech.dankmemes.activities.MainActivity
import com.gelostech.dankmemes.commoners.BaseFragment
import com.gelostech.dankmemes.commoners.DankMemesUtil
import com.gelostech.dankmemes.commoners.DankMemesUtil.drawableToBitmap
import com.gelostech.dankmemes.commoners.DankMemesUtil.setDrawable
import com.gelostech.dankmemes.utils.replaceFragment
import com.gelostech.dankmemes.utils.setDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.mikepenz.ionicons_typeface_library.Ionicons
import kotlinx.android.synthetic.main.fragment_login.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast


class LoginFragment : BaseFragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signupSuccessful: Bitmap

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        firebaseAuth = FirebaseAuth.getInstance()
        val successfulIcon = setDrawable(activity!!, Ionicons.Icon.ion_checkmark_round, R.color.white, 25)
        signupSuccessful = drawableToBitmap(successfulIcon)

        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginEmail.setDrawable(setDrawable(activity!!, Ionicons.Icon.ion_ios_email, R.color.secondaryText, 18))
        loginPassword.setDrawable(setDrawable(activity!!, Ionicons.Icon.ion_android_lock, R.color.secondaryText, 18))

        loginRegister.setOnClickListener { (activity as AppCompatActivity).replaceFragment(SignupFragment(), R.id.loginHolder) }

        loginButton.setOnClickListener { signIn() }
        loginForgotPassword.setOnClickListener { forgotPassword() }
    }

    private fun signIn() {
        if (!DankMemesUtil.validated(loginEmail, loginPassword)) return

        val email = loginEmail.text.toString().trim()
        val pw = loginPassword.text.toString().trim()

        loginButton.startAnimation()
        firebaseAuth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener(activity!!, { task ->
                    if (task.isSuccessful) {
                        loginButton.doneLoadingAnimation(DankMemesUtil.getColor(activity!!, R.color.pink), signupSuccessful)
                        Log.e(TAG, "signingIn: Success!")

                        // update UI with the signed-in user's information
                        val user = firebaseAuth.currentUser
                        updateUI(user!!)
                    } else {
                        try {
                            throw task.exception!!
                        } catch (wrongPassword: FirebaseAuthInvalidCredentialsException) {
                            loginButton.revertAnimation()
                            loginPassword.error = "Password incorrect"

                        } catch (userNull: FirebaseAuthInvalidUserException) {
                            loginButton.revertAnimation()
                            activity?.toast("Account not found. Have you signed up?")

                        } catch (e: Exception) {
                            loginButton.revertAnimation()
                            Log.e(TAG, "signingIn: Failure - ${e.localizedMessage}" )
                            activity?.toast("Error signing in. Please try again.")
                        }
                    }
                })

    }

    private fun forgotPassword() {
        if (!DankMemesUtil.validated(loginEmail)) return

        val email = loginEmail.text.toString().trim()

        activity?.alert("Instructions to reset your password will be sent to $email") {
            title = "Forgot password"

            positiveButton("SEND EMAIL") {

                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(activity!!, { task ->
                            if (task.isSuccessful) {
                                Log.e(TAG, "sendResetPassword: Success!")

                                activity?.toast("Email sent")
                            } else {
                                try {
                                    throw task.exception!!
                                } catch (malformedEmail: FirebaseAuthInvalidCredentialsException) {
                                    loginEmail.error = "Incorrect email format"
                                    activity?.toast("Email not sent. Please try again.")

                                } catch (e: Exception) {
                                    Log.e(TAG, "sendResetEmail: Failure - $e" )
                                    activity?.toast("Email not sent. Please try again.")
                                }
                            }
                        })

            }

            negativeButton("CANCEL") {}
        }!!.show()
    }

    private fun updateUI(user: FirebaseUser) {
        Handler().postDelayed({
            startActivity(Intent(activity!!, MainActivity::class.java))
            activity!!.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
            activity!!.finish()
        }, 400)
    }

    override fun onDestroy() {
        if (loginButton != null) loginButton.dispose()
        super.onDestroy()
    }

}