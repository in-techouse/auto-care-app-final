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

import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shreyaspatil.MaterialDialog.MaterialDialog;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.director.Helpers;
import lcwu.fyp.autocareapp.director.Session;
import de.hdodenhof.circleimageview.CircleImageView;
import lcwu.fyp.autocareapp.director.Constants;
import lcwu.fyp.autocareapp.model.Booking;
import lcwu.fyp.autocareapp.model.User;

public class ProviderDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private DatabaseReference bookingsReference = FirebaseDatabase.getInstance().getReference().child("Bookings");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener bookingsValueListener, bookingValueListener, userValueListener;
    private MapView map;
    private Helpers helpers;
    private GoogleMap googleMap;
    private DrawerLayout drawer;
    private User user, activeCustomer;
    private FusedLocationProviderClient locationProviderClient;
    private Marker marker, customerMarker;
    private TextView locationAddress;
    private LinearLayout amountLayout;
    private BottomSheetBehavior sheetbehavoior;
    private ProgressBar sheetprogress;
    private RelativeLayout mainsheet;
    private Booking activeBooking;
    private TextView profileName, profileEmail, profilePhone, providerName, bookingAddress, bookingDate;
    private CircleImageView providerImage;
    private EditText totalCharge;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetbehavoior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetbehavoior.setHideable(true);
        sheetbehavoior.setPeekHeight(0);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetprogress = findViewById(R.id.sheetProgress);
        mainsheet = findViewById(R.id.mainSheet);
        amountLayout = findViewById(R.id.amountLayout);
        providerImage = findViewById(R.id.providerImage);
        providerName = findViewById(R.id.providerName);
        bookingAddress = findViewById(R.id.bookingAddress);
        bookingDate = findViewById(R.id.bookingDate);
        Button cancelBooking = findViewById(R.id.cancelBooking);
        Button completeBooking = findViewById(R.id.mark_complete);

        totalCharge = findViewById(R.id.totalCharge);
        Button amountSubmit = findViewById(R.id.amountSubmit);


        cancelBooking.setOnClickListener(this);
        completeBooking.setOnClickListener(this);
        amountSubmit.setOnClickListener(this);


        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        session = new Session(ProviderDashboard.this);
        user = session.getUser();
        helpers = new Helpers();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(ProviderDashboard.this);


        View header = navigationView.getHeaderView(0);
        TextView profile_email = header.findViewById(R.id.profile_email);
        TextView profile_name = header.findViewById(R.id.profile_name);
        CircleImageView profile_image = header.findViewById(R.id.profile_image);
        TextView profile_phone = header.findViewById(R.id.profile_phone);
        TextView profile_type = header.findViewById(R.id.profile_type);
        TextView profile_experience = header.findViewById(R.id.profile_experience);
        String name = user.getFirstName() + " " + user.getLastName();
        profile_name.setText(name);
        profile_email.setText(user.getEmail());
        profile_type.setText(user.getType());
        profile_phone.setText(user.getPhone());
        profile_experience.setText("Experience: " + user.getExperience());
        locationAddress = findViewById(R.id.locationAddress);
        map = findViewById(R.id.map);
        map.onCreate(savedInstanceState);
        try {
            MapsInitializer.initialize(ProviderDashboard.this);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gM) {
                    Log.e("ProviderDashboard", "Call back received");

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
            helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
        }

    }

    private boolean askForPermission() {
        if (ActivityCompat.checkSelfPermission(ProviderDashboard.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ProviderDashboard.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProviderDashboard.this, new String[]{
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
                    FusedLocationProviderClient current = LocationServices.getFusedLocationProviderClient(ProviderDashboard.this);
                    current.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            getDeviceLocation();
                        }
                    });
                    return true;
                }
            });
            getDeviceLocation();
            listenToBookings();
        }
    }

    private void getDeviceLocation() {
        Log.e("ProviderDashboard", "Call received to get device location");
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
                helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);

            }
            if (!gps_enabled && !network_enabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ProviderDashboard.this);
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
                            Geocoder geocoder = new Geocoder(ProviderDashboard.this);
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
                                helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                            }
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                }
            });
        } catch (Exception e) {
            helpers.showError(ProviderDashboard.this, Constants.ERROR_SOMETHING_WENT_WRONG);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        Log.e("ProviderDashboard", "" + id);
        switch (id) {
            case R.id.nav_home: {
                break;
            }
            case R.id.nav_booking: {
                Intent it = new Intent(ProviderDashboard.this, BookingActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_notification: {
                Intent it = new Intent(ProviderDashboard.this, NotificationActivity.class);
                startActivity(it);
                break;
            }
            case R.id.nav_userProfile: {
                Intent it = new Intent(ProviderDashboard.this, EditUserProfile.class);
                startActivity(it);
                break;
            }
            case R.id.nav_logout: {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                Session session = new Session(ProviderDashboard.this);
                auth.signOut();
                session.destroySession();
                Intent it = new Intent(ProviderDashboard.this, LoginActivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
                finish();
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
        if (bookingsValueListener != null) {
            bookingsReference.removeEventListener(bookingsValueListener);
        }
        if (bookingValueListener != null) {
            bookingsReference.removeEventListener(bookingValueListener);
        }

        if (userValueListener != null) {
            userReference.removeEventListener(userValueListener);
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

    private void updateUserLocation(double lat, double lng) {
        Log.e("ProviderDashboard", "Lat: " + lat + " Lng: " + lng);
        user.setLatidue(lat);
        user.setLongitude(lng);
        session.setSession(user);
        userReference.child(user.getPhone()).setValue(user);
    }

    private void listenToBookings() {
        bookingsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("ProviderDashboard", "Bookings value event Listener");
                if (activeBooking == null) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        Booking booking = d.getValue(Booking.class);
                        if (booking != null) {
                            Log.e("ProviderDashboard", "Bookings value event Listener, booking found with status: " + booking.getStatus());
                            if (booking.getStatus().equals("New")) {
                                showBookingDialog(booking);
                            } else if (booking.getStatus().equals("In Progress")) {
                                activeBooking = booking;
                                onBookingInProgress();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingsReference.orderByChild("type").equalTo(user.getType()).addValueEventListener(bookingsValueListener);
    }

    private void showBookingDialog(final Booking booking) {
        helpers.showNotification(ProviderDashboard.this, "New Booking", "We have a new booking for you. It's time to get some revenue.");

        final MaterialDialog dialog = new MaterialDialog.Builder(ProviderDashboard.this)
                .setTitle("NEW BOOKING")
                .setMessage("A NEW BOOKING HAS ARRIVED, DO YOU WANT TO EARN SOME MORE PROFIT?")
                .setCancelable(false)
                .setPositiveButton("DETAILS", R.drawable.ic_okay, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        Intent it = new Intent(ProviderDashboard.this, ShowBookingDetail.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("Booking", booking);
                        it.putExtras(bundle);
                        startActivity(it);
                    }
                })
                .setNegativeButton("REJECT", R.drawable.ic_close, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(com.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .build();
        dialog.show();
    }

    private void onBookingInProgress() {
        bookingsReference.removeEventListener(bookingsValueListener);
        sheetbehavoior.setHideable(false);
        sheetbehavoior.setPeekHeight(220);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetprogress.setVisibility(View.VISIBLE);
        mainsheet.setVisibility(View.GONE);
        amountLayout.setVisibility(View.GONE);
        listenToBookingChanges();

        userValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userReference.removeEventListener(userValueListener);
                Log.e("ProviderDashboard", "User value event listener called SnapShot: " + dataSnapshot.toString());
                sheetprogress.setVisibility(View.GONE);
                mainsheet.setVisibility(View.VISIBLE);
                activeCustomer = dataSnapshot.getValue(User.class);
                if (activeCustomer != null && activeBooking != null) {
                    if (activeCustomer.getImage() != null && activeCustomer.getImage().length() > 0) {
                        Glide.with(ProviderDashboard.this).load(activeCustomer.getImage()).into(providerImage);
                    }
                    providerName.setText(activeCustomer.getFirstName() + " " + activeCustomer.getLastName());

                    bookingAddress.setText(activeBooking.getAddres());
                    bookingDate.setText(activeBooking.getDate());

                    if (customerMarker != null) {
                        customerMarker.remove();
                    }
                    LatLng latLng = new LatLng(activeCustomer.getLatidue(), activeCustomer.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(activeCustomer.getFirstName());
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    customerMarker = googleMap.addMarker(markerOptions);
                    customerMarker.showInfoWindow();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                userReference.removeEventListener(userValueListener);
                Log.e("ProviderDashboard", "User value event listener called");
                sheetprogress.setVisibility(View.GONE);
                mainsheet.setVisibility(View.VISIBLE);
            }
        };

        userReference.child(activeBooking.getUserId()).addValueEventListener(userValueListener);

    }

    private void forBothCancelledAndCompleted() {
        sheetprogress.setVisibility(View.VISIBLE);
        mainsheet.setVisibility(View.GONE);
        if (userValueListener != null) {
            userReference.removeEventListener(userValueListener);
        }
        if (bookingValueListener != null) {
            bookingsReference.addValueEventListener(bookingValueListener);
        }
        if (customerMarker != null) {
            customerMarker.remove();
        }
        sheetprogress.setVisibility(View.GONE);
        mainsheet.setVisibility(View.VISIBLE);
        sheetbehavoior.setHideable(true);
        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
        activeBooking = null;
        listenToBookings();
    }

    private void onBookingCancelled() {
        forBothCancelledAndCompleted();
    }

    private void onBookingCompleted() {
        forBothCancelledAndCompleted();

    }

    private void listenToBookingChanges() {
        bookingValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("ProviderDashboard", "Booking Value Listener");
                Booking booking = dataSnapshot.getValue(Booking.class);
                if (activeBooking != null && booking != null) {
                    activeBooking = booking;
                    if (activeBooking != null && activeBooking.getStatus() != null) {
                        switch (activeBooking.getStatus()) {
                            case "Cancelled":
                                onBookingCancelled();
                                break;
                            case "Completed":
                                onBookingCompleted();
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        bookingValueListener = bookingsReference.child(activeBooking.getId()).addValueEventListener(bookingValueListener);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.cancelBooking: {
                Log.e("ProviderDashboard", "button clicked");
                mainsheet.setVisibility(View.GONE);
//                sheetbehavoior.setPeekHeight(120);
                sheetprogress.setVisibility(View.VISIBLE);
                activeBooking.setStatus("Cancelled");
                bookingsReference.child(activeBooking.getId()).child("status").setValue(activeBooking.getStatus()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("ProviderDashboard", "Cancelled");
//                        sheetbehavoior.setHideable(true);
//                        sheetprogress.setVisibility(View.GONE);
//                        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProviderDashboard", "Cancellation Failed");
                        helpers.showError(ProviderDashboard.this, "something went wrong while cancelling the booking,plz try later");
                        sheetprogress.setVisibility(View.GONE);
                        mainsheet.setVisibility(View.VISIBLE);
                    }
                });
                break;
            }
            case R.id.mark_complete: {
                Log.e("ProviderDashboard", "button clicked");
                mainsheet.setVisibility(View.GONE);
                amountLayout.setVisibility(View.VISIBLE);
                break;
            }

            case R.id.amountSubmit: {
                String strTotalCharge = totalCharge.getText().toString();
                if (strTotalCharge.length() < 1) {
                    totalCharge.setError("Enter some valid amount.");
                    return;
                }
                int amount = 0;
                try {
                    amount = Integer.parseInt(strTotalCharge);
                } catch (Exception e) {
                    totalCharge.setError("Enter some valid amount.");
                    return;
                }

                mainsheet.setVisibility(View.GONE);
                amountLayout.setVisibility(View.GONE);
//                sheetbehavoior.setPeekHeight(120);
                sheetprogress.setVisibility(View.VISIBLE);
                activeBooking.setStatus("Completed");
                activeBooking.setAmountCharged(amount);
                totalCharge.setText("");
                bookingsReference.child(activeBooking.getId()).setValue(activeBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("ProviderDashboard", "Completed");
//                        sheetbehavoior.setHideable(true);
//                        sheetprogress.setVisibility(View.GONE);
//                        sheetbehavoior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProviderDashboard", "Complete");
                        sheetprogress.setVisibility(View.GONE);
                        mainsheet.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }
}
