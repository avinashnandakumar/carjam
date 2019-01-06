package sideprojects.carjam;

import android.Manifest;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsMessage;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final String CLIENT_ID = "0407da6387be4e3aba45344f618f48cf";
    private SpotifyAppRemote mSpotifyAppRemote;
    boolean mIsReceiverRegistered = false;
    private SmsReciever mReciever;
    private Integer queueLimitPeriod = 2;
    private Map<String, Integer> queueLimit;
    RecyclerView recyclerView;
    QueueListAdapter QLA;

    private int MY_PERMISSIONS_REQUEST_SMS_RECEIVE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queueLimit = new HashMap<String,Integer>();
        QLA = new QueueListAdapter();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(QLA);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
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
                        songName.setText(track.name);
                        songArtist.setText(track.artist.name);
                        mSpotifyAppRemote.getImagesApi().getImage(track.imageUri).setResultCallback(image -> {songImage.setImageBitmap(image);});
                        //Toast.makeText(this, track.name, Toast.LENGTH_SHORT).show();
                    }
                });

        /*// Instantiate the RequestQueue.
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
        queue.add(stringRequest);*/
        // Then we will write some more code here
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                MY_PERMISSIONS_REQUEST_SMS_RECEIVE);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                24);
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

    @Override
    protected void onPause(){
        /*if (mIsReceiverRegistered) {
            unregisterReceiver(mReciever);
            mReciever = null;
            mIsReceiverRegistered = false;
        }*/
        super.onPause();

    }

    public void updateMessage(String messageBody) {
        TextView smsMessage = (TextView) findViewById(R.id.smsMessage);
        smsMessage.setText(messageBody);
        smsMessage.setMovementMethod(new ScrollingMovementMethod());
        System.out.println("It worked");
    }

    private class SmsReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            Bundle data  = intent.getExtras();
            Object[] pdus = (Object[]) data.get("pdus");
            String fullMessage = "";
            String senderNumber= "";
            for(int i=0;i<pdus.length;i++){
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
            if(matcher2.find() || matcher3.find()){
                //String temp = messageBody.substring(6, 15);
                //if (temp.equals(" a song f")) {
                System.out.println("In the loop");
                updateMessage(fullMessage);
                Pattern p = Pattern.compile("https.*\\?");
                Matcher matcher = p.matcher(fullMessage);
                if (matcher.find()) {
                    System.out.println(matcher.group());
                    String songToQueue = matcher.group();
                    String songId = songToQueue.substring(31,songToQueue.length()-1);
                    System.out.println(songId);
                    addToQueue(senderNumber, songId);
                    deleteText(context, senderNumber, fullMessage);
                }

            } else {
                System.out.println("Didnot go in the loop");
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
            if(queueLimit.get(sender) < queueLimitPeriod){
                queueLimit.put(sender, queueLimit.get(sender)+1);
                mSpotifyAppRemote.getPlayerApi().queue("spotify:track:"+songId);
                Toast.makeText(this, "New Song Added to Queue", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(this, "Sender is over the limit! Try again later. ", Toast.LENGTH_SHORT);
                Toast.makeText(this, "Sender is over the limit!" , Toast.LENGTH_LONG).show();
            }
        }
        else{
            queueLimit.put(sender, 1);
            mSpotifyAppRemote.getPlayerApi().queue("spotify:track:"+songId);
            Toast.makeText(this, "New Song Added to Queue", Toast.LENGTH_LONG).show();
        }

    }

    public void skipTrack(View view){
        mSpotifyAppRemote.getPlayerApi().skipPrevious();
    }

    public void backTrack(View view){
        mSpotifyAppRemote.getPlayerApi().skipNext();

    }

    public void play(View view){

        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
            if(playerState.isPaused){
                mSpotifyAppRemote.getPlayerApi().resume();
            }
            else{
                mSpotifyAppRemote.getPlayerApi().pause();
            }
        });
    }

    public void saveTrack(View view){
        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
          mSpotifyAppRemote.getUserApi().addToLibrary(playerState.track.uri);
        });
    }

    public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.ViewHolder>{
        String[] fakeData = new String[]{"Sicko Mode", "God's Plan", "Nonstop", "HYFR", "Moonlight", "SAD!"};
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int i){
            //Create a new view
            CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_task, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder vH, int i){
            vH.titleView.setText(fakeData[i]);
        }

        @Override
        public int getItemCount(){
            return fakeData.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            CardView cardView;
            TextView titleView;

            public ViewHolder(CardView card){
                super(card);
                cardView = card;
                titleView = (TextView) card.findViewById(R.id.text1);
            }
        }
    }

}
