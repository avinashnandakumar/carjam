package sideprojects.carjam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.TextView;
import android.widget.Toast;


import java.sql.SQLOutput;

public class SmsReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {


            Bundle data  = intent.getExtras();
            System.out.println("HELLOP");
            Object[] pdus = (Object[]) data.get("pdus");

            for(int i=0;i<pdus.length;i++){
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);

                String sender = smsMessage.getDisplayOriginatingAddress();
                //Check the sender to filter messages which we require to read
                String messageBody = smsMessage.getMessageBody();
                Toast.makeText(context, messageBody, Toast.LENGTH_LONG).show();
                //updateMessage(messageBody);
                System.out.println("it didnt work");
                //MainActivity.updateMessage()
                //TextView smsTextView = obj.getSMSTextView();
                //smsTextView.setText("roaster");
                Intent temp = new Intent(context, MainActivity.class);
                //temp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                temp.putExtra("message", messageBody);
                context.startActivity(temp);
            }
            //Toast.makeText(context, "something received", Toast.LENGTH_SHORT).show();
            //final TextView smsMessage  = ((Activity) context).findViewById(R.id.smsMessage);
            //smsMessage.setText("It worked roaster!");
            //MainActivity.getInstace().updateSMSMessage("wqerqwer");
            //MainActivity.smsTextView = "ROASTER";

        }

        /*public static void bindListener(SmsListener listener) {
            mListener = listener;
        }*/

}

