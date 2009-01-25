package org.dodgybits.android.shuffle.view;

import org.dodgybits.android.shuffle.util.TextColours;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A TextView with coloured text and a round edged coloured background.
 */
public class LabelView extends TextView {
	protected PaintDrawable mBackground;
	protected TextColours mTextColours;
	
	public LabelView(Context context) {
		super(context);
		init(context);
	}
	
	@SuppressWarnings("unchecked")
    public LabelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	@SuppressWarnings("unchecked")
	public LabelView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

    private void init(Context context) {
    	mBackground = new PaintDrawable();
    	mBackground.setCornerRadius(4.0f);
		//mBackground.setAlpha(240);
    	setBackgroundDrawable(mBackground);
		mTextColours = TextColours.getInstance(context);
    }
    
    public void setColourIndex(int colourIndex, boolean isSelected) {
		int textColour = mTextColours.getTextColour(colourIndex);
		int bgColour = mTextColours.getBackgroundColours(colourIndex);
		setTextColor(isSelected ? bgColour : textColour);
		mBackground.setColorFilter(isSelected ? textColour : bgColour, Mode.SRC);

    }
}