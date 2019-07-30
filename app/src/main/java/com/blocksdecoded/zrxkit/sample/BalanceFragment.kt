package com.blocksdecoded.zrxkit.sample

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_balance.*
import java.math.BigDecimal

class BalanceFragment : Fragment() {

    lateinit var viewModel: MainViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            viewModel.ethBalance.observe(this, Observer { balance ->
                ethBalance.text = "Eth: ${balance ?: 0}"
            })

            viewModel.wethBalance.observe(this, Observer { balance ->
                wethBalance.text = "Weth: ${balance ?: 0}"
            })

            viewModel.tokenBalance.observe(this, Observer { balance ->
                tokenBalance.text = "Token: ${balance ?: 0}"
            })

            viewModel.lastBlockHeight.observe(this, Observer { lastBlock ->
                currentBlockHeight.text = "Last block: ${lastBlock ?: 0}"
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_balance, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wrapConfirm.setOnClickListener {
            val amount = wrapAmountInput.text.toString().toDoubleOrNull()
            if (amount != null && amount > 0) {
                viewModel.wrapEther(BigDecimal(wrapAmountInput.text.toString()))
            } else {
                showAlert("Invalid input amount")
            }
        }

        unwrapConfirm.setOnClickListener {
            val amount = unwrapAmountInput.text.toString().toDoubleOrNull()
            if (amount != null && amount > 0) {
                viewModel.unwrapEther(BigDecimal(unwrapAmountInput.text.toString()))
            } else {
                showAlert("Invalid input amount")
            }
        }

        refresh.setOnClickListener {
            viewModel.refresh()
        }

        receiveAddressBtn.setOnClickListener {
            receiveAddress.text = viewModel.receiveAddress
        }
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }
}
