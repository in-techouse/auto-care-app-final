package lcwu.fyp.autocareapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;

import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.director.Constants;
import lcwu.fyp.autocareapp.director.Helpers;
import lcwu.fyp.autocareapp.director.Session;
import lcwu.fyp.autocareapp.model.User;

public class BecameProvider extends AppCompatActivity implements View.OnClickListener {
    private Helpers helpers;
    private TextView edit_email;
    private Spinner type, experience;
    private Button upgrade_account;
    private ProgressBar progress_bar;
    private String str_email, str_type, str_experience;
    private User user;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_became_provider);


        helpers = new Helpers();
        session = new Session(BecameProvider.this);
        user = session.getUser();

        edit_email = findViewById(R.id.edit_email);
        type = findViewById(R.id.type);
        experience = findViewById(R.id.experience);
        upgrade_account = findViewById(R.id.upgrade_account);
        progress_bar = findViewById(R.id.progress_bar);


        upgrade_account.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.upgrade_account: {
                final boolean flag = helpers.isConnected(this);
                if (!flag) {
                    helpers.showNoInternetError(BecameProvider.this);
                    return;
                }

                boolean flag1 = isValid();
                if (flag1) {
                    upgrade_account.setVisibility(View.GONE);
                    progress_bar.setVisibility(View.VISIBLE);
                    user.setRoll(1);
                    user.setEmail(str_email);
                    user.setType(str_type);
                    user.setExperience(str_experience);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    database.getReference().child("Users").child(user.getPhone()).setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    upgrade_account.setVisibility(View.GONE);
                                    progress_bar.setVisibility(View.VISIBLE);
                                    session.setSession(user);
                                    Intent it = new Intent(BecameProvider.this, ProviderDashboard.class);
                                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(it);
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            upgrade_account.setVisibility(View.GONE);
                            progress_bar.setVisibility(View.VISIBLE);
                            helpers.showError(BecameProvider.this, Constants.ERROR_SOMETHING_WENT_WRONG);
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
        str_email = edit_email.getText().toString();
        if (str_email.length() < 7 || !Patterns.EMAIL_ADDRESS.matcher(str_email).matches()) {
            edit_email.setError(Constants.ERROR_EMAIL);
            flag = false;
        } else {
            edit_email.setError(null);
        }
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
            helpers.showError(BecameProvider.this, Error);
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
