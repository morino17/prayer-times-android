package com.metinkale.prayerapp.vakit.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.EditText;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.vakit.times.Times;

/**
 * Created by Metin on 21.07.2015.
 */
public class SettingsFragment extends Fragment {
    private View mView;

    private EditText mName;
    private EditText[] mMins = new EditText[6];
    private ImageView[] mMinus = new ImageView[6];
    private ImageView[] mPlus = new ImageView[6];
    private Times mTimes;
    private int[] mMinAdj;
    private EditText mTimeZone;

    @SuppressLint("WrongViewCast")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bdl) {
        View v = inflater.inflate(R.layout.vakit_settings, container, false);
        mName = (EditText) v.findViewById(R.id.name);
        ViewGroup tz = (ViewGroup) v.findViewById(R.id.tz);
        mTimeZone = (EditText) tz.findViewById(R.id.timezonefix);
        tz.findViewById(R.id.plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    float h = Float.parseFloat(mTimeZone.getText().toString());
                    h += 0.5;
                    mTimeZone.setText(h + "");
                } catch (Exception e) {
                }
            }
        });

        tz.findViewById(R.id.minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    double h = Double.parseDouble(mTimeZone.getText().toString());
                    h -= 0.5;
                    mTimeZone.setText(h + "");
                } catch (Exception e) {
                }
            }
        });

        mTimeZone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mTimes == null) return;
                try {
                    mTimes.setTZFix(Double.parseDouble(editable.toString()));
                } catch (Exception ignore) {
                }

            }
        });
        ViewGroup vg = (ViewGroup) v.findViewById(R.id.minAdj);
        for (int i = 1; i < 7; i++) {
            final int ii = i - 1;
            ViewGroup time = (ViewGroup) vg.getChildAt(i);
            mMins[ii] = (EditText) time.findViewById(R.id.nr);
            mPlus[ii] = (ImageView) time.findViewById(R.id.plus);
            mMinus[ii] = (ImageView) time.findViewById(R.id.minus);
            mPlus[ii].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        int min = Integer.parseInt(mMins[ii].getText().toString());
                        min++;
                        mMins[ii].setText(min + "");
                    } catch (Exception e) {
                    }
                }
            });

            mMinus[ii].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        int min = Integer.parseInt(mMins[ii].getText().toString());
                        min--;
                        mMins[ii].setText(min + "");
                    } catch (Exception e) {
                    }
                }
            });

            mMins[ii].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (mTimes == null) return;
                    try {
                        mMinAdj[ii] = Integer.parseInt(editable.toString());
                    } catch (Exception ignore) {
                    }
                    mTimes.setMinuteAdj(mMinAdj);
                }
            });
        }


        mName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mTimes != null) mTimes.setName(editable.toString());
            }
        });

        setTimes(mTimes);
        return v;
    }


    public void setTimes(Times t) {
        if (mName != null && t != null) {
            mTimes = null;
            mMinAdj = t.getMinuteAdj();
            for (int i = 0; i < mMins.length; i++) {
                mMins[i].setText(mMinAdj[i] + "");
            }
            mName.setText(t.getName());
            mTimeZone.setText(t.getTZFix() + "");
        }
        mTimes = t;
    }
}
