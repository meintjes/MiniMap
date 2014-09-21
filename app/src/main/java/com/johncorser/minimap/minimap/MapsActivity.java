package com.johncorser.minimap.minimap;

import android.app.ActionBar;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.SeekBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity {


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private boolean moved = false;
    private final int MIN_ZOOM = 14;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setMyLocationEnabled(true);
            // Check if we were successful in obtaining the map.

            double delta = 0.1;
            List<LatLng> points = Arrays.asList(new LatLng(90, -180),
                    new LatLng(-90 + delta, -180 + delta),
                    new LatLng(-90 + delta, 0),
                    new LatLng(-90 + delta, 180 - delta),
                    new LatLng(0, 180 - delta),
                    new LatLng(90 - delta, 180 - delta),
                    new LatLng(90 - delta, 0),
                    new LatLng(90 - delta, -180 + delta),
                    new LatLng(0, -180 + delta));

            PolygonOptions options = new PolygonOptions();

            options.addAll(points);
            options.fillColor(Color.rgb(0, 0, 0));
            options.strokeWidth(0);
            final Polygon worldOverlay = mMap.addPolygon(options);

            if (mMap != null) {
                mMap.getUiSettings().setZoomControlsEnabled(false);
                mMap.getUiSettings().setZoomGesturesEnabled(false);
                mMap.getUiSettings().setTiltGesturesEnabled(false);
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                SeekBar zoomBar = (SeekBar) findViewById(R.id.zoomBar);
                zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(MIN_ZOOM + progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }
                });

                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {

                        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

                            @Override
                            public void onMyLocationChange(Location arg0) {

                                if (!moved) {
                                    CameraUpdate center =
                                            CameraUpdateFactory.newLatLngZoom(new LatLng(arg0.getLatitude(), arg0.getLongitude()), 16);
                                    mMap.moveCamera(center);
                                }
                                moved = true;

                                FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getApplicationContext());
                                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                                ContentValues values = new ContentValues();
                                LatLng currentLocation = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                                List<LatLng> data = new ArrayList<LatLng>();


                                Cursor cursor = db.rawQuery("select " + FeedReaderDbHelper.COLUMN_LATITUDE + "," + FeedReaderDbHelper.COLUMN_LONGITUDE + " from locations;", new String[]{});
                                cursor.moveToFirst();
                                while (!cursor.isAfterLast()) {
                                    data.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
                                    //Log.e("HTN", data.toString());
                                    cursor.moveToNext();
                                }
                                // make sure to close the cursor
                                cursor.close();

                                if (isFarAway(data, currentLocation)) {
                                    values.put(FeedReaderDbHelper.COLUMN_LATITUDE, arg0.getLatitude());
                                    values.put(FeedReaderDbHelper.COLUMN_LONGITUDE, arg0.getLongitude());
                                    long id = db.insert(FeedReaderDbHelper.TABLE_LOCATIONS, null, values);

                                }


                                if (mMap.getCameraPosition().zoom >= MIN_ZOOM) {
                                    List<List<LatLng>> holes = new ArrayList<List<LatLng>>();
                                    LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                                    for (double longBound = Math.floor(bounds.southwest.longitude * 1000) / 1000; longBound < bounds.northeast.longitude; longBound += 0.001) {
                                        for (double latBound = Math.floor(bounds.southwest.latitude * 1000) / 1000; latBound < bounds.northeast.latitude; latBound += 0.001) {
                                            double squareSize = 0.001;
                                            boolean hasBeenVisited = false;
                                            for (LatLng location : data) {
                                                if (location.latitude >= latBound && location.latitude <= latBound + squareSize && location.longitude >= longBound && location.longitude <= longBound + squareSize) {
                                                    hasBeenVisited = true;
                                                    break;
                                                }
                                            }
                                            if (hasBeenVisited) {
                                                List<LatLng> visitedPoints = Arrays.asList(
                                                        new LatLng(latBound + 0.000001, longBound + 0.000001),
                                                        new LatLng(latBound + 0.000001, longBound + squareSize),
                                                        new LatLng(latBound + squareSize, longBound + squareSize),
                                                        new LatLng(latBound + squareSize, longBound + 0.000001)
                                                );
                                                holes.add(visitedPoints);

                                            }
                                        }
                                    }
                                    worldOverlay.setHoles(holes);

                                }


                            }
                        });
                    }
                });
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
    private boolean isFarAway(List<LatLng> data, LatLng currentLocation) {
        for (LatLng location : data){
            if (Math.floor(location.latitude*1000)/1000 == Math.floor(currentLocation.latitude*1000)/1000 ){
                if (Math.floor(location.longitude*1000)/1000 == Math.floor(currentLocation.longitude*1000)/1000){
                    return false;
                }
            }
        }
        return true;
    }
    private ArrayList<LatLng> getCircle(LatLng location){
        double lat = location.latitude;
        double lon = location.longitude;
        ArrayList<LatLng> visitedArea = new ArrayList<LatLng>();
        for (double i=0; i<360; i+=4){

            LatLng piece = new LatLng(lat + Math.sin(Math.toRadians(i)), lon + Math.cos(Math.toRadians(i)));
            visitedArea.add(piece);
        }
        return visitedArea;
    }
    private void breakIntoSquares(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }

}
