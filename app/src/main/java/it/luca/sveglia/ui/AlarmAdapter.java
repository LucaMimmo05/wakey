package it.luca.sveglia.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.DayOfWeek;
import java.util.Objects;

import it.luca.sveglia.R;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.util.Days;
import it.luca.sveglia.util.TimeFormat;

/** Card della lista sveglie: etichetta, orario, giorni (L M M G V S D) e interruttore. */
class AlarmAdapter extends ListAdapter<AlarmEntity, AlarmAdapter.Holder> {

    interface OnClick {
        void onClick(AlarmEntity alarm);
    }

    interface OnToggle {
        void onToggle(AlarmEntity alarm, boolean enabled);
    }

    private static final String[] DAY_LETTERS = {"L", "M", "M", "G", "V", "S", "D"};

    private final OnClick onClick;
    private final OnToggle onToggle;

    AlarmAdapter(OnClick onClick, OnToggle onToggle) {
        super(DIFF);
        this.onClick = onClick;
        this.onToggle = onToggle;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AlarmEntity alarm = getItem(position);
        Context context = holder.itemView.getContext();
        holder.time.setText(TimeFormat.display(alarm.hour, alarm.minute));
        String days = Days.describe(alarm.days);
        holder.label.setText(TextUtils.isEmpty(alarm.label) ? days
                : context.getString(R.string.alarm_details, alarm.label, days));
        holder.days.setText(dayLetters(context, alarm.days));
        // Le sveglie spente restano visibili ma sbiadite
        holder.content.setAlpha(alarm.enabled ? 1f : 0.45f);
        holder.itemView.setOnClickListener(v -> onClick.onClick(alarm));
        // Il listener va tolto prima di setChecked, altrimenti il riciclo delle righe lo farebbe scattare
        holder.enabled.setOnCheckedChangeListener(null);
        holder.enabled.setChecked(alarm.enabled);
        holder.enabled.setOnCheckedChangeListener((button, checked) -> onToggle.onToggle(alarm, checked));
    }

    /** "L M M G V S D" con i giorni attivi in evidenza; per le sveglie singole tutti spenti. */
    private static CharSequence dayLetters(Context context, int mask) {
        int on = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0);
        int off = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, 0);
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (int i = 0; i < 7; i++) {
            int start = text.length();
            text.append(DAY_LETTERS[i]);
            boolean active = Days.contains(mask, DayOfWeek.of(i + 1));
            text.setSpan(new ForegroundColorSpan(active ? on : off), start, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (active) {
                text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (i < 6) {
                text.append(' ');
            }
        }
        return text;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View content;
        final TextView label;
        final TextView time;
        final TextView days;
        final MaterialSwitch enabled;

        Holder(View view) {
            super(view);
            content = view.findViewById(R.id.alarm_content);
            label = view.findViewById(R.id.alarm_label);
            time = view.findViewById(R.id.alarm_time);
            days = view.findViewById(R.id.alarm_days);
            enabled = view.findViewById(R.id.alarm_enabled);
        }
    }

    private static final DiffUtil.ItemCallback<AlarmEntity> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull AlarmEntity a, @NonNull AlarmEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AlarmEntity a, @NonNull AlarmEntity b) {
            return a.hour == b.hour && a.minute == b.minute && a.days == b.days
                    && a.enabled == b.enabled
                    && Objects.equals(a.label, b.label)
                    && Objects.equals(a.soundUri, b.soundUri);
        }
    };
}
