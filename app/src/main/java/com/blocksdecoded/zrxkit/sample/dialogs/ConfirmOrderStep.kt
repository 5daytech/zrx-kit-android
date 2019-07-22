package com.blocksdecoded.zrxkit.sample.dialogs

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.sample.MainViewModel
import com.blocksdecoded.zrxkit.sample.R
import com.blocksdecoded.zrxkit.sample.Utils
import com.blocksdecoded.zrxkit.sample.core.EOrderSide
import com.blocksdecoded.zrxkit.sample.core.EOrderSide.*
import kotlinx.android.synthetic.main.dialog_confirm_order_step.*
import java.math.BigDecimal

class ConfirmOrderStep: BaseBottomDialog(R.layout.dialog_confirm_order_step, true) {
    lateinit var viewModel: MainViewModel
    lateinit var order: SignedOrder
    lateinit var side: EOrderSide
    var price = BigDecimal(0.0)
    var makerAmount = BigDecimal(0.0)
    var takerAmount = BigDecimal(0.0)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            dialog_title.text = when(side) {
                BID -> "Sell ZRX for WETH"
                ASK -> "Buy ZRX for WETH"
            }

            confirm_trade_sell_amount.text = if (side == BID) {
                "${Utils.df.format(takerAmount)} ZRX"
            } else {
                "${Utils.df.format(makerAmount)} ZRX"
            }

            confirm_trade_per_token.text = "Per token: ${Utils.df.format(price)} WETH"

            confirm_order_amount.addTextChangedListener(object : SimpleTextWatcher() {
                @SuppressLint("SetTextI18n")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    super.onTextChanged(s, start, before, count)

                    confirm_trade_total.text = "Total price: ${Utils.df.format(getTotalPrice())} WETH"
                }
            })

            confirm_trade_approve.setOnClickListener {
                if (getAmount() > BigDecimal.ZERO) {
                    disableTrade()
                    Toast.makeText(context, "Trade started", Toast.LENGTH_LONG).show()
                    viewModel.fillOrder(
                            order,
                            side,
                            when(side) {
                                ASK -> getAmount()
                                BID -> getAmount().multiply(price)
                            }
                    )

                    dismiss()
                } else {
                    Toast.makeText(context, "Check amount", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disableTrade() {
        confirm_trade_approve.alpha = 0.5f
        confirm_trade_approve.isEnabled = false
    }

    private fun getAmount(): BigDecimal = try {
        BigDecimal(confirm_order_amount.text.toString())
    } catch (e: Exception) {
        BigDecimal(0)
    }

    private fun getTotalPrice(): BigDecimal = getAmount().multiply(price)

    companion object {
        fun open(fm: FragmentManager, order: SignedOrder, side: EOrderSide) {
            val fragment = ConfirmOrderStep()

            val makerAmount = order.makerAssetAmount.toBigDecimal().movePointLeft(18).stripTrailingZeros()
            val takerAmount = order.takerAssetAmount.toBigDecimal().movePointLeft(18).stripTrailingZeros()

            val pricePerToken = if (side == BID)
                makerAmount.toDouble().div(takerAmount.toDouble())
            else
                takerAmount.toDouble().div(makerAmount.toDouble())

            fragment.order = order
            fragment.side = side
            fragment.price = pricePerToken.toBigDecimal()
            fragment.makerAmount = makerAmount
            fragment.takerAmount = takerAmount

            fragment.show(fm, "confirm")
        }
    }
}