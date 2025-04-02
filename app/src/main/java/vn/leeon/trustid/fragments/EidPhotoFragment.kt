package vn.leeon.trustid.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import vn.leeon.trustid.common.IntentData
import vn.leeon.trustid.databinding.FragmentEidPhotoBinding

class EidPhotoFragment : androidx.fragment.app.Fragment() {

    private var eidPhotoFragmentListener: EidPhotoFragmentListener? = null
    private var bitmap: Bitmap? = null
    private var binding:FragmentEidPhotoBinding?=null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentEidPhotoBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_IMAGE)) {
            bitmap = arguments.getParcelable(IntentData.KEY_IMAGE)
        } else {
            //error
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(bitmap)
    }

    private fun refreshData(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        binding?.image?.setImageBitmap(bitmap)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is EidPhotoFragmentListener) {
            eidPhotoFragmentListener = activity
        }
    }

    override fun onDetach() {
        eidPhotoFragmentListener = null
        super.onDetach()

    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    interface EidPhotoFragmentListener

    companion object {

        fun newInstance(bitmap: Bitmap): EidPhotoFragment {
            val myFragment = EidPhotoFragment()
            val args = Bundle()
            args.putParcelable(IntentData.KEY_IMAGE, bitmap)
            myFragment.arguments = args
            return myFragment
        }
    }

}