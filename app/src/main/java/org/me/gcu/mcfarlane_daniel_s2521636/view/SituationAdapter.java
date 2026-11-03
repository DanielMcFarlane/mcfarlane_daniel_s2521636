//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Adapter for the RecyclerView to display a list of traffic situations.
//

package org.me.gcu.mcfarlane_daniel_s2521636.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.me.gcu.mcfarlane_daniel_s2521636.R;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import java.util.ArrayList;
import java.util.List;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClicked(Situation situation);
    }

    private List<Situation> situationList = new ArrayList<>();
    private final OnSituationClickListener clickListener;

    private double referenceLatitude = 55.8669;
    private double referenceLongitude = -4.2504;

    public SituationAdapter(OnSituationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<Situation> newSituations, double userLatitude, double userLongitude) {
        this.situationList = newSituations != null ? newSituations : new ArrayList<>();
        this.referenceLatitude = userLatitude;
        this.referenceLongitude = userLongitude;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Situation currentSituation = situationList.get(position);

        holder.roadAndType.setText(currentSituation.getPrimaryRoadName() + "   " + currentSituation.getDisplayTypeLabel());
        holder.comment.setText(currentSituation.getGeneralPublicComment());

        double distanceMiles = currentSituation.getMinimumDistanceMiles(referenceLatitude, referenceLongitude);
        String formattedDistance = distanceMiles < 10000 ? holder.itemView.getContext().getString(R.string.miles_away, distanceMiles) : "";

        String badgeContent = currentSituation.getShortStatus();
        if (!formattedDistance.isEmpty()) {
            badgeContent += "   " + formattedDistance;
        }

        holder.delayBadge.setText(badgeContent);
        holder.delayBadge.getBackground().setTint(currentSituation.getSeverityColour());
        holder.icon.setImageResource(currentSituation.getIconResource());

        holder.itemView.setOnClickListener(itemView -> clickListener.onSituationClicked(currentSituation));
    }

    @Override
    public int getItemCount() {
        return situationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView roadAndType;
        final TextView comment;
        final TextView delayBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.situationIcon);
            roadAndType = itemView.findViewById(R.id.itemRoadAndType);
            comment = itemView.findViewById(R.id.itemComment);
            delayBadge = itemView.findViewById(R.id.itemDelayBadge);
        }
    }
}