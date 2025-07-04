package com.example.proyectointegrador.ranking.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.proyectointegrador.R
import com.example.proyectointegrador.databinding.FragmentRankingTabsBinding
import com.google.android.material.tabs.TabLayoutMediator

class RankingTabsFragment : Fragment() {

    private var _binding: FragmentRankingTabsBinding? = null
    private val binding get() = _binding!!

    private val tabTitles by lazy {
        listOf(
            context?.getString(R.string.tab_global),
            context?.getString(R.string.tab_by_zone),
            context?.getString(R.string.tab_by_heatmap)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingTabsBinding.inflate(inflater, container, false)
        val view = binding.root

        val fragments = listOf(
            RankingFragment(),
            RankingZonaFragment(),
            HeatmapFragment()
        )

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}