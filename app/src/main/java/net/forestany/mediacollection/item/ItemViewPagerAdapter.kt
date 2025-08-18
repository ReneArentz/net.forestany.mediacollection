package net.forestany.mediacollection.item

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ItemViewPagerAdapter(activity: ItemViewActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GeneralFragment()
            1 -> DetailsFragment()
            2 -> PosterFragment()
            3 -> OtherFragment()
            else -> GeneralFragment()
        }
    }
}