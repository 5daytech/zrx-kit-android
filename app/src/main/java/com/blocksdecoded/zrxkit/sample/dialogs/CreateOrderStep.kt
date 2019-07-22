package com.blocksdecoded.zrxkit.sample.dialogs

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import android.view.View
import android.widget.Toast
import com.blocksdecoded.zrxkit.sample.MainViewModel
import com.blocksdecoded.zrxkit.sample.R
import com.blocksdecoded.zrxkit.sample.Utils
import com.blocksdecoded.zrxkit.sample.core.EOrderSide
import com.blocksdecoded.zrxkit.sample.core.EOrderSide.*
import kotlinx.android.synthetic.main.dialog_create_order_step.*
import kotlinx.android.synthetic.main.dialog_create_order_step.dialog_title
import java.math.BigDecimal

class CreateOrderStep: BaseBottomDialog(R.layout.dialog_create_order_step, true) {
    lateinit var viewModel: MainViewModel
    var side: EOrderSide = BID

    private val amount: Double
        get() = create_order_amount.text.toString().toDoubleOrNull() ?: 0.0

    private val price: Double
        get() = create_order_price.text.toString().toDoubleOrNull() ?: 0.0

    private val totalPrice: Double
        get() = amount * price

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            dialog_title.text = when (side) {
                ASK -> "Place SELL order"
                BID -> "Place BUY order"
            }

            create_order_base.text = "ZRX: "

            create_order_amount.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateTotal()
                }
            })

            create_order_price.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateTotal()
                }
            })

            create_trade_confirm.setOnClickListener {
                if (amount > 0.0 && price > 0.0) {
                    create_trade_confirm.alpha = 0.5f
                    create_trade_confirm.isEnabled = false

                    viewModel.createOrder(
                            BigDecimal(create_order_amount.text.toString()),
                            BigDecimal(create_order_price.text.toString()),
                            side
                    )

                    dismiss()
                } else {
                    Toast.makeText(context, "Check amount or price", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateTotal() {
        create_order_total.text = "Total price: ${Utils.df.format(totalPrice)} WETH"
    }

    companion object {
        fun open(fm: FragmentManager, side: EOrderSide) {
            val fragment = CreateOrderStep()

            fragment.side = side

            fragment.show(fm, "create")
        }
    }
}