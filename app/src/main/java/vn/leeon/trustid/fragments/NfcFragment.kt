package vn.leeon.trustid.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import net.sf.scuba.smartcards.CardServiceException
import net.sf.scuba.smartcards.ISO7816
import org.jmrtd.BACDeniedException
import org.jmrtd.PACEException
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.eidsdk.data.Eid
import vn.leeon.eidsdk.facade.EidCallback
import vn.leeon.eidsdk.facade.EidFacade
import vn.leeon.eidsdk.jmrtd.VerificationStatus
import vn.leeon.trustid.R
import vn.leeon.trustid.activities.SelectionActivity
import vn.leeon.trustid.common.IntentData
import vn.leeon.trustid.databinding.FragmentNfcBinding
import java.security.Security

class NfcFragment(private val mrzInfo: MRZInfo) : Fragment() {
    private var mHandler = Handler(Looper.getMainLooper())
    private var disposable = CompositeDisposable()
    private val activity: SelectionActivity by lazy {
        requireActivity() as SelectionActivity
    }

    private var binding: FragmentNfcBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNfcBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleNfcTag()
    }

    private fun handleNfcTag() {
        val subscribe = EidFacade.handleDocumentNfcTag(
            requireContext(), mrzInfo,
            object : EidCallback {
                override fun onEidReadStart() {
                    onNFCSReadStart()
                }

                override fun onEidReadFinish() {
                    onNFCReadFinish()
                }

                override fun onEidRead(passport: Eid?) {
                    onNFCRead(passport)
                }

                override fun onAccessDeniedException(exception: org.jmrtd.AccessDeniedException) {
                    Toast.makeText(
                        context,
                        getString(R.string.warning_authentication_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    exception.printStackTrace()
                    this@NfcFragment.onCardException(exception)
                }

                override fun onBACDeniedException(exception: BACDeniedException) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                    this@NfcFragment.onCardException(exception)
                }

                override fun onPACEException(exception: PACEException) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                    this@NfcFragment.onCardException(exception)
                }

                override fun onCardException(exception: CardServiceException) {
                    when (exception.sw.toShort()) {
                        ISO7816.SW_CLA_NOT_SUPPORTED -> {
                            Toast.makeText(
                                context,
                                getString(R.string.warning_cla_not_supported),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                    this@NfcFragment.onCardException(exception)
                }

                override fun onGeneralException(exception: Exception?) {
                    Toast.makeText(context, exception!!.toString(), Toast.LENGTH_SHORT).show()
                    this@NfcFragment.onCardException(exception)
                }
            })

        disposable.add(subscribe)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        val activity = activity
//        if (activity is NfcFragmentListener) {
//            nfcFragmentListener = activity
//        }
    }

    override fun onDetach() {
//        nfcFragmentListener = null
        super.onDetach()
    }


    override fun onResume() {
        super.onResume()
        activity.step = 3
        binding?.valuePassportNumber?.text =
            getString(R.string.doc_number, mrzInfo!!.documentNumber)
        binding?.valueDOB?.text = getString(R.string.doc_dob, mrzInfo!!.dateOfBirth)
        binding?.valueExpirationDate?.text = getString(R.string.doc_expiry, mrzInfo!!.dateOfExpiry)
    }

    override fun onDestroyView() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        binding = null
        super.onDestroyView()
    }

    private fun onNFCSReadStart() {
        mHandler.post { binding?.progressBar?.visibility = View.VISIBLE }
    }

    private fun onNFCReadFinish() {
        mHandler.post { binding?.progressBar?.visibility = View.GONE }
    }

    private fun onCardException(cardException: Exception?) {
        mHandler.post {
//            if (nfcFragmentListener != null) {
//                nfcFragmentListener?.onCardException(cardException)
//            }
        }
    }

    private fun onNFCRead(eid: Eid?) {
        mHandler.post {
            eid?.verificationStatus?.setCS(
                VerificationStatus.Verdict.SUCCEEDED,
                "Verified DSCert",
                null
            )
            activity.showFragmentDetails(eid!!)
//            val eidNumber = eid?.personOptionalDetails?.eidNumber
//            val dsCert = StringUtils.encodeToBase64String(eid?.sodFile?.docSigningCertificate)
//            val province = StringUtils.getProvince(eid?.personOptionalDetails?.placeOfOrigin)
//            val code = BuildConfig.CUSTOMER_CODE;
//            EidService.EIDSERVICE.verifyEid(
//                eidNumber, dsCert, province, code,
//                object : RestCallback<ResponseModel<EidVerifyModel>>(this) {
//                    override fun Success(model: ResponseModel<EidVerifyModel>?) {
//                        val respondsMsg = Gson().toJson(model?.data?.responds ?: JsonNull.INSTANCE)
//                        val signature = model?.data?.signature ?: ""
//                        val checkSignature = EidFacade.verifyRsaSignatureDefault(
//                            requireContext(), respondsMsg, signature
//                        )
//
////                        if (!checkSignature) {
////                            eid?.verificationStatus?.setCS(
////                                VerificationStatus.Verdict.FAILED,
////                                "Invalid Signature",
////                                null
////                            )
////                            activity.showFragmentDetails(eid!!)
////                            return
////                        }
////
////                        if (model?.data?.IsValidIdCard == true) {
//                            eid?.verificationStatus?.setCS(
//                                VerificationStatus.Verdict.SUCCEEDED,
//                                "Verified DSCert",
//                                null
//                            )
////                        } else {
////                            eid?.verificationStatus?.setCS(
////                                VerificationStatus.Verdict.FAILED,
////                                "Invalid DSCert",
////                                null
////                            )
////                        }
//                        activity.showFragmentDetails(eid!!)
//                    }
//
//                    override fun Error(error: String?) {
//                        eid?.verificationStatus?.setCS(
//                            VerificationStatus.Verdict.FAILED,
//                            "Cannot contact the verification server, please try again",
//                            null
//                        )
//                        activity.showFragmentDetails(eid!!)
//                    }
//                }
//            )
        }
    }

    interface NfcFragmentListener {
        fun onEidRead(eid: Eid?)
        fun onCardException(cardException: Exception?)
    }

    companion object {
        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }

        fun newInstance(mrzInfo: MRZInfo): NfcFragment {
            val myFragment = NfcFragment(mrzInfo)
            val args = Bundle()
            args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo)
            myFragment.arguments = args
            return myFragment
        }
    }
}
