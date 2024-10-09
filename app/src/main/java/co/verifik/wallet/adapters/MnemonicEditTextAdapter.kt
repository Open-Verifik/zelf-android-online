package co.verifik.wallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.data.domain.MnemonicSize
import co.verifik.wallet.utils.CutCopyPasteEditText
import kotlin.math.min

class MnemonicEditTextAdapter(
    private val context: Context,
    private var size: MnemonicSize,
    private val callback: (Boolean) -> Unit,
    private val changeFocus: (Int) -> Unit
) : RecyclerView.Adapter<MnemonicEditTextAdapter.ViewHolder>() {

    var words: MutableList<String>

    init {
        val sizeNum = when (size) {
            MnemonicSize.MNEMONIC12 -> 12
            MnemonicSize.MNEMONIC24 -> 24
        }
        words = MutableList(sizeNum) { "" }
    }

    fun setMnemonicSize(size: MnemonicSize) {
        this.size = size
        val sizeNum = when (size) {
            MnemonicSize.MNEMONIC12 -> 12
            MnemonicSize.MNEMONIC24 -> 24
        }
        words = MutableList(sizeNum) { "" }
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val editTextView: CutCopyPasteEditText = view.findViewById(R.id.edittext_pssw)

        init {
            editTextView.doAfterTextChanged { text ->
                if (!text.isNullOrBlank()) {
                    words[bindingAdapterPosition] = text.toString()
                    callback(words.all { it.isNotEmpty() })
                }
                val lastChar = text.toString().lastOrNull()
                if (lastChar == ' ') {
                    text?.delete(text.count()-1, text.count())
                    changeFocus(bindingAdapterPosition)
                }
            }
            editTextView.setOnCutCopyPasteListener(object: CutCopyPasteEditText.OnCutCopyPasteListener {
                override fun onCut(editText: CutCopyPasteEditText) {
                }
                override fun onCopy(editText: CutCopyPasteEditText) {
                }
                override fun onPaste(editText: CutCopyPasteEditText) {
                    val pastedWords = editText.text.toString().split(" ").toMutableList()
                    val lastIndex = min(words.size-1, pastedWords.size-1)
                    for (i in 0..lastIndex) {
                        words[i] = pastedWords[i]
                    }
                    notifyDataSetChanged()
                }
            })
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.mnemonic_edittext_cell_view,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.editTextView.hint = "${position + 1}."
        holder.editTextView.setText(words[position])
    }

    override fun getItemCount(): Int {
        return if (size == MnemonicSize.MNEMONIC12)
            12
        else 24
    }
}
