package com.fridaytech.zrxkit.sample

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.sample.Utils.df
import com.fridaytech.zrxkit.sample.core.EOrderSide
import com.fridaytech.zrxkit.sample.core.EOrderSide.*
import com.fridaytech.zrxkit.sample.dialogs.ConfirmOrderStep
import com.fridaytech.zrxkit.sample.dialogs.CreateOrderStep
import kotlinx.android.synthetic.main.fragment_orders.*

class OrdersFragment: Fragment() {

    lateinit var side: EOrderSide
    lateinit var viewModel: MainViewModel
    lateinit var adapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            when(side) {
                ASK -> { viewModel.asks }
                BID -> { viewModel.bids }
            }.observe(this, Observer { adapter.setOrders(it) })

            viewModel.orderInfoEvent.observe(this, Observer {
                ConfirmOrderStep.open(childFragmentManager, it.first, it.second)
            })

            adapter = OrdersAdapter(side) { position ->
                viewModel.onOrderClick(position, side)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ordersRecycler.adapter = adapter
        ordersRecycler.layoutManager = LinearLayoutManager(context)

        createOrder.setOnClickListener { CreateOrderStep.open(childFragmentManager, side) }
    }

    companion object {
        fun newInstance(type: EOrderSide): Fragment = OrdersFragment().apply {
            this.side = type
        }
    }
}


class OrdersAdapter(
    private val side: EOrderSide,
    private val onClickListener: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var orders = listOf<SignedOrder>()

    override fun getItemCount() = orders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false), side, onClickListener)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OrderViewHolder -> holder.bind(orders[position])
        }
    }

    fun setOrders(orders: List<SignedOrder>) {
        this.orders = orders
        notifyDataSetChanged()
    }
}

class OrderViewHolder(
    view: View,
    private val side: EOrderSide,
    private val onClickListener: (Int) -> Unit
) : RecyclerView.ViewHolder(view) {

    private val baseAmount: TextView = itemView.findViewById(R.id.order_base_amount)
    private val quoteAmount: TextView = itemView.findViewById(R.id.order_quote_amount)
    private val price: TextView = itemView.findViewById(R.id.order_price)

    init {
        itemView.setOnClickListener {
            onClickListener(adapterPosition)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(order: SignedOrder) {
        itemView.setBackgroundColor(if (adapterPosition % 2 == 0)
            Color.parseColor("#dddddd")
        else
            Color.TRANSPARENT
        )

        val makerAmount = order.makerAssetAmount.toBigDecimal().movePointLeft(18).stripTrailingZeros()
        val takerAmount = order.takerAssetAmount.toBigDecimal().movePointLeft(18).stripTrailingZeros()

        val pricePerToken = if (side == BID)
            makerAmount.toDouble().div(takerAmount.toDouble())
        else
            takerAmount.toDouble().div(makerAmount.toDouble())

        if (side == BID) {
            baseAmount.text = "${df.format(takerAmount)} Token"
            quoteAmount.text = "${df.format(makerAmount)} WETH"
        } else {
            baseAmount.text = "${df.format(makerAmount)} Token"
            quoteAmount.text = "${df.format(takerAmount)} WETH"
        }

        price.text = "${df.format(pricePerToken)} WETH per Token"
    }
}