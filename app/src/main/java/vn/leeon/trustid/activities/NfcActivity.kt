package vn.leeon.trustid.activities


import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonNull
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.eidsdk.data.Eid
import vn.leeon.eidsdk.facade.EidFacade
import vn.leeon.eidsdk.jmrtd.VerificationStatus
import vn.leeon.eidsdk.network.EidService.EIDSERVICE
import vn.leeon.eidsdk.network.models.EidVerifyModel
import vn.leeon.eidsdk.network.models.ResponseModel
import vn.leeon.eidsdk.network.models.RestCallback
import vn.leeon.eidsdk.utils.StringUtils
import vn.leeon.trustid.BuildConfig
import vn.leeon.trustid.R
import vn.leeon.trustid.common.IntentData
import vn.leeon.trustid.fragments.EidDetailsFragment
import vn.leeon.trustid.fragments.EidPhotoFragment
import vn.leeon.trustid.fragments.NfcFragment
import vn.leeon.trustid.utils.serializable

class NfcActivity : androidx.fragment.app.FragmentActivity(), NfcFragment.NfcFragmentListener,
    EidDetailsFragment.EidDetailsFragmentListener, EidPhotoFragment.EidPhotoFragmentListener {

    private var mrzInfo: MRZInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        if (intent.hasExtra(IntentData.KEY_MRZ_INFO)) {
            mrzInfo = intent.serializable(IntentData.KEY_MRZ_INFO)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NfcFragment.newInstance(mrzInfo!!), TAG_NFC)
                .commit()
        }
    }

    /////////////////////////////////////////////////////
    //
    //  NFC Fragment events
    //
    /////////////////////////////////////////////////////

    override fun onEidRead(eid: Eid?) {
        val eidNumber = eid?.personOptionalDetails?.eidNumber
        val dsCert = StringUtils.encodeToBase64String(eid?.sodFile?.docSigningCertificate)
        val province = StringUtils.getProvince(eid?.personOptionalDetails?.placeOfOrigin)
        val code = BuildConfig.CUSTOMER_CODE;
        EIDSERVICE.verifyEid(
            eidNumber, dsCert, province, code,
            object : RestCallback<ResponseModel<EidVerifyModel>>(this) {
                override fun Success(model: ResponseModel<EidVerifyModel>?) {
                    val respondsMsg = Gson().toJson(model?.data?.responds ?: JsonNull.INSTANCE)
                    val signature = model?.data?.signature ?: ""
                    val checkSignature = EidFacade.verifyRsaSignatureDefault(
                        baseContext, respondsMsg, signature
                    )

                    if (!checkSignature) {
                        eid?.verificationStatus?.setCS(
                            VerificationStatus.Verdict.FAILED,
                            "Invalid Signature",
                            null
                        )
                        showFragmentDetails(eid!!)
                        return
                    }

                    if (model?.data?.IsValidIdCard == true) {
                        eid?.verificationStatus?.setCS(
                            VerificationStatus.Verdict.SUCCEEDED,
                            "Verified DSCert",
                            null
                        )
                    } else {
                        eid?.verificationStatus?.setCS(
                            VerificationStatus.Verdict.FAILED,
                            "Invalid DSCert",
                            null
                        )
                    }
                    showFragmentDetails(eid!!)
                }

                override fun Error(error: String?) {
                    eid?.verificationStatus?.setCS(
                        VerificationStatus.Verdict.FAILED,
                        "Cannot contact the verification server, please try again",
                        null
                    )
                    showFragmentDetails(eid!!)
                }
            }
        )
    }

    override fun onCardException(cardException: Exception?) {
        //Toast.makeText(this, cardException.toString(), Toast.LENGTH_SHORT).show();
        //onBackPressed();
    }

    private fun showFragmentDetails(eid: Eid) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, EidDetailsFragment.newInstance(eid))
            .addToBackStack(TAG_PASSPORT_DETAILS)
            .commit()
    }

    private fun showFragmentPhoto(bitmap: Bitmap) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, EidPhotoFragment.newInstance(bitmap))
            .addToBackStack(TAG_PASSPORT_PICTURE)
            .commit()
    }

    override fun onImageSelected(bitmap: Bitmap?) {
        showFragmentPhoto(bitmap!!)
    }

    companion object {
        private val TAG = NfcActivity::class.java.simpleName
        private val TAG_NFC = "TAG_NFC"
        private val TAG_PASSPORT_DETAILS = "TAG_PASSPORT_DETAILS"
        private val TAG_PASSPORT_PICTURE = "TAG_PASSPORT_PICTURE"
    }
}
