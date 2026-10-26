package it.luca.sveglia.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import it.luca.sveglia.R;

/** Linee sottili tra le righe di un gruppo One UI: sotto ogni riga tranne l'ultima. */
class GroupDividers extends RecyclerView.ItemDecoration {

    private final Drawable divider;

    GroupDividers(Context context) {
        divider = ContextCompat.getDrawable(context, R.drawable.bg_divider_inset);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int last = state.getItemCount() - 1;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (parent.getChildAdapterPosition(child) >= last) {
                continue;
            }
            int bottom = child.getBottom();
            divider.setBounds(0, bottom - divider.getIntrinsicHeight(), parent.getWidth(), bottom);
            divider.draw(canvas);
        }
    }
}
