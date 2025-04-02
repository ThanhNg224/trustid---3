package vn.leeon.trustid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import vn.leeon.trustid.R
import vn.leeon.trustid.activities.SelectionActivity

class TestFragment : Fragment() {
    private lateinit var imageView: ImageView

    private val activity: SelectionActivity by lazy {
        requireActivity() as SelectionActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_test,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageView = view.findViewById(R.id.image_view)
        imageView.setImageBitmap(activity.frontBitmap)
    }
}