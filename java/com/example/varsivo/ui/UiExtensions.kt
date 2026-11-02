package com.example.varsivo.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.example.varsivo.databinding.IncludeSearchBarBinding

/** Small helpers shared by several screens. */

fun View.show() { visibility = View.VISIBLE }

fun View.hide() { visibility = View.GONE }

fun View.showIf(condition: Boolean) { visibility = if (condition) View.VISIBLE else View.GONE }

/** Calls [onChange] with the new text every time the user edits this field. */
fun EditText.onTextChanged(onChange: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            onChange(s?.toString().orEmpty())
        }
    })
}

/** Runs [action] when the keyboard's action key (Search / Done) is pressed. */
fun TextView.onImeAction(imeAction: Int, action: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == imeAction) {
            action()
            true
        } else {
            false
        }
    }
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Wires the shared search bar (include_search_bar.xml): shows the clear button only while there is
 * text, reports each change, and optionally reports the keyboard's Search key.
 */
fun IncludeSearchBarBinding.setup(
    hint: String? = null,
    onSearchKey: (() -> Unit)? = null,
    onQueryChanged: (String) -> Unit = {}
) {
    if (hint != null) searchInput.hint = hint
    searchInput.onTextChanged { text ->
        clearButton.showIf(text.isNotEmpty())
        onQueryChanged(text)
    }
    clearButton.setOnClickListener { searchInput.setText("") }
    if (onSearchKey != null) {
        searchInput.onImeAction(EditorInfo.IME_ACTION_SEARCH) {
            searchInput.hideKeyboard()
            onSearchKey()
        }
    }
}
