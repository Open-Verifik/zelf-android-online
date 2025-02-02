package co.verifik.wallet.utils;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputEditText;

public class CutCopyPasteEditText extends TextInputEditText {

    public interface OnCutCopyPasteListener {
        void onCut(CutCopyPasteEditText editText);
        void onCopy(CutCopyPasteEditText editText);
        void onPaste(CutCopyPasteEditText editText);
    }

    private OnCutCopyPasteListener mOnCutCopyPasteListener;

    /**
     * Set a OnCutCopyPasteListener.
     * @param listener
     */
    public void setOnCutCopyPasteListener(OnCutCopyPasteListener listener) {
        mOnCutCopyPasteListener = listener;
    }

    /*
        Just the constructors to create a new EditText...
     */
    public CutCopyPasteEditText(Context context) {
        super(context);
    }

    public CutCopyPasteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CutCopyPasteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * <p>This is where the "magic" happens.</p>
     * <p>The menu used to cut/copy/paste is a normal ContextMenu, which allows us to
     *  overwrite the consuming method and react on the different events.</p>
     * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3_r1/android/widget/TextView.java#TextView.onTextContextMenuItem%28int%29">Original Implementation</a>
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        // Do your thing:
        boolean consumed = super.onTextContextMenuItem(id);
        // React:
        switch (id){
            case android.R.id.cut:
                onCut();
                break;
            case android.R.id.copy:
                onCopy();
                break;
            case android.R.id.paste:
                onPaste();
        }
        return consumed;
    }

    /**
     * Text was cut from this EditText.
     */
    public void onCut(){
        if(mOnCutCopyPasteListener!=null)
            mOnCutCopyPasteListener.onCut(this);
    }

    /**
     * Text was copied from this EditText.
     */
    public void onCopy(){
        if(mOnCutCopyPasteListener!=null)
            mOnCutCopyPasteListener.onCopy(this);
    }

    /**
     * Text was pasted into the EditText.
     */
    public void onPaste(){
        if(mOnCutCopyPasteListener!=null)
            mOnCutCopyPasteListener.onPaste(this);
    }
}
