package com.example.smartroad.profile;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartroad.R;
import com.example.smartroad.model.HazardReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyReportsAdapter extends RecyclerView.Adapter<MyReportsAdapter.ViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(String reportId);
    }

    private final List<HazardReport> reports = new ArrayList<>();
    private final OnReportClickListener listener;

    public MyReportsAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<HazardReport> newReports) {
        reports.clear();
        reports.addAll(newReports);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reports.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageThumbnail;
        private final TextView textHazardType;
        private final TextView textDate;
        private final TextView textStatusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumbnail = itemView.findViewById(R.id.imageThumbnail);
            textHazardType = itemView.findViewById(R.id.textHazardType);
            textDate = itemView.findViewById(R.id.textDate);
            textStatusBadge = itemView.findViewById(R.id.textStatusBadge);
        }

        void bind(HazardReport report, OnReportClickListener listener) {
            textHazardType.setText(report.hazardType);

            String status = report.status != null ? report.status : "New";
            textStatusBadge.setText(status);

            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setShape(GradientDrawable.RECTANGLE);
            badgeBackground.setCornerRadius(24f);
            badgeBackground.setColor(statusColor(status));
            textStatusBadge.setBackground(badgeBackground);

            if (report.timestamp != null) {
                SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                textDate.setText(format.format(new java.util.Date(report.timestamp)));
            } else {
                textDate.setText("");
            }

            if (report.photoUrl != null) {
                Glide.with(itemView).load(report.photoUrl).into(imageThumbnail);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && report.id != null) {
                    listener.onReportClick(report.id);
                }
            });
        }

        private int statusColor(String status) {
            switch (status) {
                case "Under Investigation":
                    return ContextCompat.getColor(itemView.getContext(), R.color.status_investigating);
                case "Resolved":
                    return ContextCompat.getColor(itemView.getContext(), R.color.status_resolved);
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.status_new);
            }
        }
    }
}
