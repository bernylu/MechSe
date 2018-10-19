package com.example.bernyspc.mechse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaruserMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mRequest, mSettings;

    private LatLng vehicleLocation;

    private Boolean requestBol  = false;

    private Marker vehicleMarker;

    private String destination, requestServices;

    private LatLng destinationLatLng;

    private LinearLayout mMechInfo;

    private ImageView mMechProfileImage;

    private TextView mMechName,mMechPhone,mMechService;

    private RadioGroup mRadioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caruser_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(CaruserMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }
        mapFragment.getMapAsync(this);

    destinationLatLng = new LatLng(0.0,0.0);
    mMechInfo = (LinearLayout)findViewById(R.id.MechInfo);
    mMechProfileImage = (ImageView)findViewById(R.id.mechProfileImage);
    mMechName = (TextView) findViewById(R.id.mechName);
    mMechPhone = (TextView) findViewById(R.id.mechPhone);
    mMechService = (TextView)findViewById(R.id.mechService);

        mRadioGroup = (RadioGroup) findViewById(R.id.RadioGroup);
        mRadioGroup.check(R.id.generalservices);


        mLogout = (Button)findViewById(R.id.logout);
        mRequest =(Button)findViewById(R.id.request);
        mSettings =(Button)findViewById(R.id.settings);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CaruserMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if (requestBol){
                   requestBol = false;
                   geoQuery.removeAllListeners();
                   mechLocationRef.removeEventListener(mechLocationRefListener);

                   if (mechFoundId !=null){
                       DatabaseReference mechRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechFoundId).child("caruserRequest");
                        mechRef.removeValue();
                        mechFoundId = null;
                   }
                   mechFound = false;
                   radius = 1;
                   String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                   DatabaseReference ref = FirebaseDatabase.getInstance().getReference("caruserRequest");
                   GeoFire geoFire = new GeoFire(ref);
                   geoFire.removeLocation(userId);

                if (vehicleMarker !=null){
                    vehicleMarker.remove();
                }
                mRequest.setText("call mechanic");

               }else {

                   int selectId = mRadioGroup.getCheckedRadioButtonId();

                   final RadioButton radioButton = (RadioButton) findViewById(selectId);

                   if (radioButton.getText() == null){
                       return;
                   }

                   requestServices = radioButton.getText().toString();

                   requestBol = true;
                   String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                   DatabaseReference ref = FirebaseDatabase.getInstance().getReference("caruserRequest");
                   GeoFire geoFire = new GeoFire(ref);
                   geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                   vehicleLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                   vehicleMarker = mMap.addMarker(new MarkerOptions().position(vehicleLocation).title("Come Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_ha)));

                   mRequest.setText("Getting your mechanic...");
                   mMechInfo.setVisibility(View.GONE);
                   mMechName.setText("");
                   mMechPhone.setText("");
                   mMechService.setText("");
                   mMechProfileImage.setImageResource(R.mipmap.ic_launcher_ha);
                   getClosestMech();
               }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CaruserMapActivity.this,CaruserSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.

        }

    }
    private int radius =1;
    private Boolean mechFound = false;
    private String mechFoundId;

    GeoQuery geoQuery;
    private void getClosestMech(){
        DatabaseReference MechLocation = FirebaseDatabase.getInstance().getReference().child("mechAvailable");

        GeoFire geoFire = new GeoFire(MechLocation);
         geoQuery = geoFire.queryAtLocation(new GeoLocation(vehicleLocation.latitude,vehicleLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!mechFound && requestBol){
                    DatabaseReference mCaruserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(key);
                    mCaruserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<String, Object> Mechmap = (Map<String, Object>) dataSnapshot.getValue();
                                if (mechFound) {
                                    return;
                                }

                                if (Mechmap.get("services").equals("engineservices")) {
                                    mechFound = true;
                                    mechFoundId = dataSnapshot.getKey();

                                    DatabaseReference mechRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechFoundId).child("caruserRequest");
                                    String caruserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("caruserConsolId", caruserId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    mechRef.updateChildren(map);

                                    getMechLocation();
                                    getMechInfo();
                                    mRequest.setText("looking for mechanic location...");
                                }

                            }
                        }



                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                mechFound = true;
                mechFoundId = key;

                DatabaseReference mechRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechFoundId).child("caruserRequest");
                String caruserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("caruserConsolId",caruserId);
                    map.put("destination",destination);
                    mechRef.updateChildren(map);

                    getMechLocation();
                    getMechInfo();
                    mRequest.setText("looking for mechanic location...");
            }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!mechFound){
                    radius++;
                    getClosestMech();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker mMechMarker;
    private  DatabaseReference mechLocationRef;
    private ValueEventListener mechLocationRefListener;
    private void getMechLocation(){
         mechLocationRef = FirebaseDatabase.getInstance().getReference().child("MechanicsWorking").child(mechFoundId).child("l");
       mechLocationRefListener = mechLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Mechanic Found");
                    if (map.get(0) !=null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(0) !=null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng mechLatLng = new LatLng(locationLat,locationLng);
                    if (mMechMarker != null){
                        mMechMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(vehicleLocation.latitude);
                    loc1.setLongitude(vehicleLocation.longitude);

                    Location loc2 = new Location("");
                    loc1.setLatitude(mechLatLng.latitude);
                    loc1.setLongitude(mechLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100){

                    mRequest.setText("Mechanic is here");
                    }else {
                        mRequest.setText("Mechanic Found:" +String.valueOf(distance));
                    }

                    mMechMarker = mMap.addMarker(new MarkerOptions().position(mechLatLng).title("Your mechanic").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_me)));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void getMechInfo(){
        mMechInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCaruserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechFoundId);

        mCaruserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if (map.get("name")!=null){
                        mMechName.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!=null){
                        mMechPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("service")!=null){
                        mMechService.setText(map.get("service").toString());
                    }
                    if (map.get("profileImageUrl")!=null){

                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mMechProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }
    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                ActivityCompat.requestPermissions(CaruserMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);

    final int LOCATION_REQUEST_CODE =1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);

                }
                else {
                    Toast.makeText(getApplicationContext(),"please provide the permission",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    @Override
    protected void onStop() {
        super.onStop();


    }
}



