package lcwu.fyp.autocareapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.director.Constants;
import lcwu.fyp.autocareapp.director.Helpers;
import lcwu.fyp.autocareapp.director.Session;
import lcwu.fyp.autocareapp.model.User;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {
    private Helpers helpers;
    private EditText edtPhoneNo, edtFirstName, edtLastName, edtEmail;
    private String strPhoneNo, strFirstName, strLastName, strEmail, str_type, str_experience;
    private Button userProfile;
    private ProgressBar userProfileProgress;
    private ImageView image;
    private CheckBox is_Provider;
    private Spinner type, experience;
    private LinearLayout provide_option;
    private int role = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            finish();
            return;
        }

        strPhoneNo = bundle.getString("phone");


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("AUTO CARE APP");
        setSupportActionBar(toolbar);

        helpers = new Helpers();

        image = findViewById(R.id.image);
        image.setImageDrawable(getResources().getDrawable(R.drawable.logo));
        edtPhoneNo = findViewById(R.id.edtPhoneNo);
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmail = findViewById(R.id.edtEmail);
        userProfileProgress = findViewById(R.id.userProfileProgress);
        userProfile = findViewById(R.id.userProfile);
        is_Provider = findViewById(R.id.is_Provider);
        type = findViewById(R.id.type);
        experience = findViewById(R.id.experience);
        provide_option = findViewById(R.id.provider_option);
        userProfile.setOnClickListener(this);


        edtPhoneNo.setText(strPhoneNo);
        edtPhoneNo.setEnabled(false);

        is_Provider.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    provide_option.setVisibility(View.VISIBLE);
                    role = 1;
                } else {
                    provide_option.setVisibility(View.GONE);
                    role = 0;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.userProfile: {
                if (!helpers.isConnected(UserProfile.this)) {
                    helpers.showNoInternetError(UserProfile.this);
                    return;
                }

                boolean flag = isValid();
                if (flag) {
                    userProfileProgress.setVisibility(View.VISIBLE);
                    userProfile.setVisibility(View.GONE);
                    final User user = new User();
                    user.setPhone(strPhoneNo);
                    user.setFirstName(strFirstName);
                    user.setLastName(strLastName);
                    user.setEmail(strEmail);
                    user.setRoll(role);
                    user.setType(str_type);
                    user.setExperience(str_experience);
                    user.setLongitude(0);
                    user.setLatidue(0);
                    final Session session = new Session(UserProfile.this);
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    db.getReference().child("Users").child(strPhoneNo).setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    userProfileProgress.setVisibility(View.GONE);
                                    userProfile.setVisibility(View.VISIBLE);
                                    session.setSession(user);
                                    if (user.getRoll() == 0) {
                                        Intent it = new Intent(UserProfile.this, Dashboard.class);
                                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(it);
                                        finish();
                                    } else {
                                        Intent it = new Intent(UserProfile.this, ProviderDashboard.class);
                                        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(it);
                                        finish();

                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            userProfileProgress.setVisibility(View.GONE);
                            userProfile.setVisibility(View.VISIBLE);
                            helpers.showError(UserProfile.this, Constants.ERROR_SOMETHING_WENT_WRONG);
                        }
                    });


                }

                break;
            }
        }
    }

    private boolean isValid() {
        boolean flag = true;
        String Error = "";

        strFirstName = edtFirstName.getText().toString();
        strLastName = edtLastName.getText().toString();
        strEmail = edtEmail.getText().toString();
        if (strFirstName.length() < 3) {
            edtFirstName.setError(Constants.ERROR_FIRST_NAME);
            flag = false;
        } else {
            edtFirstName.setError(null);
        }
        if (strLastName.length() < 3) {
            edtLastName.setError(Constants.ERROR_LAST_NAME);
            flag = false;
        } else {
            edtLastName.setError(null);
        }
        if (strEmail.length() < 7 || !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()) {
            edtEmail.setError(Constants.ERROR_EMAIL);
            flag = false;
        } else {
            edtEmail.setError(null);
        }
        if (is_Provider.isChecked()) {
            if (type.getSelectedItemPosition() == 0) {
                flag = false;
                Error = Error + "Select Your Type First\n";
            } else {
                str_type = type.getSelectedItem().toString();
            }
            if (experience.getSelectedItemPosition() == 0) {
                flag = false;
                Error = Error + "Select Your Experience First\n";
            } else {
                str_experience = experience.getSelectedItem().toString();
            }
            if (!Error.equals("")) {
                helpers.showError(UserProfile.this, Error);
            }
        } else {
            str_type = "";
            str_experience = "";
        }
        return flag;
    }

    @Override
    public void onBackPressed() {
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
}
