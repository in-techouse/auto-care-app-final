package lcwu.fyp.autocareapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import lcwu.fyp.autocareapp.R;
import lcwu.fyp.autocareapp.director.Session;
import lcwu.fyp.autocareapp.model.User;

public class SplashActivity extends Activity {
    private boolean isFirstAnimation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        /*Simple hold animation to hold ImageView in the centre of the screen at a slightly larger
        scale than the ImageView's original one.*/
        Animation hold = AnimationUtils.loadAnimation(this, R.anim.hold);

        /* Translates ImageView from current position to its original position, scales it from
        larger scale to its original scale.*/
        final Animation translateScale = AnimationUtils.loadAnimation(this, R.anim.translate_scale);

        final ImageView imageView = findViewById(R.id.header_icon);

        translateScale.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!isFirstAnimation) {
                    imageView.clearAnimation();
                    Session session = new Session(SplashActivity.this);
                    User user = session.getUser();
                    if (user == null) {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                    } else {
                        if (user.getRoll() == 0) {
                            Intent intent = new Intent(SplashActivity.this, Dashboard.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SplashActivity.this, ProviderDashboard.class);
                            startActivity(intent);
                        }
                        finish();
                    }
                    isFirstAnimation = true;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        hold.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.clearAnimation();
                imageView.startAnimation(translateScale);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        imageView.startAnimation(hold);


    }
}
