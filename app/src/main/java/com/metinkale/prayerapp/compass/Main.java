package com.metinkale.prayerapp.compass;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.App;
import com.metinkale.prayerapp.BaseActivity;
import com.metinkale.prayerapp.PermissionUtils;
import com.metinkale.prayerapp.compass._2D.Frag2D;
import com.metinkale.prayerapp.compass._3D.Frag3D;
import com.metinkale.prayerapp.compass.classes.OrientationCalculator;
import com.metinkale.prayerapp.compass.classes.OrientationCalculatorImpl;
import com.metinkale.prayerapp.compass.classes.math.Matrix4;
import com.metinkale.prayerapp.compass.classes.rotation.MagAccelListener;
import com.metinkale.prayerapp.compass.classes.rotation.RotationUpdateDelegate;
import com.metinkale.prayerapp.settings.Prefs;

import java.util.List;

public class Main extends BaseActivity implements LocationListener, RotationUpdateDelegate {

    static private double mQAngle;
    static private float mDist;
    public MagAccelListener mMagAccel;
    private Matrix4 mRotationMatrix = new Matrix4();
    private int mDisplayRotation;
    private SensorManager mSensorManager;
    private TextView mSelCity;
    private MenuItem mRefresh;
    private boolean mOnlyNew;
    private MyCompassListener mList;
    private OrientationCalculator mOrientationCalculator = new OrientationCalculatorImpl();
    private float[] mDerivedDeviceOrientation = {0, 0, 0};
    private boolean m3D;
    private Frag2D mFrag2D;
    private Frag3D mFrag3D;

    public static float getDistance() {
        return mDist;
    }

    public static double getQiblaAngle() {
        return mQAngle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compass_main);
        PermissionUtils.get(this).needLocation(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        final Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // sensor listeners
        mMagAccel = new MagAccelListener(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        mFrag2D = new Frag2D();
        mList = mFrag2D;
        fragmentTransaction.add(R.id.frag2D, mFrag2D, "2d");
        fragmentTransaction.commit();
    }

    private void updateFrag(boolean is3d) {


        if (is3d != m3D) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if (is3d) {

                if (PermissionUtils.get(this).pCamera) {

                    if (mFrag3D == null)
                        mFrag3D = new Frag3D();
                    if (mList != mFrag3D) {
                        fragmentTransaction.add(R.id.frag3D, mFrag3D, "3d");

                        mList = mFrag3D;
                        mFrag2D.hide();
                    }
                } else {
                    PermissionUtils.get(this).needCamera(this);
                }

            } else if (mFrag2D.mHidden) {
                fragmentTransaction.remove((Fragment) mList);
                mList = mFrag2D;
                mFrag2D.show();
            }

            fragmentTransaction.commit();
        }

        m3D = is3d;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mRefresh == item) {
            mOnlyNew = true;
            if (PermissionUtils.get(this).pLocation) {
                LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                locMan.removeUpdates(this);
                List<String> providers = locMan.getProviders(true);
                for (String provider : providers) {
                    locMan.requestLocationUpdates(provider, 0, 0, this);
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mRefresh = menu.add(Menu.NONE, Menu.NONE, 1, R.string.refresh);
        MenuItemCompat.setShowAsAction(mRefresh, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.get(this).pLocation) {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            List<String> providers = locMan.getProviders(true);
            for (String provider : providers) {
                locMan.requestLocationUpdates(provider, 0, 0, this);
                Location lastKnownLocation = locMan.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    calcQiblaAngel(lastKnownLocation);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.unregisterListener(mMagAccel);

        mSensorManager.registerListener(mMagAccel, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mMagAccel, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);

        if (mSelCity == null) {
            mSelCity = (TextView) findViewById(R.id.selcity);
            mSelCity.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (App.isOnline()) {
                        startActivity(new Intent(Main.this, LocationPicker.class));
                    } else {
                        Toast.makeText(Main.this, R.string.noConnection, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        if (PermissionUtils.get(this).pLocation) {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            List<String> providers = locMan.getProviders(true);
            for (String provider : providers) {
                locMan.requestLocationUpdates(provider, 0, 0, this);
                Location lastKnownLocation = locMan.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    calcQiblaAngel(lastKnownLocation);
                }
            }
        }

        if (Prefs.getCompassLat() != 0) {
            Location loc = new Location("custom");
            loc.setLatitude(Prefs.getCompassLat());
            loc.setLongitude(Prefs.getCompassLng());
            calcQiblaAngel(loc);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.get(this).pCamera) {
            m3D = false;
        }
    }

    // RotationUpdateDelegate methods
    @Override
    public void onRotationUpdate(float[] newMatrix) {

        // remap matrix values according to display rotation, as in
        // SensorManager documentation.
        switch (mDisplayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(newMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, newMatrix);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(newMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, newMatrix);
                break;
            default:
                break;
        }
        mRotationMatrix.set(newMatrix);
        mOrientationCalculator.getOrientation(mRotationMatrix, mDisplayRotation, mDerivedDeviceOrientation);

        updateFrag(mDerivedDeviceOrientation[1] > -55f);

        mList.onUpdateSensors(mDerivedDeviceOrientation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mMagAccel);
        if (PermissionUtils.get(this).pLocation) {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locMan.removeUpdates(this);
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        calcQiblaAngel(location);
        if (System.currentTimeMillis() - location.getTime() < (mOnlyNew ? 1000 * 60 : 1000 * 60 * 60 * 24)) {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locMan.removeUpdates(this);
        }

    }

    private void calcQiblaAngel(Location location) {
        if (location.getProvider() != "custom") {
            mSelCity.setVisibility(View.GONE);
        }
        double lat1 = location.getLatitude();// Latitude of Desired Location
        double lng1 = location.getLongitude();// Longitude of Desired Location
        double lat2 = 21.42247;// Latitude of Mecca (+21.45° north of Equator)
        double lng2 = 39.826198;// Longitude of Mecca (-39.75° east of Prime
        // Meridian)

        double q = -getDirection(lat1, lng1, lat2, lng2);

        Location mLoc = new Location(location);
        mLoc.setLatitude(lat2);
        mLoc.setLongitude(lng2);
        mQAngle = q;
        mDist = location.distanceTo(mLoc) / 1000;
        mList.onUpdateDirection();

    }

    private double getDirection(double lat1, double lng1, double lat2, double lng2) {
        double dLng = lng1 - lng2;
        return Math.toDegrees(getDirectionRad(Math.toRadians(lat1), Math.toRadians(lat2), Math.toRadians(dLng)));
    }

    private double getDirectionRad(double lat1, double lat2, double dLng) {
        return Math.atan2(Math.sin(dLng), Math.cos(lat1) * Math.tan(lat2) - Math.sin(lat1) * Math.cos(dLng));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public boolean setNavBar() {
        return false;
    }

    public interface MyCompassListener {
        void onUpdateDirection();

        void onUpdateSensors(float[] rot);
    }

}