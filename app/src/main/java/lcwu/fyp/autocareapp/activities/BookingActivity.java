package lcwu.fyp.autocareapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.adapters.BookingAdapter;
import lcwu.fyp.autocareapp.director.Session;
import lcwu.fyp.autocareapp.model.Booking;
import lcwu.fyp.autocareapp.model.Notification;
import lcwu.fyp.autocareapp.model.User;

public class BookingActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout loading;
    private TextView noBooking;
    private RecyclerView bookings;
    private Session session;
    private User user;
    private List<Booking> Data;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Bookings");
    private DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private ValueEventListener bookingListener, userListener;
    private BookingAdapter bookingAdapter;
    private String orderBy;
    private BottomSheetBehavior sheetBehavior;
    private Button closeSheet;
    private ProgressBar sheetProgress;
    private LinearLayout mainSheet;
    private CircleImageView image;
    private TextView about, name, date, type, status, totalCharge, address;
    private RelativeLayout userLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        loading = findViewById(R.id.loading);
        noBooking = findViewById(R.id.noBooking);
        bookings = findViewById(R.id.bookings);
        session = new Session(BookingActivity.this);
        user = session.getUser();
        bookingAdapter = new BookingAdapter(BookingActivity.this, BookingActivity.this);
        bookings.setLayoutManager(new LinearLayoutManager(BookingActivity.this));
        bookings.setAdapter(bookingAdapter);
        Data = new ArrayList<>();

        LinearLayout layoutBottomSheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        closeSheet = findViewById(R.id.closeSheet);
        closeSheet.setOnClickListener(this);
        sheetProgress = findViewById(R.id.sheetProgress);
        mainSheet = findViewById(R.id.mainSheet);

        image = findViewById(R.id.image);
        about = findViewById(R.id.about);
        name = findViewById(R.id.name);
        date = findViewById(R.id.date);
        type = findViewById(R.id.type);
        status = findViewById(R.id.status);
        totalCharge = findViewById(R.id.totalCharge);
        address = findViewById(R.id.address);
        userLayout = findViewById(R.id.userLayout);

        if (user.getRoll() == 0) {
            orderBy = "userId";
        } else {
            orderBy = "providerId";
        }
        loadBookings();
    }

    private void loadBookings() {
        loading.setVisibility(View.VISIBLE);
        noBooking.setVisibility(View.GONE);
        bookings.setVisibility(View.GONE);

        bookingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Bookings", "Data Snap Shot: " + dataSnapshot.toString());
                Data.clear();
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    Booking b = d.getValue(Booking.class);
                    if (b != null) {
                        Data.add(b);
                    }
                }
                Collections.reverse(Data);
                Log.e("Bookings", "Data List Size: " + Data.size());
                if (Data.size() > 0) {
                    Log.e("Bookings", "If, list visible");
                    bookings.setVisibility(View.VISIBLE);
                    noBooking.setVisibility(View.GONE);
                } else {
                    Log.e("Bookings", "Else, list invisible");
                    noBooking.setVisibility(View.VISIBLE);
                    bookings.setVisibility(View.GONE);
                }
                loading.setVisibility(View.GONE);
                bookingAdapter.setData(Data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loading.setVisibility(View.GONE);
                noBooking.setVisibility(View.VISIBLE);
                bookings.setVisibility(View.GONE);
            }
        };
        reference.orderByChild(orderBy).equalTo(user.getPhone()).addValueEventListener(bookingListener);
    }

    public void showBottomSheet(Booking booking) {
        sheetBehavior.setHideable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        sheetProgress.setVisibility(View.VISIBLE);
        mainSheet.setVisibility(View.GONE);

        if(user.getRoll() == 0){
            about.setText("Provider detail");
        }
        else{
            about.setText("Customer detail");
        }
        date.setText(booking.getDate());
        address.setText(booking.getAddres());
        totalCharge.setText(booking.getAmountCharged()+" RS.");
        type.setText(booking.getType());
        status.setText(booking.getStatus());

        if(booking.getProviderId().equals("")){
            about.setVisibility(View.GONE);
            userLayout.setVisibility(View.GONE);
            sheetProgress.setVisibility(View.GONE);
            mainSheet.setVisibility(View.VISIBLE);
            return;
        }

        about.setVisibility(View.VISIBLE);
        userLayout.setVisibility(View.VISIBLE);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (user.getRoll() == 0) {
                    userReference.child(booking.getProviderId()).removeEventListener(userListener);
                } else {
                    userReference.child(booking.getUserId()).removeEventListener(userListener);
                }
                userReference.removeEventListener(userListener);
                User tempUser = dataSnapshot.getValue(User.class);
                if (tempUser != null) {
                    if (tempUser.getImage() != null && tempUser.getImage().length() > 0) {
                        Glide.with(BookingActivity.this).load(tempUser.getImage()).into(image);
                    } else {
                        image.setImageDrawable(getResources().getDrawable(R.drawable.user));
                    }
                    String strName = tempUser.getFirstName() + " " + tempUser.getLastName();
                    name.setText(strName);
                    sheetProgress.setVisibility(View.GONE);
                    mainSheet.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (user.getRoll() == 0) {
                    userReference.child(booking.getProviderId()).removeEventListener(userListener);
                } else {
                    userReference.child(booking.getUserId()).removeEventListener(userListener);
                }
                userReference.removeEventListener(userListener);
                sheetProgress.setVisibility(View.GONE);
                mainSheet.setVisibility(View.VISIBLE);
            }
        };
        if (user.getRoll() == 0) {
            userReference.child(booking.getProviderId()).addValueEventListener(userListener);
        } else {
            userReference.child(booking.getUserId()).addValueEventListener(userListener);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.closeSheet: {
                sheetBehavior.setHideable(true);
                sheetBehavior.setPeekHeight(0);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED){
            sheetBehavior.setHideable(true);
            sheetBehavior.setPeekHeight(0);
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        else
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bookingListener != null){
            reference.removeEventListener(bookingListener);
        }
        if(userListener != null){
            userReference.removeEventListener(userListener);
        }
    }

}
