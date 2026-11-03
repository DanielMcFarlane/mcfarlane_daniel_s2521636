//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Fragment that displays a filterable list of traffic situations using a RecyclerView.
//

package org.me.gcu.mcfarlane_daniel_s2521636.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import org.me.gcu.mcfarlane_daniel_s2521636.R;
import org.me.gcu.mcfarlane_daniel_s2521636.viewmodel.TrafficViewModel;

public class SituationListFragment extends Fragment {

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private TextView listCountHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listCountHeader = view.findViewById(R.id.listCountHeader);
        RecyclerView recyclerView = view.findViewById(R.id.situationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        ChipGroup filterGroup = view.findViewById(R.id.streamFilterGroup);
        if (filterGroup != null) {
            filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;

                int selectedId = checkedIds.get(0);
                if (selectedId == R.id.radioIncidents) {
                    viewModel.setStreamFilter(TrafficViewModel.StreamFilter.INCIDENTS_ONLY);
                } else if (selectedId == R.id.radioRoadworks) {
                    viewModel.setStreamFilter(TrafficViewModel.StreamFilter.ROADWORKS_ONLY);
                } else {
                    viewModel.setStreamFilter(TrafficViewModel.StreamFilter.ALL);
                }
            });
        }

        adapter = new SituationAdapter(situation -> {
            viewModel.selectSituation(situation);
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).showDetailsPaneIfNeeded();
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getFilteredSituations().observe(getViewLifecycleOwner(), filteredSituations -> {
            int matchingCount = filteredSituations != null ? filteredSituations.size() : 0;
            listCountHeader.setText(getString(R.string.situations_match_search, matchingCount));
            adapter.submitList(filteredSituations, viewModel.getUserLatitude(), viewModel.getUserLongitude());
        });
    }
}