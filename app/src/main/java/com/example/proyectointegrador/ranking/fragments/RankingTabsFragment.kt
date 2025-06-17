package com.example.proyectointegrador.ranking.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.proyectointegrador.databinding.FragmentRankingTabsBinding
import com.google.android.material.tabs.TabLayoutMediator

class RankingTabsFragment : Fragment() {

    private var _binding: FragmentRankingTabsBinding? = null
    private val binding get() = _binding!!

    private val tabTitles = listOf("Global", "Por zona")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingTabsBinding.inflate(inflater, container, false)
        val view = binding.root

        val fragments = listOf(
            RankingFragment(),        // Ranking global por usuario
            RankingZonaFragment()     // Ranking por zona/ciudad
        )

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        binding.viewPager.adapter = adapter

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