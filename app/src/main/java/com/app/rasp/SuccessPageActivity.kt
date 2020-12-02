package com.app.rasp

import androidx.core.text.HtmlCompat
import com.app.rasp.base.BaseActivity
import com.app.rasp.databinding.ActivitySuccessPageBinding

class SuccessPageActivity : BaseActivity<ActivitySuccessPageBinding>() {
    override fun initViews() {

        val leftQuotation = getString(R.string.left_quotation)
        val rightQuotation = getString(R.string.right_quotation)
        val content = "Success.. See the face verification device camera"
        binding.text = HtmlCompat.fromHtml(
            "$leftQuotation $content $rightQuotation",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        binding.btnDone.setOnClickListener {
            onBackPressed()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_success_page
}