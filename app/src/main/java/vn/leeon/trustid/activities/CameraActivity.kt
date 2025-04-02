package vn.leeon.trustid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.eidsdk.facade.CameraOcrCallback
import vn.leeon.eidsdk.facade.EidFacade
import vn.leeon.trustid.R
import vn.leeon.trustid.common.IntentData
import vn.leeon.trustid.fragments.CameraMLKitFragment

class CameraActivity : AppCompatActivity(), CameraOcrCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        EidFacade.registerOcrListener(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, CameraMLKitFragment())
            .commit()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onEidRead(mrzInfo: MRZInfo) {
        val intent = Intent()
        intent.putExtra(IntentData.KEY_MRZ_INFO, mrzInfo)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onError(e: Exception?) {
        Toast.makeText(baseContext, e?.message, Toast.LENGTH_SHORT).show()
    }
}
