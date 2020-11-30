package com.tcorp.cap.testapp.ui.terminal

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import com.tcorp.cap.testapp.R
import com.tcorp.cap.testapp.bluetooth.Bluetooth
import com.tcorp.cap.testapp.bluetooth.Bluetooth.inputChannel
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class TerminalFragment : Fragment() {

    private lateinit var textView: TextView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton

    private val displayString = SpannableStringBuilder()

    private var reader: Job? = null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_terminal, container, false)

        textView = view.findViewById(R.id.txt)
        editText = view.findViewById(R.id.edtTxt)
        sendButton = view.findViewById(R.id.sendButton)

        textView.movementMethod = ScrollingMovementMethod()

        sendButton.setOnClickListener {
            Bluetooth.mapManagerAndDevice.value?.forEach { _, manager ->
                CoroutineScope(Dispatchers.IO).launch { manager.write(editText.text.toString()) }
                editText.text.clear()
            }
        }

         reader = CoroutineScope(Dispatchers.Main).launch { readPipeline() }

        return view
    }

    override fun onDestroy() {
        reader?.cancel()
        super.onDestroy()
    }

    private suspend fun readPipeline(){
        while (NonCancellable.isActive) {
                val pair = inputChannel.receive()
                if (displayString.length > DEFAULT_BUFFER_SIZE - 100) {
                    displayString.delete(1, 101)
                }

                displayString.append("\n${pair.second}: ", R.color.yellow)
                displayString.append(pair.first, R.color.white)

                textView.text = displayString

        }
    }


    private fun SpannableStringBuilder.append(text: String, @ColorRes color: Int){
        val index = length
        this.append(text)
        this.setSpan(ForegroundColorSpan(resources.getColor(color, null)), index, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}