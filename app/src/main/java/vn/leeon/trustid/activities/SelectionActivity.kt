package vn.leeon.trustid.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.eidsdk.data.Eid
import vn.leeon.eidsdk.network.EidService.EIDSERVICE
import vn.leeon.trustid.BuildConfig
import vn.leeon.trustid.R
import vn.leeon.trustid.common.IntentData
import vn.leeon.trustid.fragments.BackIDCardFragment
import vn.leeon.trustid.fragments.EidDetailsFragment
import vn.leeon.trustid.fragments.FrontIDCardFragment
import vn.leeon.trustid.fragments.NfcFragment
import vn.leeon.trustid.fragments.SelectionFragment
import vn.leeon.trustid.utils.serializable

class SelectionActivity : AppCompatActivity(), SelectionFragment.SelectionFragmentListener {

    private lateinit var progressBar: ProgressBar
    var backBitmap: Bitmap? = null
    var frontBitmap: Bitmap? = null
    var step: Int = 1
        set(value) {
            val pValue = 100 / 4 * value
            progressBar.setProgress(pValue, true)
            field = value
        }
    private lateinit var cameraFragment: FrontIDCardFragment
//    private val mManager: CardManager by lazy { CardManager.instance }

    override fun onResume() {
        super.onResume()

//        val out1: String = hexStringToBytes(res.replace(" ", "")).contentToString()
//        res = mManager.apdu(bytesToHexString(cmd2))
//        val out2: String = hexStringToBytes(res.replace(" ", "")).contentToString()
//        res = mManager.apdu(bytesToHexString(cmd3))
//        val out3: String = Arrays.toString(hexStringToBytes(res.replace(" ", "")))
//        Log.d("TAG", "onClick: read " + Arrays.toString(cmd1) + " " + out1)
//        Log.d("TAG", "onClick: read " + Arrays.toString(cmd2) + " " + out2)
//        Log.d("TAG", "onClick: read " + Arrays.toString(cmd3) + " " + out3)
    }

    fun hexStringToBytes(string: String): ByteArray {
        return string.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun bytesToHexString(bytes: ByteArray): String {
        val res = StringBuilder()
        for (ch in bytes) {
            val ch1 = ch.toInt() shr 4 and 0x0000000f
            val ch2 = ch.toInt() and 0x0000000f
            res.append(Integer.toHexString(ch1)).append(Integer.toHexString(ch2))
        }
        return res.toString()
    }

    override fun onPause() {
        super.onPause()
//        mManager.stop()
//        mManager.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.setProgressTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_light
                )
            )
        );
        if (allPermissionsGranted()) {
            if (null == savedInstanceState) showFrontIDCardFragment()
            EIDSERVICE.init(BuildConfig.API_KEY, BuildConfig.API_BASE_URL)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        onBackPressedDispatcher.addCallback(this) {
            when (step) {
                1 -> finish()
                2 -> {
                    step = 1
                    cameraFragment.checkTitle()
                }

                3 -> {
                    step = 1
                    supportFragmentManager.popBackStack()
                    cameraFragment.checkTitle()
                }

                else -> supportFragmentManager?.let {
                    step = 1
                    it.popBackStack(
                        it.getBackStackEntryAt(0).id,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    cameraFragment.checkTitle()
                }
            }
        }


//        val open = mManager.open(this)
//        mManager.reset()
//        Log.d(TAG, "onResume: $open")
//        var res:String? = ""
//        val cmd1 = byteArrayOf(0, -92, 2, 12, 2, 1, 28)
//        val cmd2 = byteArrayOf(0, -80, 0, 0, 8)
//        val cmd3 = byteArrayOf(0, -80, 0, 8, 46)
////        res = mManager.apdu(bytesToHexString(cmd1))
//        val res1=mManager.apdu(cmd1)
//        Log.d(TAG, "onResume: ${bytesToHexString(cmd1)} ${bytesToHexString(res1)} ${hexStringToBytes(bytesToHexString(res1)).contentToString()}")
//        Log.d(TAG, "onResume: ${mManager.apdu(cmd2).contentToString()}")
//        Log.d(TAG, "onResume: ${mManager.apdu(cmd3).contentToString()}")
    }

    private fun showFrontIDCardFragment() {
        cameraFragment = FrontIDCardFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.container, cameraFragment)
            .commit()
    }

    fun showBackIDCardFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, BackIDCardFragment(), TAG_SELECTION_FRAGMENT)
            .addToBackStack(TAG_SELECTION_FRAGMENT)
            .commit()
    }

    fun showFragmentDetails(eid: Eid) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, EidDetailsFragment.newInstance(eid))
            .addToBackStack(null)
            .commit()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val nonNullData = data ?: Intent()
        when (requestCode) {
            REQUEST_MRZ -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        onEidRead(nonNullData.serializable(IntentData.KEY_MRZ_INFO)!!)
                    }

                    Activity.RESULT_CANCELED -> {
                        val fragmentByTag = supportFragmentManager
                            .findFragmentByTag(TAG_SELECTION_FRAGMENT)
                        if (fragmentByTag is SelectionFragment) {
                            fragmentByTag.selectManualToggle()
                        }
                    }

                    else -> {
                        val fragmentByTag = supportFragmentManager
                            .findFragmentByTag(TAG_SELECTION_FRAGMENT)
                        if (fragmentByTag is SelectionFragment) {
                            fragmentByTag.selectManualToggle()
                        }
                    }
                }
            }

            REQUEST_NFC -> {
                val fragmentByTag = supportFragmentManager
                    .findFragmentByTag(TAG_SELECTION_FRAGMENT)
                if (fragmentByTag is SelectionFragment) {
                    fragmentByTag.selectManualToggle()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, nonNullData)
    }

    override fun onEidRead(mrzInfo: MRZInfo) {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, NfcFragment(mrzInfo), TAG_SELECTION_FRAGMENT)
            .addToBackStack(TAG_SELECTION_FRAGMENT)
            .commit()
    }

    override fun onMrzRequest() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, REQUEST_MRZ)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showFrontIDCardFragment()
        } else {
            Toast.makeText(baseContext, "denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val TAG = SelectionActivity::class.java.simpleName
        private const val TAG_SELECTION_FRAGMENT = "TAG_SELECTION_FRAGMENT"
        private const val TAG_PASSPORT_DETAILS = "TAG_PASSPORT_DETAILS"
        private const val REQUEST_MRZ = 12
        private const val REQUEST_NFC = 11
    }
}
