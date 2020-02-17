package lcwu.fyp.autocareapp.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.director.Constants;
import lcwu.fyp.autocareapp.director.Helpers;
import lcwu.fyp.autocareapp.director.Session;
import lcwu.fyp.autocareapp.model.Booking;
import lcwu.fyp.autocareapp.model.Notification;
import lcwu.fyp.autocareapp.model.User;

public class Dashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Bookings");
    private ValueEventListener providerValueListener, bookingValueListener, bookingsValueListener, providerDetailValueListener;
    private MapView map;
    private Helpers helpers;
    private Session session;
    private GoogleMap googleMap;
    private DrawerLayout drawer;
    private User user;
    private CircleImageView providerImage;
    private TextView providerName;
    private TextView providerCategory;
    private TextView bookingAddress;
    private TextView bookingDate;
    private FusedLocationProviderClient locationProviderClient;
    private Marker marker, activeProviderMarker;
    private TextView locationAddress;
    private Spinner selecttype;
    private LinearLayout searching, bookingMain;
    private CardView confirmCard;
    private ProgressBar sheetProgress;
    private RelativeLayout mainSheet;
    private Booking activeBooking;
    private User activeProvider;
    private BottomSheetBehavior sheetBehavior;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        sheetProgress = findViewById(R.id.sheetProgress);
        mainSheet = findViewById(R.id.mainSheet);
        providerImage = findViewById(R.id.providerImage);
        providerName = findViewById(R.id.providerName);
        providerCategory = findViewById(R.id.providerCategory);
        bookingAddress = findViewById(R.id.bookingAddress);
        bookingDate = findViewById(R.id.bookingDate);
        Button cancelBooking = findViewById(R.id.cancelBooking);
        cancelBooking.setOnClickListener(this);


        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        selecttype = findViewById(R.id.selecttype);
        CheckBox showmechanics = findViewById(R.id.showmechanics);
        CheckBox showpetrolpumps = findViewById(R.id.showpetrolpumps);
        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
        searching = findViewById(R.id.searching);
        bookingMain = findViewById(R.id.bookingMain);
        confirmCard = findViewById(R.id.confirmCard);
        session = new Session(Dashboard.this);
        user = session.getUser();
        helpers = new Helpers();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(Dashboard.this);


        View header = navigationView.getHeaderView(0);
        TextView profile_email = header.findViewById(R.id.profile_email);
        TextView profile_name = header.findViewById(R.id.profile_name);
        CircleImageView profile_image = header.findViewById(R.id.profile_image);
        TextView profile_phone = header.findViewById(R.id.profile_phone);

        String name = user.getFirstName() + " " + user.getLastName();
        profile_name.setText(name);
        profile_email.setText(user.getEmail());
        profile_phone.setText(user.getPhone());
        if (user.getImage() != null && user.getImage().length() > 1) {
            Glide.with(Dashboard.this).load(user.getImage()).into(profile_image);
        }

        locationAddress = findViewById(R.id.locationAddress);


        map = findViewById(R.id.map);
        map.onCreate(savedInstanceState);
        try {
            MapsInitializer.initialize(Dashboard.this);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gM) {
                    Log.e("Dashboard", "Maps Call back received");


                    View locationButton = ((View) map.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                    RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                    // position on right bottom
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    rlp.setMargins(0, 350, 100, 0);

                    googleMap = gM;
                    LatLng defaultPosition = new LatLng(31.5204, 74.3487);
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(defaultPosition).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    enableLocation();

                }
            });

        } catch (Exception e) {
            helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }

    }

    private boolean askForPermission() {
        if (ActivityCompat.checkSelfPermission(Dashboard.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(Dashboard.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Dashboard.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            return false;
        }
        return true;
    }

    public void enableLocation() {
        if (askForPermission()) {
            googleMap.setMyLocationEnabled(true);
            googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(Dashboard.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation();
            getAllProviders();
            listenToBookingsChanges();
        }
    }

    private void getDeviceLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);

            }
            if (!gps_enabled && !network_enabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(Dashboard.this);
                dialog.setMessage("Oppsss.Your Location Service is off.\n Please turn on your Location and Try again Later");
                dialog.setPositiveButton("Let me On", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                });
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                dialog.show();
                return;
            }

            locationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        if (location != null) {
                            googleMap.clear();
                            LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                            marker = googleMap.addMarker(new MarkerOptions().position(me).title("You're Here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 11));
                            Geocoder geocoder = new Geocoder(Dashboard.this);
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(me.latitude, me.longitude, 1);
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    String strAddress = "";
                                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                        strAddress = strAddress + " " + address.getAddressLine(i);
                                    }
                                    locationAddress.setText(strAddress);
                                    updateUserLocation(me.latitude, me.longitude);
                                }
                            } catch (Exception exception) {
                                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                }
            });
        } catch (Exception e) {
            helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation();
            }
        }
    }

    private void getAllProviders() {
        providerValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                googleMap.clear();
                marker = googleMap.addMarker(new MarkerOptions().position(marker.getPosition()).title("You're Here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 11));
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    User u = data.getValue(User.class);
                    if (u != null) {
                        LatLng user_location = new LatLng(u.getLatidue(), u.getLongitude());
                        MarkerOptions markerOptions = new MarkerOptions().position(user_location).title(u.getType());
                        switch (u.getType()) {
                            case "Car Mechanic":
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.carmechanic));
                                break;
                            case "Bike Mechanic":
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bikemechanic));
                                break;
                            case "Petrol Provider":
                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.petrolpump));
                                break;
                        }
                        Marker marker = googleMap.addMarker(markerOptions);
                        marker.showInfoWindow();
                        marker.setTag(u);
                        Log.e("Dashboard", "Name: " + u.getFirstName() + " Lat: " + u.getLatidue() + " Lng: " + u.getLongitude());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        userReference.orderByChild("roll").equalTo(1).addValueEventListener(providerValueListener);
    }

    private void listenToBookingsChanges() {
        bookingsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingReference.removeEventListener(bookingsValueListener);
                Log.e("Dashboard", "Bookings Value Event Listener");
                if (activeBooking == null) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        Booking booking = d.getValue(Booking.class);
                        if (booking != null) {
                            Log.e("Dashboard", "Bookings Value Event Listener, Booking found with status: " + booking.getStatus());
                            if (booking.getStatus().equals("In Progress")) {
                                activeBooking = booking;
                                listenToBookingChanges();
                                onBookingInProgress();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                bookingReference.removeEventListener(bookingsValueListener);
            }
        };

        bookingReference.orderByChild("userId").equalTo(user.getPhone()).addValueEventListener(bookingsValueListener);
    }

    private void updateUserLocation(double lat, double lng) {
        user.setLatidue(lat);
        user.setLongitude(lng);
        session.setSession(user);
        userReference.child(user.getPhone()).setValue(user);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        Log.e("Dashboard", "" + id);
        switch (id) {
            case R.id.nav_home: {
                break;
            }
            case R.id.nav_booking: {
                Intent it = new Intent(Dashboard.this, BookingActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_notification: {
                Intent it = new Intent(Dashboard.this, NotificationActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_userProfile: {
                Intent it = new Intent(Dashboard.this, EditUserProfile.class);
                startActivity(it);
                break;
            }
            case R.id.nav_logout: {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                Session session = new Session(Dashboard.this);
                auth.signOut();
                session.destroySession();
                Intent it = new Intent(Dashboard.this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
                finish();
                break;
            }
            case R.id.became_a_provider: {
                Intent it = new Intent(Dashboard.this, BecameProvider.class);
                startActivity(it);
                break;
            }
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();

        if (providerValueListener != null) {
            userReference.removeEventListener(providerValueListener);
        }

        if (providerDetailValueListener != null) {
            userReference.removeEventListener(providerDetailValueListener);
        }

        if (bookingValueListener != null) {
            bookingReference.removeEventListener(bookingValueListener);
        }

        if (bookingsValueListener != null) {
            bookingReference.removeEventListener(bookingsValueListener);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.confirm: {
                if (!helpers.isConnected(Dashboard.this)) {
                    helpers.showNoInternetError(Dashboard.this);
                    return;
                }
                if (selecttype.getSelectedItemPosition() == 0) {
                    helpers.showError(Dashboard.this, "Select your type first");
                    return;
                }
                postBooking();
                break;
            }
            case R.id.cancelBooking: {
                Log.e("Dashboard", "Cancel button clicked");
                mainSheet.setVisibility(View.GONE);
                sheetProgress.setVisibility(View.VISIBLE);
                activeBooking.setStatus("Cancelled");
                bookingReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("Dashboard", "Booking Cancelled");
                        sendCancelledNotification();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Dashboard", "Booking Cancellation Failed");
                        helpers.showError(Dashboard.this, "something went wrong while cancelling the booking,plz try later");
                        sheetProgress.setVisibility(View.GONE);
                        mainSheet.setVisibility(View.VISIBLE);
                    }
                });
                break;
            }
        }
    }

    private void sendCancelledNotification() {
        DatabaseReference notificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
        Notification notification = new Notification();
        String id = notificationReference.push().getKey();
        notification.setId(id);
        notification.setBookingId(activeBooking.getId());
        notification.setUserId(activeBooking.getUserId());
        notification.setProviderId(activeBooking.getProviderId());
        notification.setRead(false);
        Date d = new Date();
        String date = new SimpleDateFormat("EEE dd, MMM, yyyy HH:mm").format(d);
        notification.setDate(date);
        notification.setUserMessage("You cancelled your booking with " + activeProvider.getFirstName() + " " + activeProvider.getLastName());
        notification.setProviderMessage("Your booking has been cancelled by " + user.getFirstName() + " " + user.getLastName());
        notificationReference.child(notification.getId()).setValue(notification);
    }

    private void postBooking() {
        searching.setVisibility(View.VISIBLE);
        bookingMain.setVisibility(View.GONE);
        DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Bookings");
        String key = bookingReference.push().getKey();
        activeBooking = new Booking();
        activeBooking.setId(key);
        activeBooking.setAmountCharged(0);
        activeBooking.setUserId(user.getPhone());
        Date d = new Date();
        String date = new SimpleDateFormat("EEE dd, MMM, yyyy HH:mm").format(d);
        activeBooking.setDate(date);
        activeBooking.setLatitude(marker.getPosition().latitude);
        activeBooking.setLongitude(marker.getPosition().longitude);
        activeBooking.setStatus("New");
        activeBooking.setType(selecttype.getSelectedItem().toString());
        activeBooking.setProviderId("");
        activeBooking.setAddres(locationAddress.getText().toString());
        bookingReference.child(activeBooking.getId()).setValue(activeBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                startTimer();
                listenToBookingChanges();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                searching.setVisibility(View.GONE);
                bookingMain.setVisibility(View.VISIBLE);
                helpers.showError(Dashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
        });
    }

    private void listenToBookingChanges() {
        bookingValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Dashboard", "Booking Value Listener");
                Booking booking = dataSnapshot.getValue(Booking.class);
                if (booking != null) {
                    activeBooking = booking;
                    switch (activeBooking.getStatus()) {
                        case "In Progress":
                            onBookingInProgress();
                            break;
                        case "Cancelled":
                            onBookingCancelled();
                            break;
                        case "Completed":
                            onBookingCompleted();
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingValueListener = bookingReference.child(activeBooking.getId()).addValueEventListener(bookingValueListener);
    }

    private void startTimer() {
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.e("Dashboard", "Time is ticking, booking status: " + activeBooking.getStatus());
            }

            @Override
            public void onFinish() {
                Log.e("Dashboard", "Time is finished, booking status: " + activeBooking.getStatus());
                if (activeBooking.getStatus().equals("New")) {
                    markBookingReject();
                } else if (activeBooking.getStatus().equals("In Progress")) {
                    onBookingInProgress();
                }
            }
        };
        timer.start();
    }

    private void markBookingReject() {
        activeBooking.setStatus("Rejected");
        DatabaseReference bookingReference = FirebaseDatabase.getInstance().getReference().child("Bookings");
        bookingReference.child(activeBooking.getId()).setValue(activeBooking);
        searching.setVisibility(View.GONE);
        bookingMain.setVisibility(View.VISIBLE);
        helpers.showError(Dashboard.this, "No provider available.\nPlease try again later.");
        activeBooking = null;
    }

    private void onBookingInProgress() {
        if (timer != null) {
            timer.cancel();
        }
        googleMap.clear();
        marker = googleMap.addMarker(new MarkerOptions().position(marker.getPosition()).title("You're Here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 11));
        if (providerValueListener != null) {
            userReference.removeEventListener(providerValueListener);
            Log.e("Dashboard", "Provider value event listener removed");
        }
        searching.setVisibility(View.GONE);
        bookingMain.setVisibility(View.VISIBLE);
        confirmCard.setVisibility(View.GONE);
        sheetBehavior.setHideable(false);
        sheetBehavior.setPeekHeight(220);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetProgress.setVisibility(View.VISIBLE);
        mainSheet.setVisibility(View.GONE);

        providerDetailValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
                userReference.removeEventListener(providerDetailValueListener);
                Log.e("Dashboard", "Provider value event listener called SnapShot: " + dataSnapshot.toString());
                activeProvider = dataSnapshot.getValue(User.class);
                if (activeProvider != null) {
                    if (activeProvider.getImage() != null && activeProvider.getImage().length() > 0) {
                        Glide.with(Dashboard.this).load(activeProvider.getImage()).into(providerImage);
                    }
                    providerName.setText(activeProvider.getFirstName() + " " + activeProvider.getLastName());
                    providerCategory.setText(activeProvider.getType());
                    bookingDate.setText(activeBooking.getDate());
                    bookingAddress.setText(activeBooking.getAddres());
                    if (activeProviderMarker != null) {
                        activeProviderMarker.remove();
                    }
                    LatLng latLng = new LatLng(activeProvider.getLatidue(), activeProvider.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(activeProvider.getType());
                    switch (activeProvider.getType()) {
                        case "Car Mechanic":
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.carmechanic));
                            break;
                        case "Bike Mechanic":
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bikemechanic));
                            break;
                        case "Petrol Provider":
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.petrolpump));
                            break;
                    }
                    activeProviderMarker = googleMap.addMarker(markerOptions);
                    activeProviderMarker.showInfoWindow();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                userReference.removeEventListener(providerDetailValueListener);
                Log.e("Dashboard", "Provider value event listener called");
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
            }
        };

        userReference.child(activeBooking.getProviderId()).addValueEventListener(providerDetailValueListener);
    }

    private void forBothCancelledAndCompleted() {
        if (providerDetailValueListener != null) {
            userReference.removeEventListener(providerValueListener);
        }
        if (bookingValueListener != null) {
            bookingReference.removeEventListener(bookingValueListener);
        }
        if (activeProviderMarker != null) {
            activeProviderMarker.remove();
        }
        sheetProgress.setVisibility(View.GONE);
        mainSheet.setVisibility(View.VISIBLE);
        sheetBehavior.setHideable(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        confirmCard.setVisibility(View.VISIBLE);
        listenToBookingsChanges();
        getAllProviders();
    }

    private void onBookingCancelled() {
        forBothCancelledAndCompleted();
    }


    private void onBookingCompleted() {
        forBothCancelledAndCompleted();

    }

}

