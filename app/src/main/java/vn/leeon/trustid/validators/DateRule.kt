package vn.leeon.trustid.validators

import android.content.Context
import androidx.appcompat.widget.AppCompatEditText
import com.mobsandgeeks.saripaar.QuickRule
import vn.leeon.trustid.R
import java.util.regex.Pattern


/**
 * Created by Surface on 15/08/2017.
 */

class DateRule : QuickRule<AppCompatEditText>() {

    override fun isValid(editText: AppCompatEditText): Boolean {
        val text = editText.text!!.toString().trim { it <= ' ' }
        val patternDate = Pattern.compile(REGEX)
        val matcherDate = patternDate.matcher(text)
        return matcherDate.find()
    }

    override fun getMessage(context: Context): String {
        return context.getString(R.string.error_validation_date)
    }

    companion object {

        private val REGEX = "[0-9]{6}$"
    }
}
