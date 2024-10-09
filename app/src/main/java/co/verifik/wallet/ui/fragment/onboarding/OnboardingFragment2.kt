package co.verifik.wallet.ui.fragment.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.verifik.wallet.R


/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingFragment2.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingFragment2 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_onboarding2, container, false)
    }
}