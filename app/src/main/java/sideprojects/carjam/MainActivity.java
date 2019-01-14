package sideprojects.carjam;

import android.Manifest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.*;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsMessage;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makeramen.roundedimageview.RoundedImageView;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.w3c.dom.Text;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;


import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity {

    private static Boolean mainPower = true;
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final String CLIENT_ID = "0407da6387be4e3aba45344f618f48cf";
    private static String spotifyToken = "";

    private SpotifyAppRemote mSpotifyAppRemote;
    boolean mIsReceiverRegistered = false;
    private SmsReciever mReciever;
    private Integer queueLimitPeriod = 7;
    private Integer queueRefreshPeriod = 20; //minutes
    private Map<String, Integer> queueLimit;
    private Map<String, Long> requesterTime;
    RecyclerView recyclerView;
    QueueListAdapter QLA;
    private SpotifyService spotify;

    String[] inter = new String[3];

    private int MY_PERMISSIONS_REQUEST_SMS_RECEIVE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queueLimitPeriod=7;
        queueRefreshPeriod=20;
        queueLimit = new HashMap<String,Integer>();
        requesterTime = new HashMap<>();
        //requestAuthorization();
        //requestWithSomeHttpHeaders("28zGTndSQj4JT9nCPHRoTV");
        QLA = new QueueListAdapter();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(QLA);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        ImageButton settingsButton = (ImageButton) findViewById(R.id.settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup();
            }
        });
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // We will start writing our code here.
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show();
        if (mReciever == null)
            mReciever = new SmsReciever();
        registerReceiver(mReciever, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        mIsReceiverRegistered = true;
        //connected();
    }

    /*class Person {
        String name;
        String age;
        int photoId;

        Person(String name, String age, int photoId) {
            this.name = name;
            this.age = age;
            this.photoId = photoId;
        }
    }

    private List<Person> persons;

    // This method creates an ArrayList that has three Person objects
// Checkout the project associated with this tutorial on Github if
// you want to use the same images.
    private void initializeData(){
        persons = new ArrayList<>();
        persons.add(new Person("Emma Wilson", "23 years old", 0));
        persons.add(new Person("Lavery Maiss", "25 years old", 0));
        persons.add(new Person("Lillie Watts", "35 years old", 0));
    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.PersonViewHolder>{

        public class PersonViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView personName;
            TextView personAge;
            ImageView personPhoto;

            PersonViewHolder(View itemView) {
                super(itemView);
                cv = (CardView)itemView.findViewById(R.id.cv);
                personName = (TextView)itemView.findViewById(R.id.person_name);
                personAge = (TextView)itemView.findViewById(R.id.person_age);
                personPhoto = (ImageView)itemView.findViewById(R.id.person_photo);
            }

        }
        List<Person> persons;
        RVAdapter(List<Person> persons){
            this.persons = persons;
        }

        @Override
        public int getItemCount() {
            return persons.size();
        }

        @Override
        public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_main, viewGroup, false);
            PersonViewHolder pvh = new PersonViewHolder(v);
            return pvh;
        }
        @Override
        public void onBindViewHolder(PersonViewHolder personViewHolder, int i) {
            personViewHolder.personName.setText(persons.get(i).name);
            personViewHolder.personAge.setText(persons.get(i).age);
            personViewHolder.personPhoto.setImageResource(persons.get(i).photoId);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }*/


    /*}*/
    private void connected() {
        // Play a playlist
        //SpotifyAppRemote.connect(this, mConnectionParams, mConnectionListener);
        //mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        final TextView songName = (TextView) findViewById(R.id.currentSongName);
        final TextView songArtist = (TextView) findViewById(R.id.currentSongArtist);
        final ImageView songImage = (ImageView) findViewById(R.id.currentSongImage);
        //final RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        /*initializeData();
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        RVAdapter adapter = new RVAdapter(persons);
        rv.setAdapter(adapter);*/
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        if (QLA.fakeData.size()>0){
                            if((track.name).equals(QLA.fakeData.get(0).nameOfSong)) {
                                QLA.fakeData.remove(0);
                                QLA.notifyDataSetChanged();
                            }
                        }
                        songName.setText(track.name);
                        songArtist.setText(track.artist.name);
                        System.out.println(track.imageUri.toString() + "SPOTIFY IMAGE URI");
                        mSpotifyAppRemote.getImagesApi().getImage(track.imageUri).setResultCallback(image -> {songImage.setImageBitmap(image);});
                        if(playerState.isPaused){
                            ((ImageButton)findViewById(R.id.play)).setImageResource(android.R.drawable.ic_media_play);
                        }
                        else{

                            ((ImageButton)findViewById(R.id.play)).setImageResource(android.R.drawable.ic_media_pause);

                        }
                        mSpotifyAppRemote.getUserApi().getLibraryState(track.uri).setResultCallback(libraryState -> {
                            if(libraryState.isAdded){
                                ((ImageButton)findViewById(R.id.save)).setImageResource(android.R.drawable.btn_star_big_on);
                                //findViewById(R.id.save).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }
                            else{
                                ((ImageButton)findViewById(R.id.save)).setImageResource(android.R.drawable.btn_star);
                                //findViewById(R.id.save).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }
                                });
                        //Toast.makeText(this, track.name, Toast.LENGTH_SHORT).show();
                    }
                });
        /*SpotifyApi api = new SpotifyApi();
        System.out.println(spotifyToken);
        api.setAccessToken(spotifyToken);
        SpotifyService roasterSpotify = api.getService();*/
        //

        final String accessToken = spotifyToken;
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + accessToken);
                    }
                })
                .build();

        spotify = restAdapter.create(SpotifyService.class);
        //String[] test=getTrackInfo("27a1mYSG5tYg7dmEjWBcmL");
        //QLA.addData(test[0]);
        //System.out.println(test[0] + "ROASTER TOASTER");
        //QLA.notifyDataSetChanged();
        //QLA.addData("Hello");
        //QLA.notifyDataSetChanged();
        /*spotify.getAlbum("2dIGnmEIy1WZIcZCFSj6i8", new Callback<Album>() {

            @Override
            public void success(Album album, retrofit.client.Response response) {
                Log.d("Album success", album.name);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Album failure", error.toString());
            }
        });*/


        // Instantiate the RequestQueue.
        //RequestQueue queue = Volley.newRequestQueue(this);


        /*JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        songQueue.setText("Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });*/

