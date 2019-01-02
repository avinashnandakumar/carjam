package sideprojects.carjam;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Request;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final String CLIENT_ID = "0407da6387be4e3aba45344f618f48cf";
    private SpotifyAppRemote mSpotifyAppRemote;
    private BroadcastReceiver reciever;
    private IntentFilter mConnectionChangeIntentFilter = new IntentFilter();
    //private SmsReceiver smsReceiver = new SmsReceiver();

    private int MY_PERMISSIONS_REQUEST_SMS_RECEIVE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //updateMessage(" ");
        //registerReceiver(smsReceiver, mConnectionChangeIntentFilter);
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        System.out.println("Roast");
        //connected();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "Remote Connected", Toast.LENGTH_SHORT).show();
        // We will start writing our code here.
    }

    @Override
    protected void onResume(){
        super.onResume();
        Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show();
        //connected();
    }

    private void connected() {
        // Play a playlist
        //SpotifyAppRemote.connect(this, mConnectionParams, mConnectionListener);
        //mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        final TextView songName = (TextView) findViewById(R.id.currentSongName);
        final TextView songArtist = (TextView) findViewById(R.id.currentSongArtist);
        final ImageView songImage = (ImageView) findViewById(R.id.currentSongImage);

        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        songName.setText(track.name);
                        songArtist.setText(track.artist.name);
                        mSpotifyAppRemote.getImagesApi().getImage(track.imageUri).setResultCallback(image -> {songImage.setImageBitmap(image);});
                        //Toast.makeText(this, track.name, Toast.LENGTH_SHORT).show();
                    }
                });

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://google.com";

        final TextView songQueue = (TextView) findViewById(R.id.requestResponse);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        songQueue.setText("Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                songQueue.setText("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
        // Then we will write some more code here
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                MY_PERMISSIONS_REQUEST_SMS_RECEIVE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SMS_RECEIVE) {
            // YES!!
            System.out.println("DUCKYES");
            Log.i("TAG", "MY_PERMISSIONS_REQUEST_SMS_RECEIVE --> YES");

        }
    }


    public void broadcastIntent(View view){
        Intent intent = getIntent();
        String message = intent.getStringExtra("message");
        updateMessage(message);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        // Aaand we will finish off here.
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    Toast.makeText(this, "Logged In", Toast.LENGTH_SHORT).show();
                    ConnectionParams connectionParams =
                            new ConnectionParams.Builder(CLIENT_ID)
                                    .setRedirectUri(REDIRECT_URI)
                                    .showAuthView(true)
                                    .build();
                    SpotifyAppRemote.connect(this, connectionParams,
                            new Connector.ConnectionListener() {

                                @Override
                                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                    mSpotifyAppRemote = spotifyAppRemote;
                                    Log.d("MainActivity", "Connected! Yay!");

                                    // Now you can start interacting with App Remote
                                    connected();
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    Log.e("MainActivity", throwable.getMessage(), throwable);

                                    // Something went wrong when attempting to connect! Handle errors here
                                }
                            });
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }


    public void updateMessage(String messageBody) {
        TextView smsMessage = (TextView) findViewById(R.id.smsMessage);
        smsMessage.setText(messageBody);
        System.out.println("It worked");
    }


}
