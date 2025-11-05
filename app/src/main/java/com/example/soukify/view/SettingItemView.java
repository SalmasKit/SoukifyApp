package com.example.soukify.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.soukify.R;

public class SettingItemView extends LinearLayout {
    private ImageView icon;
    private TextView title;
    private View divider;
    private ImageView chevron;
    // The inner clickable row (first child in item_setting.xml)
    private View clickableArea;

    public SettingItemView(Context context) {
        this(context, null);
    }

    public SettingItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.item_setting, this, true);
        
        // Initialize views
        icon = findViewById(R.id.icon);
        title = findViewById(R.id.title);
        divider = findViewById(R.id.divider);
        chevron = findViewById(R.id.chevron);
        // item_setting.xml inflates a merge with a clickable LinearLayout as first child
        if (getChildCount() > 0) {
            clickableArea = getChildAt(0);
        } else {
            clickableArea = this;
        }

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.SettingItemView,
                    defStyleAttr,
                    0
            );

            try {
                // Set icon if provided
                int iconResId = typedArray.getResourceId(
                        R.styleable.SettingItemView_settingIcon,
                        R.drawable.ic_settings
                );
                icon.setImageResource(iconResId);

                // Set title if provided
                String titleText = typedArray.getString(R.styleable.SettingItemView_settingTitle);
                if (titleText != null) {
                    title.setText(titleText);
                }

                // Show/hide divider
                boolean showDivider = typedArray.getBoolean(R.styleable.SettingItemView_showDivider, true);
                divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);

                // Show/hide chevron
                boolean showChevron = typedArray.getBoolean(R.styleable.SettingItemView_showChevron, true);
                chevron.setVisibility(showChevron ? View.VISIBLE : View.GONE);
            } finally {
                typedArray.recycle();
            }
        }
    }

    public void setTitle(String titleText) {
        this.title.setText(titleText);
    }

    public void setIcon(int iconResId) {
        this.icon.setImageResource(iconResId);
    }

    public void setOnSettingClickListener(OnClickListener listener) {
        if (clickableArea != null) {
            clickableArea.setOnClickListener(listener);
        } else {
            setOnClickListener(listener);
        }
    }
}
