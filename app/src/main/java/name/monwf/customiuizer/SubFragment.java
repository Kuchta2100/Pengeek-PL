package name.monwf.customiuizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import name.monwf.customiuizer.prefs.PreferenceCategoryEx;
import name.monwf.customiuizer.prefs.PreferenceState;
import name.monwf.customiuizer.prefs.SpinnerEx;
import name.monwf.customiuizer.prefs.SpinnerExFake;
import name.monwf.customiuizer.subs.AppSelector;
import name.monwf.customiuizer.subs.MultiAction;
import name.monwf.customiuizer.subs.SortableList;
import name.monwf.customiuizer.utils.AppHelper;
import name.monwf.customiuizer.utils.Helpers;

public class SubFragment extends PreferenceFragmentBase {
    private int contentResId = 0;
    public String settingTitle = "";
    protected String sub = "";
    protected Bundle catInfo = null;
    protected boolean isStandalone = false;
    private float order = 100.0f;
    private String highlightKey = null;
    public boolean padded = true;
    AppHelper.SettingsType settingsType = AppHelper.SettingsType.Preference;
    AppHelper.ActionBarType abType = AppHelper.ActionBarType.Edit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            settingsType = AppHelper.SettingsType.values()[args.getInt("settingsType")];
            abType = AppHelper.ActionBarType.values()[args.getInt("abType")];
            contentResId = args.getInt("contentResId");
            settingTitle = args.getString("titleResId", "");
            order = args.getFloat("order") + 10.0f;
            catInfo = args.getBundle("catInfo");
            sub = args.getString("sub", "");
            isStandalone = args.getBoolean("isStandalone");
            highlightKey = args.getString("mod");
        }
        
        if (abType == AppHelper.ActionBarType.Edit) {
            isCustomActionBar = true;
        }
        toolbarMenu = toolbarMenu || isCustomActionBar;

        if (contentResId == 0) {
            if (getActivity() != null) getActivity().finish();
            return;
        }

        if (settingsType == AppHelper.SettingsType.Preference) {
            super.onCreate(savedInstanceState, contentResId);
        } else {
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (settingsType == AppHelper.SettingsType.Edit) {
            loadSharedPrefs();
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (isStandalone && catInfo != null && catInfo.getBoolean("isDynamic")) {
                actionBar.setTitle(settingTitle + " ⟲");
            }
            else if (!isStandalone && !sub.isEmpty()) {
                PreferenceScreen screen = getPreferenceScreen();
                if (screen != null && screen.getPreferenceCount() > 0) {
                    Preference pref0 = screen.getPreference(0);
                    if (pref0 != null && pref0.getTitle() != null) {
                        actionBar.setTitle(pref0.getTitle());
                    } else {
                        actionBar.setTitle(settingTitle);
                    }
                } else {
                    actionBar.setTitle(settingTitle);
                }
            }
            else {
                actionBar.setTitle(settingTitle);
            }
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if (settingsType == AppHelper.SettingsType.Preference) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(contentResId, rootKey);
            PreferenceState highlightPref;
            if (highlightKey != null && (highlightPref = findPreference(highlightKey)) != null) {
                highlightPref.applyHighlight();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            LayoutInflater crtInflator = inflater.cloneInContext(requireContext());
            if (settingsType == AppHelper.SettingsType.Preference) {
                return super.onCreateView(crtInflator, container, savedInstanceState);
            }
            View view = crtInflator.inflate(padded ? R.layout.prefs_common_padded : R.layout.prefs_common, container, false);
            crtInflator.inflate(contentResId, (FrameLayout)view);
            return view;
        } catch (Exception e) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTranslationZ(order);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (highlightKey != null) {
            RecyclerView mList = getListView();
            if (mList != null && mList.getAdapter() instanceof PreferenceGroup.PreferencePositionCallback) {
                int position = ((PreferenceGroup.PreferencePositionCallback) mList.getAdapter())
                    .getPreferenceAdapterPosition(highlightKey);
                highlightKey = null;
                if (position >= 9) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(mList.getContext()) {
                        @Override protected int getVerticalSnapPreference() { return LinearSmoothScroller.SNAP_TO_START; }
                    };
                    smoothScroller.setTargetPosition(position);
                    View v = getView();
                    if (v != null) v.postDelayed(() -> mList.getLayoutManager().startSmoothScroll(smoothScroller), 380);
                }
            }
        }
    }

    public void saveSharedPrefs() {
        View v = getView();
        if (v == null) return;
        ArrayList<View> nViews = Helpers.getChildViewsRecursive(v.findViewById(R.id.container), false);
        for (View nView : nViews) {
            if (nView != null && nView.getTag() instanceof String) {
                String tag = (String) nView.getTag();
                try {
                    if (nView instanceof TextView)
                        AppHelper.appPrefs.edit().putString(tag, ((TextView)nView).getText().toString()).apply();
                    else if (nView instanceof SpinnerExFake) {
                        AppHelper.appPrefs.edit().putString(tag, ((SpinnerExFake)nView).getValue()).apply();
                        ((SpinnerExFake)nView).applyOthers();
                    } else if (nView instanceof SpinnerEx)
                        AppHelper.appPrefs.edit().putInt(tag, ((SpinnerEx)nView).getSelectedArrayValue()).apply();
                } catch (Exception ignored) {}
            }
        }
    }

    public void loadSharedPrefs() {
        View v = getView();
        if (v == null) return;
        ArrayList<View> nViews = Helpers.getChildViewsRecursive(v.findViewById(R.id.container), false);
        for (View nView: nViews) {
            if (nView != null && nView.getTag() instanceof String) {
                if (nView instanceof TextView) {
                    ((TextView)nView).setText(AppHelper.getStringOfAppPrefs((String)nView.getTag(), ""));
                }
            }
        }
    }

    public void selectSub() {
        if (isStandalone) return;
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || sub.isEmpty()) return;
        
        for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
            Preference pref = screen.getPreference(i);
            if (pref != null && pref.getKey() != null && !pref.getKey().equals(sub)) {
                screen.removePreference(pref);
            }
        }
    }

    public void finish() {
        AppCompatActivity act = (AppCompatActivity) getActivity();
        if (act != null) {
            Helpers.hideKeyboard(act, getView());
            FragmentManager fragmentManager = getParentFragmentManager();
            if (fragmentManager != null && isResumed()) {
                fragmentManager.popBackStackImmediate();
            } else {
                act.getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void confirmEdit() {
        saveSharedPrefs();
        finish();
    }
}