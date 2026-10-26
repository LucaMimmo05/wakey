package it.luca.sveglia.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * Rotella per scegliere un numero (ore o minuti), come nell'Orologio Samsung:
 * si scorre in verticale, il valore al centro è quello scelto e gli altri si
 * rimpiccioliscono e sbiadiscono. La lista è "infinita" (dopo 59 viene 00).
 */
public class WheelPicker extends RecyclerView {

    public interface OnValueChangeListener {
        void onValueChange(int value);
    }

    private static final int VISIBLE_ROWS = 3;
    // Copie della lista: abbastanza da non arrivare mai in fondo scorrendo
    private static final int LOOPS = 2000;

    private final LinearLayoutManager layoutManager;
    private final LinearSnapHelper snapHelper = new LinearSnapHelper();
    private final int rowHeight;
    private int count = 60;
    private int value;
    @Nullable
    private OnValueChangeListener listener;

    public WheelPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rowHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 76,
                getResources().getDisplayMetrics()));
        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);
        setAdapter(new NumberAdapter());
        setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);
        snapHelper.attachToRecyclerView(this);
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                styleRows();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int state) {
                if (state == SCROLL_STATE_IDLE) {
                    updateValueFromCenter();
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Altezza fissa: tre righe, quella centrale è il valore scelto
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(rowHeight * VISIBLE_ROWS, MeasureSpec.EXACTLY));
    }

    /** Numero di valori: 24 per le ore, 60 per i minuti. */
    public void setCount(int count) {
        this.count = count;
        getAdapter().notifyDataSetChanged();
    }

    public void setOnValueChangeListener(@Nullable OnValueChangeListener listener) {
        this.listener = listener;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        // Si parte da metà della lista, così si può scorrere in entrambe le direzioni
        int position = (LOOPS / 2) * count + value;
        // La riga sopra quella centrale va in cima
        layoutManager.scrollToPositionWithOffset(position - 1, 0);
        post(this::styleRows);
    }

    private void updateValueFromCenter() {
        View center = snapHelper.findSnapView(layoutManager);
        if (center == null) {
            return;
        }
        int newValue = getChildAdapterPosition(center) % count;
        if (newValue != value) {
            value = newValue;
            if (listener != null) {
                listener.onValueChange(value);
            }
        }
    }

    /** Dimensione e trasparenza di ogni riga in base alla distanza dal centro. */
    private void styleRows() {
        float middle = getHeight() / 2f;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            float childMiddle = (child.getTop() + child.getBottom()) / 2f;
            float distance = Math.min(1f, Math.abs(childMiddle - middle) / rowHeight);
            float scale = 1f - 0.35f * distance;
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(1f - 0.75f * distance);
        }
    }

    private class NumberAdapter extends Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView text = new TextView(parent.getContext());
            text.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
            text.setGravity(Gravity.CENTER);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
            // Numeri sottili come nell'Orologio Samsung (font di sistema)
            text.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            text.setIncludeFontPadding(false);
            text.setTextColor(MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurface));
            return new ViewHolder(text) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(
                    String.format(Locale.ITALIAN, "%02d", position % count));
        }

        @Override
        public int getItemCount() {
            return count * LOOPS;
        }
    }
}
