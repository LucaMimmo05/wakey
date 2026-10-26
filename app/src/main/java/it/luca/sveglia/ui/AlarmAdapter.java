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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.util.Days;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.TimeFormat;

/**
 * Righe del gruppo sveglie: etichetta, orario, giorni (L M M G V S D) e interruttore.
 * Tenendo premuta una riga si entra nella modalità selezione, come nell'Orologio Samsung.
 */
class AlarmAdapter extends ListAdapter<AlarmEntity, AlarmAdapter.Holder> {

    interface OnClick {
        void onClick(AlarmEntity alarm);
    }

    interface OnToggle {
        void onToggle(AlarmEntity alarm, boolean enabled);
    }

    interface OnSelectionChanged {
        /** Chiamato entrando, uscendo o cambiando la selezione. */
        void onSelectionChanged(boolean selectionMode, int selectedCount);
    }

    private static final String[] DAY_LETTERS = {"L", "M", "M", "G", "V", "S", "D"};

    private final OnClick onClick;
    private final OnToggle onToggle;
    private final OnSelectionChanged onSelectionChanged;
    private boolean selectionMode;
    private final Set<Long> selected = new HashSet<>();

    AlarmAdapter(OnClick onClick, OnToggle onToggle, OnSelectionChanged onSelectionChanged) {
        super(DIFF);
        this.onClick = onClick;
        this.onToggle = onToggle;
        this.onSelectionChanged = onSelectionChanged;
    }

    boolean isSelectionMode() {
        return selectionMode;
    }

    /** Entra in modalità selezione con una sveglia già spuntata. */
    void startSelection(long alarmId) {
        selectionMode = true;
        selected.clear();
        selected.add(alarmId);
        refreshSelection();
    }

    void endSelection() {
        selectionMode = false;
        selected.clear();
        refreshSelection();
    }

    void setAllSelected(boolean all) {
        selected.clear();
        if (all) {
            for (AlarmEntity alarm : getCurrentList()) {
                selected.add(alarm.id);
            }
        }
        refreshSelection();
    }

    boolean areAllSelected() {
        return !getCurrentList().isEmpty() && selected.size() == getCurrentList().size();
    }

    List<AlarmEntity> getSelected() {
        List<AlarmEntity> result = new ArrayList<>();
        for (AlarmEntity alarm : getCurrentList()) {
            if (selected.contains(alarm.id)) {
                result.add(alarm);
            }
        }
        return result;
    }

    private void toggle(long alarmId) {
        if (!selected.remove(alarmId)) {
            selected.add(alarmId);
        }
        refreshSelection();
    }

    private void refreshSelection() {
        notifyItemRangeChanged(0, getItemCount());
        onSelectionChanged.onSelectionChanged(selectionMode, selected.size());
    }

    @Override
    public void onCurrentListChanged(@NonNull List<AlarmEntity> previous, @NonNull List<AlarmEntity> current) {
        // Sveglie eliminate nel frattempo non devono restare selezionate
        Set<Long> ids = new HashSet<>();
        for (AlarmEntity alarm : current) {
            ids.add(alarm.id);
        }
        if (selected.retainAll(ids) && selectionMode) {
            onSelectionChanged.onSelectionChanged(true, selected.size());
        }
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
        holder.label.setText(alarm.label);
        holder.label.setVisibility(TextUtils.isEmpty(alarm.label) ? View.GONE : View.VISIBLE);
        holder.days.setTextColor(MaterialColors.getColor(holder.days,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        holder.days.setText(alarm.isRepeating() ? dayLetters(context, alarm.days) : onceText(context, alarm));
        // Le sveglie spente restano leggibili ma più tenui
        holder.content.setAlpha(alarm.enabled ? 1f : 0.55f);
        holder.select.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.select.setActivated(selected.contains(alarm.id));
        holder.enabled.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggle(alarm.id);
            } else {
                onClick.onClick(alarm);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (selectionMode) {
                toggle(alarm.id);
            } else {
                startSelection(alarm.id);
            }
            return true;
        });
        // Il listener va tolto prima di setChecked, altrimenti il riciclo delle righe lo farebbe scattare
        holder.enabled.setOnCheckedChangeListener(null);
        holder.enabled.setChecked(alarm.enabled);
        holder.enabled.setOnCheckedChangeListener((button, checked) -> onToggle.onToggle(alarm, checked));
    }

    /** Sveglia singola: "Una volta · domani", "Una volta · oggi" o "Una volta · mercoledì". */
    private static String onceText(Context context, AlarmEntity alarm) {
        ZoneId zone = ZoneId.systemDefault();
        long now = System.currentTimeMillis();
        LocalDateTime nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        LocalDateTime when = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(AlarmScheduler.nextTrigger(alarm, now)), zone);
        return context.getString(R.string.once_on, RelativeTime.day(nowTime, when));
    }

    /** "L M M G V S D" con i giorni attivi in blu e gli altri appena visibili. */
    private static CharSequence dayLetters(Context context, int mask) {
        int on = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0);
        int off = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, 0);
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
                // Due spazi invece di letterSpacing, che allargherebbe anche altri testi
                text.append("  ");
            }
        }
        return text;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView select;
        final View content;
        final TextView label;
        final TextView time;
        final TextView days;
        final MaterialSwitch enabled;

        Holder(View view) {
            super(view);
            select = view.findViewById(R.id.alarm_select);
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
