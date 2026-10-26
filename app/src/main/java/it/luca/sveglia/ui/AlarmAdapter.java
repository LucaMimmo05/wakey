package it.luca.sveglia.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Objects;

import it.luca.sveglia.R;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.util.Days;
import it.luca.sveglia.util.TimeFormat;

/** Righe della lista sveglie: orario, etichetta/giorni e interruttore. */
class AlarmAdapter extends ListAdapter<AlarmEntity, AlarmAdapter.Holder> {

    interface OnClick {
        void onClick(AlarmEntity alarm);
    }

    interface OnToggle {
        void onToggle(AlarmEntity alarm, boolean enabled);
    }

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
        holder.time.setText(TimeFormat.display(alarm.hour, alarm.minute));
        String days = Days.describe(alarm.days);
        holder.details.setText(TextUtils.isEmpty(alarm.label) ? days
                : holder.itemView.getContext().getString(R.string.alarm_details, alarm.label, days));
        holder.itemView.setAlpha(alarm.enabled ? 1f : 0.6f);
        holder.itemView.setOnClickListener(v -> onClick.onClick(alarm));
        // Il listener va tolto prima di setChecked, altrimenti il riciclo delle righe lo farebbe scattare
        holder.enabled.setOnCheckedChangeListener(null);
        holder.enabled.setChecked(alarm.enabled);
        holder.enabled.setOnCheckedChangeListener((button, checked) -> onToggle.onToggle(alarm, checked));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView details;
        final MaterialSwitch enabled;

        Holder(View view) {
            super(view);
            time = view.findViewById(R.id.alarm_time);
            details = view.findViewById(R.id.alarm_details);
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