// Access the RequestQueue through your singleton class.
        //queue.add(jsonObjectRequest);
//        final TextView songQueue = (TextView) findViewById(R.id.smsMessage);
//        // Request a string response from the provided URL.
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        // Display the first 500 characters of the response string.
//                        songQueue.setText("Response is: "+ response.substring(0,500));
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                songQueue.setText("That didn't work!");
//            }
//        });
//
//        // Add the request to the RequestQueue.
//        //queue.add(stringRequest);

        // Then we will write some more code here
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS},
                MY_PERMISSIONS_REQUEST_SMS_RECEIVE);
        /*ActivityCompat.requestPermissions(this,
                new String[]{},
                24);
        ActivityCompat.requestPermissions(this, new String[]{}, 100);*/


    }

    /*private void internetRequest() throws Exception {
        //String url ="https://api.spotify.com/v1/tracks/3ee8Jmje8o58CHK66QrVC2";
        String url = "http://api.ipinfodb.com/v3/ip-city/?key=d64fcfdfacc213c7ddf4ef911dfe97b55e4696be3532bf8302876c09ebd06b&ip=74.125.45.100&format=json";
        final TextView songQueue = (TextView) findViewById(R.id.smsMessage);

        //String url = "http://api.ipinfodb.com/v3/ip-city/?key=d64fcfdfacc213c7ddf4ef911dfe97b55e4696be3532bf8302876c09ebd06b&ip=74.125.45.100&format=json";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //print in String
        System.out.println(response.toString());
        //Read JSON response and print
        JSONObject myResponse = new JSONObject(response.toString());
        System.out.println("result after Reading JSON Response");
        System.out.println("statusCode- "+myResponse.getString("statusCode"));
        System.out.println("statusMessage- "+myResponse.getString("statusMessage"));
        System.out.println("ipAddress- "+myResponse.getString("ipAddress"));
        System.out.println("countryCode- "+myResponse.getString("countryCode"));
        System.out.println("countryName- "+myResponse.getString("countryName"));
        System.out.println("regionName- "+myResponse.getString("regionName"));
        System.out.println("cityName- "+myResponse.getString("cityName"));
        System.out.println("zipCode- "+myResponse.getString("zipCode"));
        System.out.println("latitude- "+myResponse.getString("latitude"));
        System.out.println("longitude- "+myResponse.getString("longitude"));
        System.out.println("timeZone- "+myResponse.getString("timeZone"));
    }*/


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SMS_RECEIVE) {
            // YES!!
            System.out.println("DUCKYES");
           // Log.i("TAG", "MY_PERMISSIONS_REQUEST_SMS_RECEIVE --> YES");

        }
        else if (requestCode == 100) {
            // YES!!
            System.out.println("roaster");
          //  Log.i("TAG", "CONTACTS --> YES");
            //updateQueueUI("+18056035340", null);

        }
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
                    spotifyToken = response.getAccessToken();
                    Toast.makeText(this, "Remote Connected", Toast.LENGTH_SHORT).show();
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
                                  //  Log.d("MainActivity", "Connected! Yay!");

                                    // Now you can start interacting with App Remote
                                    connected();
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                   // Log.e("MainActivity", throwable.getMessage(), throwable);

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

    @Override
    protected void onPause(){
        /*if (mIsReceiverRegistered) {
            unregisterReceiver(mReciever);
            mReciever = null;
            mIsReceiverRegistered = false;
        }*/
        super.onPause();

    }

    /*public void updateMessage(String messageBody) {
        TextView smsMessage = (TextView) findViewById(R.id.smsMessage);
        smsMessage.setText(messageBody);
        smsMessage.setMovementMethod(new ScrollingMovementMethod());
        System.out.println("It worked");
    }*/

    private class SmsReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Bundle data = intent.getExtras();
                Object[] pdus = (Object[]) data.get("pdus");
                String fullMessage = "";
                String senderNumber = "";
                for (int i = 0; i < pdus.length; i++) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String sender = smsMessage.getDisplayOriginatingAddress();
                    senderNumber = sender;
                    //Check the sender to filter messages which we require to read
                    String messageBody = smsMessage.getMessageBody();
                    fullMessage += messageBody;
                    //System.out.println(messageBody);
                    //System.out.println(messageBody.substring(6,15));
                    //messageBody = " a song f";
                    //System.out.println(messageBody);
                    //System.out.println(" ");
                }
                //System.out.println(fullMessage);
                Pattern p2 = Pattern.compile("a song for you");
                Matcher matcher2 = p2.matcher(fullMessage);
                Pattern p3 = Pattern.compile("https://open.spotify.com");
                Matcher matcher3 = p3.matcher(fullMessage);
                if (matcher2.find() || matcher3.find()) {
                    //String temp = messageBody.substring(6, 15);
                    //if (temp.equals(" a song f")) {
                    System.out.println("In the loop");
                    //updateMessage(fullMessage);
                    Pattern p = Pattern.compile("https.*\\?");
                    Matcher matcher = p.matcher(fullMessage);
                    List<String> toProcess = new LinkedList<>();
                    while (matcher.find()) {
                        System.out.println(matcher.group());
                        String songToQueue = matcher.group();
                        String songId = songToQueue.substring(31, songToQueue.length() - 1);
                        System.out.println(songId);
                        toProcess.add(songId);
                        //addToQueue(senderNumber, songId);
                        //deleteText(context, senderNumber, fullMessage);
                    }
                    for (int i = 0; i < toProcess.size(); i++) {
                        if(mainPower){
                            addToQueue(senderNumber, toProcess.get(i));
                            Thread.sleep(250);
                        }

                    }


                } else {
                    System.out.println("Didnot go in the loop");
                }
                //QLA.notifyDataSetChanged();
            } catch (Exception e) {
                System.out.println("delay error");
            }
        }
    }

    private void deleteText(Context context, String sender, String message){
        Uri inbox = Uri.parse("content://sms/inbox");
        String[] columns = new String[] { "_id","address", "body" };
        Cursor c = context.getContentResolver().query(inbox, new String[] { "_id", "thread_id", "address",
                        "person", "date", "body" }, null,
                null, null);
        boolean MatchFound=false;
        c.moveToNext();
        try {
            while (!MatchFound) {
            /*Integer index = c.getColumnIndexOrThrow("body");
            Integer pidIndex = c.getColumnIndexOrThrow("_id");
            String pid = c.getString(pidIndex);
            String dataMessage = c.getString(index);*/
                String id = c.getString(0);
                long threadId = c.getLong(1);
                String address = c.getString(2);
                String body = c.getString(5);
                System.out.println(body);
                if (body.equals(message)) {
                    System.out.println("Found a match");
                    ContentValues cv = new ContentValues();
                    cv.put(Telephony.Sms.ADDRESS, address);
                    cv.put(Telephony.Sms.BODY, body);
                    cv.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
                    //Uri uri = getContentResolver().insert(Telephony.Sms.CONTENT_URI, cv);
                    MatchFound = true;
                    //String uri = Telephony.Sms.CONTENT_URI.buildUpon().appendPath(id);
                    int deleted = context.getContentResolver().delete(Uri.parse("content://sms/"), "_id=? and thread_id=?",new String[] { String.valueOf(id),String.valueOf(threadId) });//"_id=?", new String[]{String.valueOf(id)});
                    System.out.println("Deleting SMS with id: " + threadId);
                    System.out.println(Uri.parse("content://sms/"+id).toString());
                    System.out.println("_id" + id);
                    System.out.println(deleted);
                }
                c.moveToNext();
            }
        }catch(Exception e){
            System.out.println("did not delete the messssage");
        }
    }
    private void addToQueue(String sender, String songId) {
        System.out.println(sender);

            if(queueLimit.containsKey(sender)){
                if(((System.currentTimeMillis() - requesterTime.get(sender))/60000)> queueRefreshPeriod){
                    requesterTime.put(sender, System.currentTimeMillis());
                    queueLimit.put(sender, queueLimitPeriod);
                }
                if(queueLimit.get(sender) > 0){
                    queueLimit.put(sender, queueLimit.get(sender)-1);
                    mSpotifyAppRemote.getPlayerApi().queue("spotify:track:"+songId);
                    //updateQueueUI(sender, songId);
                    getTrackInfo(songId, sender, queueLimitPeriod- queueLimit.get(sender));
                    //QLA.addData(temp.get(0));
                    //QLA.notifyItemInserted(QLA.fakeData.size()-1);
                    //Toast.makeText(this, "New Song Added to Queue", Toast.LENGTH_LONG).show();

                }
                else{
                    Toast.makeText(this, "Sender is over the limit!", Toast.LENGTH_LONG);
                    //Toast.makeText(this, "Sender is over the limit!" , Toast.LENGTH_LONG).show();
                }
            }
            else{
                queueLimit.put(sender, queueLimitPeriod-1);
                requesterTime.put(sender, System.currentTimeMillis());
                getTrackInfo(songId, sender, 1);
                //QLA.addData(temp.get(0));
                mSpotifyAppRemote.getPlayerApi().queue("spotify:track:"+songId);
                //Toast.makeText(this, "New Song Added to Queue", Toast.LENGTH_LONG).show();
            }
        }



    public void skipTrack(View view){
        mSpotifyAppRemote.getPlayerApi().skipNext();
        if(QLA.fakeData.size()>0){
            QLA.fakeData.remove(0);
            QLA.notifyDataSetChanged();
        }

    }

    public void backTrack(View view){
        mSpotifyAppRemote.getPlayerApi().skipPrevious();

    }

    public void play(View view){

        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
            if(playerState.isPaused){

                mSpotifyAppRemote.getPlayerApi().resume();
                ((ImageButton) findViewById(R.id.play)).setImageResource(android.R.drawable.ic_media_pause);


            }
            else{
                mSpotifyAppRemote.getPlayerApi().pause();
                ((ImageButton)findViewById(R.id.play)).setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    public void saveTrack(View view){

        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {

            mSpotifyAppRemote.getUserApi().getLibraryState(playerState.track.uri).setResultCallback(libraryState -> {
                if(libraryState.isAdded){
                    Toast.makeText(getApplicationContext(), "Song is already in library!", Toast.LENGTH_SHORT);
                    ((ImageButton)findViewById(R.id.save)).setImageResource(android.R.drawable.btn_star_big_on);
                    //findViewById(R.id.save).setBackgroundResource(android.R.drawable.btn_star_big_on);
                    //findViewById(R.id.save).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                else{
                    mSpotifyAppRemote.getUserApi().addToLibrary(playerState.track.uri);
                    ((ImageButton)findViewById(R.id.save)).setImageResource(android.R.drawable.btn_star_big_on);
                    //findViewById(R.id.save).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }

          //findViewById(R.id.save).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        });
    });

    }

    public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.ViewHolder>{
        int lastPosition = -1;
        List<songQ> fakeData = new LinkedList<>();//new String[]{"Sicko Mode", "God's Plan", "Nonstop", "HYFR", "Moonlight", "SAD!"};
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int i){
            //Create a new view
            CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_task, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder vH, int i){
            vH.songTitle.setText(fakeData.get(i).nameOfSong + " by " + fakeData.get(i).nameArtist);
            vH.requesterName.setText(fakeData.get(i).senderName);
            //vH.contactPhoto.setImageBitmap(fakeData.get(i).senderPhoto);
            RoundedImageView riv = vH.contactPhoto;
            riv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            riv.setCornerRadius((float) 10);
            riv.setBorderWidth((float) 2);
            riv.setBorderColor(Color.DKGRAY);
            riv.mutateBackground(true);
            Bitmap temp = fakeData.get(i).senderPhoto;
            if(temp==null){
                //riv.setBackgroundResource(android.R.drawable.sym_def_app_icon);
            }
            else{
                riv.setImageBitmap(temp);
            }
            //riv.setBackground(backgroundDrawable);
            riv.setOval(true);
            riv.setTileModeX(Shader.TileMode.REPEAT);
            riv.setTileModeY(Shader.TileMode.REPEAT);
            //System.out.println(fakeData.get(i).photoRequesting.url);
            vH.numberRequested.setText(fakeData.get(i).requesterNumber.toString() + "/" + queueLimitPeriod.toString());
            /*Bitmap temp = null;
            new DownloadImageTask(temp).execute(fakeData.get(i).photoRequesting.url);
            while(temp==null){}
            vH.albumPhoto.setImageBitmap(temp);*/
            Picasso.get().load(fakeData.get(i).photoRequesting.url).into(vH.albumPhoto);
            setAnimation(vH.itemView, i);
        }
        private void setAnimation(View viewToAnimate, int position)
        {
            // If the bound view wasn't previously displayed on screen, it's animated
            if (position > lastPosition)
            {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.push_left_in);
                viewToAnimate.startAnimation(animation);
                this.lastPosition = position;
            }
        }

        @Override
        public int getItemCount(){
            return fakeData.size();
        }
        public void addData(songQ temp){
            fakeData.add(temp);
        }
        public void addMultipleData(List<songQ> temp){
            fakeData.addAll(temp);
        }
        class ViewHolder extends RecyclerView.ViewHolder{
            CardView cardView;
            TextView songTitle;
            TextView requesterName;
            RoundedImageView contactPhoto;
            ImageView albumPhoto;
            TextView numberRequested;

            public ViewHolder(CardView card){
                super(card);
                cardView = card;
                songTitle = (TextView) card.findViewById(R.id.text1);
                requesterName = (TextView) card.findViewById(R.id.text2);
                contactPhoto = (RoundedImageView) card.findViewById(R.id.imageView4);
                albumPhoto = (ImageView)   card.findViewById(R.id.albumCover);
                numberRequested = (TextView) card.findViewById(R.id.numberRequested);
            }
        }
    }

    public String retrieveNameOfSender(String sender){
        System.out.println(sender);
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender));
        ContentResolver cR = getContentResolver();
        Cursor contactLookup = cR.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        String name = sender;
        if (contactLookup != null && contactLookup.getCount() > 0) {
            contactLookup.moveToNext();
            name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            System.out.println(name);
        }
        //ImageView temp = (ImageView) findViewById(R.id.currentSongImage);
        //temp.setImageBitmap(retrieveContactPhoto(this, "+18056035340"));
        return name;
    }
    public static Bitmap retrieveContactPhoto(Context context, String number) {
        ContentResolver contentResolver = context.getContentResolver();
        String contactId = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};
        Cursor cursor =
                contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            }
            cursor.close();
        }

        Bitmap photo= null; //BitmapFactory.decodeResource(context.getResources(),
               // R.drawable.default_image);

        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactId)));

            if (inputStream != null) {
                System.out.println("input steream is not null");
                photo = BitmapFactory.decodeStream(inputStream);
            }

            assert inputStream != null;
            if (inputStream!=null){
                inputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return photo;
    }

    class songQ{
        String senderName;
        String senderNumber;
        Bitmap senderPhoto;
        String nameOfSong;
        String nameArtist;
        kaaes.spotify.webapi.android.models.Image photoRequesting;
        String id;
        Integer requesterNumber;

        public songQ(String name, String artist, String id, String senderName, String senderNumber, kaaes.spotify.webapi.android.models.Image temp, Integer trackNumber){
            this.nameOfSong = name;
            this.nameArtist = artist;
            this.id = id;
            this.senderName = senderName;
            this.senderNumber = senderNumber;
            this.senderPhoto = retrieveContactPhoto(getApplicationContext(), senderNumber);
            this.photoRequesting = temp;
            this.requesterNumber = trackNumber;

        }
    }

    /*public void requestAuthorization() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://open.spotify.com/track/27a1mYSG5tYg7dmEjWBcmL";
        String responseDictionary = null;
        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        System.out.println("spotify authorized is a bitch");
                        //response
                        System.out.println(response);
                        //add code to create song objects
                        //Log.d("Response", response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("ERROR", "error => " + error.toString());
                    }
                }
        ); *//*{
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("client_id", CLIENT_ID);
                params.put("response_type", "code");
                params.put("redirect_uri", REDIRECT_URI);
                return params;
            }
        };*//*
        queue.add(getRequest);
    }*/


    public List<songQ> getTrackInfo(String songId, String sender, Integer trackNumber){
        Integer[] info = new Integer[3];
        info[0] = trackNumber;
        List<songQ> toReturn = new ArrayList<songQ>();
        spotify.getTrack(songId, new Callback<kaaes.spotify.webapi.android.models.Track>() {
            @Override
            public void success(kaaes.spotify.webapi.android.models.Track track, retrofit.client.Response response) {
                //Log.d("Track success", track.name);

                /*inter[0] = track.name;
                inter[1] = track.artists.get(0).name;
                inter[2] = track.id;*/
                String nameSender = retrieveNameOfSender(sender);
               /* System.out.println(track.id + "TRACK ID KAES");
                System.out.println(track.uri + "TRACK URIS ");
                System.out.println(track.external_urls.toString() + "TRACK EXTERNAL URLS KAES");*/
                songQ temp = new songQ(track.name, track.artists.get(0).name, track.id, nameSender, sender, track.album.images.get(0), info[0]);
                //toReturn.add(temp);
                Toast.makeText(getBaseContext(), nameSender.toString() + " added " + track.name, Toast.LENGTH_LONG);
                QLA.addData(temp);
                QLA.notifyDataSetChanged();
                //QLA.notifyDataSetChanged();
            }

            @Override
            public void failure(RetrofitError error) {
               // Log.d("Track failure", error.toString());

            }
        });

        return toReturn;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        Bitmap bmImage;

        public DownloadImageTask(Bitmap downloadedImage){//ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
               // Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage = (result);
        }
    }
    private PopupWindow pw;
    private void showPopup() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater)
            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.popup_settings,
                    (ViewGroup) findViewById(R.id.popup_1));
            pw = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true );
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
            EditText numberRequested = (EditText) layout.findViewById(R.id.requestsAllowed);
            numberRequested.setText(queueLimitPeriod.toString());
            EditText minutesRefreshed = (EditText) layout.findViewById(R.id.minuteRefresh);
            minutesRefreshed.setText(queueRefreshPeriod.toString());
            Button Close = (Button) layout.findViewById(R.id.close_popup);
            Close.setOnClickListener(cancel_button);
            Button Save = (Button) layout.findViewById(R.id.save);
            Save.setOnClickListener(save_button);
            Switch power = (Switch) layout.findViewById(R.id.power);
            power.setOnCheckedChangeListener(powerSwitch);
            power.setChecked(mainPower);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener cancel_button = new View.OnClickListener() {
        public void onClick(View v) {
            pw.dismiss();
        }
    };
    private Switch.OnCheckedChangeListener powerSwitch = new Switch.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mainPower= true;
            } else {
                mainPower=false;
            }
        }
    };


    private View.OnClickListener save_button = new View.OnClickListener(){
        public void onClick(View v){

            EditText numberRequested = (EditText) v.getRootView().findViewById(R.id.requestsAllowed);
            EditText minutesRefreshed = (EditText) v.getRootView().findViewById(R.id.minuteRefresh);
            if(numberRequested.getText() != null){
                queueLimitPeriod = Integer.parseInt(numberRequested.getText().toString());
                queueLimit.clear();
                requesterTime.clear();

            }
            if(minutesRefreshed.getText() != null){
                queueRefreshPeriod = Integer.parseInt(minutesRefreshed.getText().toString());
            }
            pw.dismiss();

        }
    };

}
