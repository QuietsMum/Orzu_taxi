package org.orzu.taxi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MapView map = null;
    private ItemizedOverlayWithFocus<OverlayItem> mMyLocationOverlay;
    private MyLocationNewOverlay mLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }else{
            // Write you code here if permission already given.
        }

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        map.setMultiTouchControls(true);


        //Copyright overlay
        String copyrightNotice = map.getTileProvider().getTileSource().getCopyrightNotice();
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(this);
        copyrightOverlay.setCopyrightNotice(copyrightNotice);
        map.getOverlays().add(copyrightOverlay);

        /* Itemized Overlay */
        {
            /* Create a static ItemizedOverlay showing a some Markers on some cities. */
            final ArrayList<OverlayItem> items = new ArrayList<>();
            items.add(new OverlayItem("Hannover", "SampleDescription",
                    new GeoPoint(43.25654, 76.82848)));
            items.add(new OverlayItem("Berlin", "SampleDescription",
                    new GeoPoint(43.25654, 76.72848)));
            items.add(new OverlayItem("Washington", "SampleDescription",
                    new GeoPoint(43.25654, 76.62848)));
            items.add(new OverlayItem("San Francisco", "SampleDescription",
                    new GeoPoint(43.25654, 76.78848)));
            items.add(new OverlayItem("Tolaga Bay", "SampleDescription",
                    new GeoPoint(-43.25654, 76.75848)));

            /* OnTapListener for the Markers, shows a simple Toast. */
            this.mMyLocationOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Item '" + item.getTitle() + "' (index=" + index
                                            + ") got single tapped up", Toast.LENGTH_LONG).show();
                            return true; // We 'handled' this event.
                        }

                        @Override
                        public boolean onItemLongPress(final int index, final OverlayItem item) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Item '" + item.getTitle() + "' (index=" + index
                                            + ") got long pressed", Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }, getApplicationContext());
            this.map.getOverlays().add(this.mMyLocationOverlay);
        }

        /* MiniMap */
        {
            final MinimapOverlay miniMapOverlay = new MinimapOverlay(this,
                    map.getTileRequestCompleteHandler());
            this.map.getOverlays().add(miniMapOverlay);
        }

        /* list of items currently displayed */
        {
            final MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    return false;
                }

                @Override
                public boolean longPressHelper(final GeoPoint p) {
                    final List<OverlayItem> displayed = mMyLocationOverlay.getDisplayedItems();
                    final StringBuilder buffer = new StringBuilder();
                    String sep = "";
                    for (final OverlayItem item : displayed) {
                        buffer.append(sep).append('\'').append(item.getTitle()).append('\'');
                        sep = ", ";
                    }
                    Toast.makeText(
                            MainActivity.this,
                            "Currently displayed: " + buffer.toString(), Toast.LENGTH_LONG).show();
                    return true;
                }
            };
            map.getOverlays().add(new MapEventsOverlay(mReceive));

            final RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
            rotationGestureOverlay.setEnabled(true);
            map.getOverlays().add(rotationGestureOverlay);
        }

        IMapController mapController = map.getController();
        mapController.setZoom(12.5);
        GeoPoint startPoint = new GeoPoint(43.25654, 76.92848);
        mapController.setCenter(startPoint);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(MainActivity.this),map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);


    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

}
