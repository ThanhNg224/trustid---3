/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.leeon.trustid.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import vn.leeon.trustid.R
import vn.leeon.trustid.databinding.FragmentCameraMrzBinding
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.view.CameraView
import vn.leeon.eidsdk.facade.EidFacade

class CameraMLKitFragment : CameraFragment() {

    private var isDecoding = false
    private var binding: FragmentCameraMrzBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCameraMrzBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
    }

    override fun onDetach() {
        super.onDetach()

    }

    override val callbackFrameProcessor: FrameProcessor
        get() = EidFacade.getCallbackFrameProcessor(0)

    override val cameraPreview: CameraView
        get() {
            return binding?.cameraPreview!!
        }

    override val requestedPermissions: ArrayList<String>
        get() {
            return ArrayList<String>()
        }

    override fun onRequestPermissionsResult(
        permissionsDenied: ArrayList<String>,
        permissionsGranted: ArrayList<String>
    ) {
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.permission_camera_rationale))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showToast(text: String) {
        val activity = activity
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    class ErrorDialog : androidx.fragment.app.DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { dialogInterface, i -> activity!!.finish() }
                .create()
        }

        companion object {

            private val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }

    class ConfirmationDialog : androidx.fragment.app.DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val parent = parentFragment
            return AlertDialog.Builder(activity)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    parent!!.requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { dialog, which ->
                    val activity = parent!!.activity
                    activity?.finish()
                }
                .create()
        }
    }

    companion object {
        private val TAG = CameraMLKitFragment::class.java.simpleName

        private val REQUEST_CAMERA_PERMISSION = 1
        private val FRAGMENT_DIALOG = "CameraMLKitFragment"

        fun newInstance(): CameraMLKitFragment {
            return CameraMLKitFragment()
        }
    }
}
