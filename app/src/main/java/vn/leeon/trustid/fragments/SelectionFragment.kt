package vn.leeon.trustid.fragments


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.MRZInfo
import vn.leeon.trustid.R
import vn.leeon.trustid.databinding.FragmentSelectionBinding
import vn.leeon.trustid.validators.DateRule
import vn.leeon.trustid.validators.DocumentNumberRule
import java.security.Security

class SelectionFragment : androidx.fragment.app.Fragment(), Validator.ValidationListener {

    private var radioGroup: RadioGroup? = null
    private var linearLayoutManual: LinearLayout? = null
    private var linearLayoutAutomatic: LinearLayout? = null
    private var appCompatEditTextDocumentNumber: AppCompatEditText? = null
    private var appCompatEditTextDocumentExpiration: AppCompatEditText? = null
    private var appCompatEditTextDateOfBirth: AppCompatEditText? = null
    private var buttonReadNFC: Button? = null

    private var mValidator: Validator? = null
    private var selectionFragmentListener: SelectionFragmentListener? = null
    private var disposable = CompositeDisposable()

    private var binding: FragmentSelectionBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        radioGroup = view.findViewById(R.id.radioButtonDataEntry)
        linearLayoutManual = view.findViewById(R.id.layoutManual)
        linearLayoutAutomatic = view.findViewById(R.id.layoutAutomatic)
        appCompatEditTextDocumentNumber = view.findViewById(R.id.documentNumber)
        appCompatEditTextDocumentExpiration = view.findViewById(R.id.documentExpiration)
        appCompatEditTextDateOfBirth = view.findViewById(R.id.documentDateOfBirth)
        buttonReadNFC = view.findViewById(R.id.buttonReadNfc)

        radioGroup!!.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonManual -> {
                    linearLayoutManual!!.visibility = View.VISIBLE
                    linearLayoutAutomatic!!.visibility = View.GONE
                }

                R.id.radioButtonOcr -> {
                    linearLayoutManual!!.visibility = View.GONE
                    linearLayoutAutomatic!!.visibility = View.VISIBLE
                    if (selectionFragmentListener != null) {
                        selectionFragmentListener!!.onMrzRequest()
                    }
                }
            }
        }

        buttonReadNFC!!.setOnClickListener { validateFields() }
        mValidator = Validator(this)
        mValidator!!.setValidationListener(this)
        mValidator!!.put(appCompatEditTextDocumentNumber!!, DocumentNumberRule())
        mValidator!!.put(appCompatEditTextDocumentExpiration!!, DateRule())
        mValidator!!.put(appCompatEditTextDateOfBirth!!, DateRule())
        binding?.buttonDeleteCSCA?.setOnClickListener {
            val subscribe = cleanCSCAFolder()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    Toast.makeText(requireContext(), "CSCA Folder deleted", Toast.LENGTH_SHORT)
                        .show()
                }
            disposable.add(subscribe)
        }
    }

    private fun validateFields() {
        try {
            mValidator!!.removeRules(appCompatEditTextDocumentNumber!!)
            mValidator!!.removeRules(appCompatEditTextDocumentExpiration!!)
            mValidator!!.removeRules(appCompatEditTextDateOfBirth!!)

            mValidator!!.put(appCompatEditTextDocumentNumber!!, DocumentNumberRule())
            mValidator!!.put(appCompatEditTextDocumentExpiration!!, DateRule())
            mValidator!!.put(appCompatEditTextDateOfBirth!!, DateRule())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mValidator!!.validate()
    }

    fun selectManualToggle() {
        radioGroup!!.check(R.id.radioButtonManual)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is SelectionFragmentListener) {
            selectionFragmentListener = activity
        }
    }

    override fun onDetach() {
        selectionFragmentListener = null
        super.onDetach()

    }

    override fun onDestroyView() {

        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        binding = null
        super.onDestroyView()
    }

    override fun onValidationSucceeded() {
        val documentNumber = appCompatEditTextDocumentNumber!!.text!!.toString()
        val dateOfBirth = appCompatEditTextDateOfBirth!!.text!!.toString()
        val documentExpiration = appCompatEditTextDocumentExpiration!!.text!!.toString()

        val mrzInfo = MRZInfo(
            "P",
            "ESP",
            "DUMMY",
            "DUMMY",
            documentNumber,
            "ESP",
            dateOfBirth,
            Gender.MALE,
            documentExpiration,
            "DUMMY"
        )
        if (selectionFragmentListener != null) {
            selectionFragmentListener!!.onEidRead(mrzInfo)
        }
    }

    override fun onValidationFailed(errors: List<ValidationError>) {
        for (error in errors) {
            val view = error.view
            val message = error.getCollatedErrorMessage(context)

            // Display error messages ;)
            if (view is EditText) {
                view.error = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    interface SelectionFragmentListener {
        fun onEidRead(mrzInfo: MRZInfo)
        fun onMrzRequest()
    }

    private fun cleanCSCAFolder(): Single<Boolean> {
        return Single.fromCallable {
            try {
                val downloadsFolder =
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                val listFiles = downloadsFolder.listFiles()
                for (tempFile in listFiles) {
                    tempFile.delete()
                }
                val listFiles1 = downloadsFolder.listFiles()
                true
            } catch (e: java.lang.Exception) {
                false
            }
        }
    }

    companion object {
        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }

        fun newInstance(mrzInfo: MRZInfo, face: Bitmap): EidDetailsFragment {
            val myFragment = EidDetailsFragment()
            val args = Bundle()
            myFragment.arguments = args
            return myFragment
        }
    }
}
