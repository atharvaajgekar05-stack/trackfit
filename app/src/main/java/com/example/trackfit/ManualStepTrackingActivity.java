package com.example.trackfit;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ManualStepTrackingActivity extends AppCompatActivity {

    private static final String TAG = "ManualStepTracking";

    private MapView map;
    private TextView tvStartLocation, tvDestination;
    private TextView tvDistanceResult, tvStepsResult, tvCalculatedVia;
    private Button btnCalculateSteps, btnSaveSteps;
    private LinearLayout layoutResults;

    private PlaceInfo startPlace;
    private PlaceInfo destinationPlace;

    private double calculatedDistanceKm = 0;
    private int estimatedSteps = 0;
    private boolean isStartSelection = true;
    private RequestQueue requestQueue;

    // Helper class to store Nominatim result
    private static class PlaceInfo {
        String name;
        GeoPoint point;
        PlaceInfo(String name, GeoPoint point) {
            this.name = name;
            this.point = point;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required by OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_manual_step_tracking);

        requestQueue = Volley.newRequestQueue(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12.0);
        mapController.setCenter(new GeoPoint(19.0760, 72.8777)); // Default Mumbai, Maharashtra, India

        tvStartLocation = findViewById(R.id.tvStartLocation);
        tvDestination = findViewById(R.id.tvDestination);
        layoutResults = findViewById(R.id.layoutResults);
        tvDistanceResult = findViewById(R.id.tvDistanceResult);
        tvStepsResult = findViewById(R.id.tvStepsResult);
        btnCalculateSteps = findViewById(R.id.btnCalculateSteps);
        btnSaveSteps = findViewById(R.id.btnSaveSteps);
        tvCalculatedVia = findViewById(R.id.tvCalculatedVia);

        tvStartLocation.setOnClickListener(v -> openPlacePickerDialog(true));
        tvDestination.setOnClickListener(v -> openPlacePickerDialog(false));

        btnCalculateSteps.setOnClickListener(v -> calculateRouteAndSteps());
        btnSaveSteps.setOnClickListener(v -> saveStepsSession());
    }

    private void openPlacePickerDialog(boolean isStart) {
        isStartSelection = isStart;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_place, null);
        builder.setView(dialogView);

        EditText etSearchQuery = dialogView.findViewById(R.id.etSearchQuery);
        Button btnSearch = dialogView.findViewById(R.id.btnSearch);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        ListView lvSearchResults = dialogView.findViewById(R.id.lvSearchResults);

        AlertDialog dialog = builder.create();
        
        // Basic list adapter
        List<String> displayNames = new ArrayList<>();
        List<PlaceInfo> resultPlaces = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_search_result, R.id.tvPlaceName, displayNames);
        lvSearchResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String q = etSearchQuery.getText().toString().trim();
            if (q.isEmpty()) return;

            progressBar.setVisibility(View.VISIBLE);
            lvSearchResults.setVisibility(View.GONE);

            String url = "https://nominatim.openstreetmap.org/search?q=" + android.net.Uri.encode(q) + "&format=json&limit=5";
            JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        lvSearchResults.setVisibility(View.VISIBLE);
                        displayNames.clear();
                        resultPlaces.clear();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String name = obj.getString("display_name");
                                double lat = obj.getDouble("lat");
                                double lon = obj.getDouble("lon");
                                displayNames.add(name);
                                resultPlaces.add(new PlaceInfo(name, new GeoPoint(lat, lon)));
                            }
                            adapter.notifyDataSetChanged();
                            if (displayNames.isEmpty()) {
                                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "TrackFitApp/1.0");
                    return headers;
                }
            };
            requestQueue.add(req);
        });

        lvSearchResults.setOnItemClickListener((parent, view, position, id) -> {
            PlaceInfo selected = resultPlaces.get(position);
            if (isStartSelection) {
                startPlace = selected;
                tvStartLocation.setText(selected.name);
            } else {
                destinationPlace = selected;
                tvDestination.setText(selected.name);
            }
            resetResults();
            updateMapMarkers();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateMapMarkers() {
        if (map == null) return;
        map.getOverlays().clear();

        List<GeoPoint> points = new ArrayList<>();

        if (startPlace != null) {
            Marker mStart = new Marker(map);
            mStart.setPosition(startPlace.point);
            mStart.setTitle("Start");
            mStart.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(mStart);
            points.add(startPlace.point);
        }

        if (destinationPlace != null) {
            Marker mEnd = new Marker(map);
            mEnd.setPosition(destinationPlace.point);
            mEnd.setTitle("Destination");
            mEnd.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(mEnd);
            points.add(destinationPlace.point);
        }

        if (!points.isEmpty()) {
            if (points.size() == 1) {
                map.getController().animateTo(points.get(0));
            } else {
                BoundingBox boundingBox = BoundingBox.fromGeoPoints(points);
                // Wait for layout before zooming
                map.post(() -> map.zoomToBoundingBox(boundingBox, true, 100));
            }
        }
        map.invalidate();
    }

    private void resetResults() {
        layoutResults.setVisibility(View.GONE);
        btnSaveSteps.setVisibility(View.GONE);
        tvCalculatedVia.setVisibility(View.GONE);
        btnCalculateSteps.setVisibility(View.VISIBLE);
        calculatedDistanceKm = 0;
        estimatedSteps = 0;
    }

    private void calculateRouteAndSteps() {
        if (startPlace == null || destinationPlace == null) {
            Toast.makeText(this, "Please select start location and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCalculateSteps.setEnabled(false);
        btnCalculateSteps.setText("Calculating...");

        // OSRM coordinates format: lon,lat
        String coords = startPlace.point.getLongitude() + "," + startPlace.point.getLatitude() + ";" +
                destinationPlace.point.getLongitude() + "," + destinationPlace.point.getLatitude();

        String url = "https://router.project-osrm.org/route/v1/walking/" + coords + "?overview=full&geometries=geojson";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    btnCalculateSteps.setEnabled(true);
                    btnCalculateSteps.setText("Calculate Steps");
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            
                            double distanceMeters = route.getDouble("distance");
                            calculatedDistanceKm = distanceMeters / 1000.0;
                            estimatedSteps = (int) Math.round(distanceMeters / 0.75);

                            JSONObject geometry = route.getJSONObject("geometry");
                            JSONArray coordinates = geometry.getJSONArray("coordinates");
                            drawRouteOnMap(coordinates);

                            showResults();
                        } else {
                            Toast.makeText(this, "No walking route found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing route.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnCalculateSteps.setEnabled(true);
                    btnCalculateSteps.setText("Calculate Steps");
                    Log.e(TAG, "Volley error: " + error.toString());
                    Toast.makeText(this, "Error fetching route. Check network.", Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "TrackFitApp/1.0");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void drawRouteOnMap(JSONArray coordinates) throws JSONException {
        if (map == null) return;
        
        // Remove old lines if any, keep markers
        map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);

        List<GeoPoint> geoPoints = new ArrayList<>();
        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray coord = coordinates.getJSONArray(i);
            // GeoJSON is [lon, lat]
            double lon = coord.getDouble(0);
            double lat = coord.getDouble(1);
            geoPoints.add(new GeoPoint(lat, lon));
        }

        Polyline line = new Polyline(map);
        line.setPoints(geoPoints);
        line.getOutlinePaint().setColor(Color.parseColor("#39cccc"));
        line.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(0, line); // Add behind markers
        map.invalidate();

        if (!geoPoints.isEmpty()) {
            map.post(() -> map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), true, 100));
        }
    }

    private void showResults() {
        tvDistanceResult.setText(String.format(Locale.getDefault(), "%.1f km", calculatedDistanceKm));
        tvStepsResult.setText(String.format(Locale.getDefault(), "%,d", estimatedSteps));

        btnCalculateSteps.setVisibility(View.GONE);
        layoutResults.setVisibility(View.VISIBLE);
        btnSaveSteps.setVisibility(View.VISIBLE);
        tvCalculatedVia.setVisibility(View.VISIBLE);
    }

    private void saveStepsSession() {
        if (estimatedSteps <= 0) {
            Toast.makeText(this, "No valid steps to save", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();

        Map<String, Object> session = new HashMap<>();
        session.put("manual_step_log_id", UUID.randomUUID().toString());
        session.put("user_id", mAuth.getCurrentUser().getUid());
        session.put("start_location_name", startPlace.name);
        session.put("destination_location_name", destinationPlace.name);
        session.put("distance_km", calculatedDistanceKm);
        session.put("estimated_steps", estimatedSteps);
        session.put("date", dateFormat.format(now));
        session.put("time", timeFormat.format(now));
        session.put("created_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("manual_step_logs")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Steps saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
