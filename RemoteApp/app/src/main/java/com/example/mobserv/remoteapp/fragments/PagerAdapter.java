package com.example.mobserv.remoteapp.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.example.mobserv.remoteapp.R;
import com.example.mobserv.remoteapp.model.ViewPagerTab;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessioalberti on 15/12/15.
 */

public class PagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.CustomTabProvider {

    private final ArrayList<ViewPagerTab> tabs = new ArrayList<>();
    private final List<Fragment> mFragmentList = new ArrayList<>();

    public PagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public View getCustomTabView(ViewGroup viewGroup, int i) {
        RelativeLayout tabLayout = (RelativeLayout)
                LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.tab_layout, null);

        TextView tabTitle = (TextView) tabLayout.findViewById(R.id.tab_title);
        TextView badge = (TextView) tabLayout.findViewById(R.id.badge);

        ViewPagerTab tab = tabs.get(i);

        tabTitle.setText(tab.title.toUpperCase());
        if (tab.notifications > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(tab.notifications));
        } else {
            badge.setVisibility(View.GONE);
        }

        return tabLayout;
    }

    @Override
    public void tabSelected(View view) {
        RelativeLayout tabLayout = (RelativeLayout) view;
        TextView badge = (TextView) tabLayout.findViewById(R.id.badge);
        if (badge.getText() == "") {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void tabUnselected(View view) {

    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    public void addFragment(Fragment fragment, String title, int notifNum) {
        mFragmentList.add(fragment);
        tabs.add(new ViewPagerTab(title, notifNum));
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabs.get(position).title;
    }
}