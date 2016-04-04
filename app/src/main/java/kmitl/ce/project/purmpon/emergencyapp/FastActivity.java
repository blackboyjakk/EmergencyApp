package kmitl.ce.project.purmpon.emergencyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FastActivity extends AppCompatActivity {
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fast);
        textView = (TextView)findViewById(R.id.text);
        registerReceiver(uiUpdated, new IntentFilter("LOCATION_UPDATED"));

        Button button = (Button)findViewById(R.id.start);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getApplicationContext(), SoundDetectService.class);
                getApplicationContext().startService(myIntent);
            }
        });
    }
    private BroadcastReceiver uiUpdated= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            textView.setText(intent.getExtras().getString("results"));

        }
    };
}
