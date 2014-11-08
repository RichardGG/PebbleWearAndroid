package au.id.richardg.wear;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.util.DisplayMetrics;
import android.util.Log;


import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Richard on 1/09/2014.
 */
public class WearService extends NotificationListenerService{

    final static UUID PEBBLE_APP_UUID = UUID.fromString("69fbcd85-91ae-4fbf-8d71-a6321dca8b28");

    final static int //keys
            COMMAND = 0,
            BYTES = 1,
            LINE = 2,
            ID = 3;

    final static byte
            CLEAR = 0,
            UPDATETEXT = 1,
            UPDATEICON = 2,
            UPDATEIMAGE = 3,
            MOVE = 4,
            VIEW = 5,
            REPORT = 6,
            ACTIONS = 7;

    final static int
            TITLE_SIZE = 30,
            TEXT_SIZE = 80,
            CACHE_SIZE = 4,
            ICON_MESSAGE_ROWS = 16,
            IMAGE_MESSAGE_ROWS = 4;

    static StatusBarNotification[] watchNotifications = new StatusBarNotification[5];

    public static class MessageInterface {
        private static boolean readyForSend = true;
        private static ArrayList<PebbleDictionary> messageQueue = new ArrayList<PebbleDictionary>();
        private static ArrayList<StatusBarNotification> currentNotifs = new ArrayList<StatusBarNotification>();

        public static synchronized void updateCurrentNotifs(ArrayList<StatusBarNotification> newNotifs){
            currentNotifs.clear();
            for(int i=0; i<newNotifs.size(); i++)
                currentNotifs.add(newNotifs.get(i));
        }

        public static synchronized void send(Context context, PebbleDictionary message){
            if (message != null)
                messageQueue.add(message);
            if (readyForSend && messageQueue.size() > 0){
                PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, messageQueue.get(0));
                readyForSend = false;
            }
            if(messageQueue.isEmpty())
                Log.d("Wear","done");
        }

        public static synchronized void success(){
            if(!messageQueue.isEmpty())
                messageQueue.remove(0);
        }

        public static synchronized void setReady(){
            readyForSend = true;
        }

        public static synchronized void cancel(){
            messageQueue.clear();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        Log.d("Wear", "posted");

       // newNotification(sbn, 0);
        /*int position = 6;

        StatusBarNotification[] topNotifs = new StatusBarNotification[5];
        for(StatusBarNotification sbnA : getActiveNotifications())
        {
            boolean found = false;
            for(int i = 0; i < 5; i++)
            {
                if(!found) {
                    if (topNotifs[i] != null) {
                        if (topNotifs[i].getNotification().priority <= sbnA.getNotification().priority){
                            for(int j = 4; j > i; j--){
                                topNotifs[j] = topNotifs[j-1];
                            }
                            topNotifs[i] = sbnA;
                            found = true;
                        }
                    } else {
                        topNotifs[i] = sbnA;
                        found = true;
                    }
                }
            }
        }

        for(int i = 0; i < 5; i++){
            if(sbn.getId() == topNotifs[i].getId())
                position = i;
        }


        if(position < 5) {
            //check if updated notification
            int updatedNotification = -1;
            for (int i = 0; i < watchNotifications.length; i++)
                if (watchNotifications[i] != null && sbn.getId() == watchNotifications[i].getId())
                    updatedNotification = i;
            if (updatedNotification >= 0)
                //update the notification
                updateNotification(sbn, updatedNotification);
            else
                //otherwise send new notification
                newNotification(sbn, position);
        }*/
    }
    /*
        void updateNotification(StatusBarNotification sbn, int watchNo)
        {
            Log.d("Wear", "update");

            //for convenience
            Bundle watchExtras = watchNotifications[watchNo].getNotification().extras;
            Bundle newExtras = sbn.getNotification().extras;

            //check if text needs updating
            boolean updateText = false;
            if(!getTitleContent(watchExtras).equals(getTitleContent(newExtras)))
                updateText = true;
            if(!getTextContent(watchExtras).equals(getTextContent(newExtras)))
                updateText = true;

            //update text
            if(updateText) {
                sendMainContent(sbn, watchNo);
            }

            //update icon if needed
            if(watchExtras.getInt(Notification.EXTRA_SMALL_ICON) != newExtras.getInt(Notification.EXTRA_SMALL_ICON))
                sendIcon(sbn, watchNo);

            //update image if needed
            if(getImage(watchNotifications[watchNo]) != getImage(sbn))
                sendImage(sbn, watchNo);
        }

    */
    void newNotification(StatusBarNotification sbn, int position)
    {
        Log.d("Wear", "new");

        PebbleDictionary message = new PebbleDictionary();
        message.addInt8(COMMAND, CLEAR);
        message.addInt32(ID, position);
        MessageInterface.send(getApplicationContext(),message);

        Log.d("PWS", "clear");

        sendMainContent(sbn, position);
          sendIcon(sbn, position);
          sendImage(sbn,position);
    }

        void sendIcon(StatusBarNotification sbn, int position)
        {
            try {
                //get appname
                String packagename = sbn.getPackageName();
                //create context (to access resources) may create exception
                Context appContext = createPackageContext(packagename,CONTEXT_IGNORE_SECURITY);
                //get iconID
                int iconID = sbn.getNotification().extras.getInt(Notification.EXTRA_SMALL_ICON);
                //get the drawable as HIGH_DENSITY (48x48px) (32x32 is another option, but may scale poorly)
                Drawable iconDrawable = appContext.getResources().getDrawableForDensity(iconID, DisplayMetrics.DENSITY_HIGH);
                //create empty bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(), iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                //set the bitmap as the canvas
                Canvas canvas = new Canvas(iconBitmap);
                //make sure the drawable bounds are the same
                iconDrawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
                //drawable will draw into canvas (which is the bitmap)
                iconDrawable.draw(canvas);

                iconBitmap = Bitmap.createScaledBitmap(iconBitmap, 48, 48, true);

                byte iconData[] = new byte[ICON_MESSAGE_ROWS*(48 / 8)];

                for(int row = 0; row < 48; row++) {
                    PebbleDictionary dictionary = new PebbleDictionary();

                    int messageRow = row%ICON_MESSAGE_ROWS;
                    int dataStart = messageRow * (48/8);
                    if(messageRow == 0)
                        iconData = new byte[ICON_MESSAGE_ROWS * (48 / 8)];
                    for (int byteNo = 0; byteNo < 6; byteNo++) {


                        iconData[dataStart+byteNo] = 0;
                        for (int bitNo = 0; bitNo < 8; bitNo++) {
                            int color = iconBitmap.getPixel(byteNo * 8+7- bitNo, row);

                            int alpha = Color.alpha(color);
                            int red = Color.red(color);
                            int green = Color.green(color);
                            int blue = Color.blue(color);

                            int average = (int) ((float) ((blue + green + red) / 3)  * ((float) alpha / 255.0));

                            iconData[dataStart + byteNo] += (average < 140) ? 0 : 1;

                            if (bitNo != 7)
                                iconData[dataStart + byteNo] <<= 1;
                        }
                    }
                    if(messageRow == ICON_MESSAGE_ROWS - 1) {
                        dictionary.addInt8(COMMAND, UPDATEICON);
                        dictionary.addBytes(BYTES, iconData);
                        dictionary.addInt32(ID, position);
                        dictionary.addInt32(LINE, row - ICON_MESSAGE_ROWS + 1);
                        MessageInterface.send(getApplicationContext(), dictionary);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        void sendImage(StatusBarNotification sbn, int position)
        {
            Bitmap image = getImage(sbn);

            if(image!=null) {


                //figure out shortest direction
                int height = image.getHeight();
                int width = image.getWidth();
                if (height < width) {
                    width = (int) ((double) width * ((double) 144 / (double) height));
                    height = 144;
                } else {
                    height = (int) ((double) height * ((double) 144 / (double) width));
                    width = 144;
                }
                //scale to that size
                image = Bitmap.createScaledBitmap(image, width, height, true);
                //figure out starting point
                int top = (height - 144) / 2;
                int left = (width - 144) / 2;
                //crop to 144x144

                Log.d("PebbleWearService", "width = " + width + " height = " + height + " top = " + top + " left = " + left);
                image = Bitmap.createBitmap(image, left, top, 144, 144);


                int total_error[][] = new int[144][144];
                byte imageData[] = new byte[IMAGE_MESSAGE_ROWS * (144 / 8)]; //144 / 8

                for (int row = 0; row < 144; row++) {
                    PebbleDictionary dictionary = new PebbleDictionary();

                    int messageRow = row%IMAGE_MESSAGE_ROWS;
                    int dataStart = messageRow * (144/8);
                    if(messageRow == 0)
                        imageData = new byte[IMAGE_MESSAGE_ROWS * (144 / 8)];

                    int bits[] = new int[8];

                    for(int col = 0; col < 144; col++){

                        //get color from image
                        int color = image.getPixel(col, row);
                        int alpha = Color.alpha(color);
                        int red = Color.red(color);
                        int green = Color.green(color);
                        int blue = Color.blue(color);

                        //grey-scale color
                        int average = (int) ((float) ((blue + green + red) / 3) * ((float) alpha / 255.0));

                        //add total error from previous pixels
                        average += total_error[col][row];

                        //save bit for writing
                        bits[col%8] = (average < 127) ? 0 : 1;

                        //grey-scale color distance from black/white
                        int error_amount = average - ((average < 127) ? 0 : 255);

                        //stucki dithering algorithm
                        //row 1
                        if (col < 143)
                            total_error[col + 1][row] += error_amount * 8 / 42;
                        if(col<142)
                            total_error[col + 1][row] += error_amount * 4 / 42;
                        //row 2
                        if (row < 143) {
                            if (col > 1)
                                total_error[col - 2][row + 1] += error_amount * 2 / 42;
                            if (col > 0)
                                total_error[col - 1][row + 1] += error_amount * 4 / 42;
                            //col is the same
                            total_error[col    ][row + 1] += error_amount * 8 / 42;
                            if (col < 143)
                                total_error[col + 1][row + 1] += error_amount * 4 / 42;
                            if (col < 142)
                                total_error[col + 2][row + 1] += error_amount * 2 / 42;
                        }
                        //row 3
                        if (row < 142) {
                            if (col > 1)
                                total_error[col - 2][row + 2] += error_amount * 1 / 42;
                            if (col > 0)
                                total_error[col - 1][row + 2] += error_amount * 2 / 42;
                            //col is the same
                            total_error[col    ][row + 2] += error_amount * 4 / 42;
                            if (col < 143)
                                total_error[col + 1][row + 2] += error_amount * 2 / 42;
                            if (col < 142)
                                total_error[col + 2][row + 2] += error_amount * 1 / 42;
                        }

                        //if last bit, save bits as byte
                        if (col % 8 == 7) {
                            //calculate byteNo
                            int byteNo = col / 8;

                            //make sure byte is zeroed
                            imageData[dataStart+byteNo] = 0;

                            for(int bit = 0; bit < 8; bit++){
                                //write bits in reverse order
                                imageData[dataStart+byteNo] += bits[7-bit];

                                //shift bit if not last
                                if(bit < 7)
                                    imageData[dataStart+byteNo] <<= 1;
                            }
                        }
                    }
                    if(messageRow % IMAGE_MESSAGE_ROWS == IMAGE_MESSAGE_ROWS - 1) {
                        dictionary.addInt8(COMMAND, UPDATEIMAGE);
                        dictionary.addInt32(LINE, row - IMAGE_MESSAGE_ROWS + 1);
                        dictionary.addInt32(ID, position);
                        dictionary.addBytes(BYTES, imageData);
                        MessageInterface.send(getApplicationContext(), dictionary);
                    }
                }
            }
        }

        Bitmap getImage(StatusBarNotification sbn)
        {
            Bundle extras = sbn.getNotification().extras;

            Bitmap image;
            //check for picture
            image = extras.getParcelable(Notification.EXTRA_PICTURE);
            //check for icon
            if(image == null)
                image = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
            //check for appicon
            if(image == null) {
                try {
                    Drawable appicon = getPackageManager().getApplicationIcon(sbn.getPackageName());
                    //create empty bitmap
                    image = Bitmap.createBitmap(appicon.getIntrinsicWidth(), appicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    //set the bitmap as the canvas
                    Canvas appcanvas = new Canvas(image);
                    //make sure the drawable bounds are the same
                    appicon.setBounds(0, 0, appcanvas.getWidth(), appcanvas.getHeight());
                    //drawable will draw into canvas (which is the bitmap)
                    appicon.draw(appcanvas);

                    // image = Bitmap.createScaledBitmap(image, 144, 144, true);
                } catch (PackageManager.NameNotFoundException e) {
                    image = null;
                }
            }
            return image;
        }

    void sendMainContent(StatusBarNotification sbn, int position)
    {
        //if vibrates

        Bundle extras = sbn.getNotification().extras;

        PebbleDictionary message = new PebbleDictionary();
        message.addInt8(COMMAND, UPDATETEXT);
        message.addInt32(ID, position);
        message.addBytes(BYTES, getTextByes(extras));
        MessageInterface.send(getApplicationContext(),message);

        Log.d("PWS", "sent");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d("Wear", "removed");
    }

    @Override
    public void onCreate()
    {
        Log.d("Wear", "created");
		
		

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID)
        {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.d("Wear", "data: " + data.getInteger(1));
                if(data.getInteger(1) == 0)
                    listRequest();
                if(data.getInteger(1) == 1)
                    removeNotification(data.getInteger(2).intValue());
                if(data.getInteger(1) == 2)
                    sendActions(data.getInteger(2).intValue());
            }

        });

        PebbleKit.registerReceivedNackHandler(this,new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID)
        {
            @Override
            public void receiveNack(final Context context, final int transactionId){
                Log.d("Wear", "nack");
                MessageInterface.cancel();
                MessageInterface.setReady();
                MessageInterface.send(getApplicationContext(),null);
            }
        });

        PebbleKit.registerReceivedAckHandler(this,new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID)
        {
            @Override
            public void receiveAck(final Context context, final int transactionId){
                //Log.d("Wear", "ack");
                MessageInterface.success();
                MessageInterface.setReady();
                MessageInterface.send(getApplicationContext(),null);

            }
        });

    }

        private void sendActions(int id)
        {
            Log.d("test","test");

            StatusBarNotification[] topNotifs = new StatusBarNotification[5];
            for(StatusBarNotification sbnA : getActiveNotifications())
            {
                boolean found = false;
                for(int i = 0; i < 5; i++)
                {
                    if(!found) {
                        if (topNotifs[i] != null) {
                            if (topNotifs[i].getNotification().priority <= sbnA.getNotification().priority){
                                for(int j = 4; j > i; j--){
                                    topNotifs[j] = topNotifs[j-1];
                                }
                                topNotifs[i] = sbnA;
                                found = true;
                            }
                        } else {
                            topNotifs[i] = sbnA;
                            found = true;
                        }
                    }
                }
            }
            if(topNotifs[id].getNotification().actions != null) {
                PebbleDictionary message = new PebbleDictionary();
                message.addInt8(COMMAND, ACTIONS);
                message.addBytes(BYTES, getActionBytes(topNotifs[id].getNotification().actions));
                MessageInterface.send(getApplicationContext(),message);
            }
        }

        private byte[] getActionBytes(Notification.Action[] actions)
        {
            byte[] output = new byte[60];
            for(int i=0; i<actions.length; i++)
            {
                for(int j=0; j<20; j++)
                {
                    output[j + i*20] = (byte)actions[i].title.charAt(j);

                }
            }
            return output;
        }

        private void removeNotification(int id)
        {
            StatusBarNotification[] topNotifs = new StatusBarNotification[5];
            for(StatusBarNotification sbnA : getActiveNotifications())
            {
                boolean found = false;
                for(int i = 0; i < 5; i++)
                {
                    if(!found) {
                        if (topNotifs[i] != null) {
                            if (topNotifs[i].getNotification().priority <= sbnA.getNotification().priority){
                                for(int j = 4; j > i; j--){
                                    topNotifs[j] = topNotifs[j-1];
                                }
                                topNotifs[i] = sbnA;
                                found = true;
                            }
                        } else {
                            topNotifs[i] = sbnA;
                            found = true;
                        }
                    }
                }
            }

            cancelNotification(topNotifs[id].getPackageName(),topNotifs[id].getTag(),topNotifs[id].getId());
        }

    public byte[] getUniqueID(StatusBarNotification sbn)
    {
        byte[] bytes = new byte[104];

        String packageName = sbn.getPackageName();
        //120 chars
        for(int i = 0; i < 100; i++)
        {
            if(packageName.length() > i)
                bytes[i] = (byte)packageName.charAt(packageName.length()-1 - i); //reversed because the first 100 chars are more likely to be the same than the last (I think) com.adobe./com.adobe. vs  redaer.ebo/pohsotohp.
            else
                bytes[i] = '\0';
        }
        //4 byte int32
        int id = sbn.getId();
        bytes[100] = (byte)((id >> (8*0)) & 0xff);
        bytes[101] = (byte)((id >> (8*1)) & 0xff);
        bytes[102] = (byte)((id >> (8*2)) & 0xff);
        bytes[103] = (byte)((id >> (8*3)) & 0xff);


        return bytes;
    }

        public void listRequest()
        {
            StatusBarNotification[] topNotifs = new StatusBarNotification[5];
            for(StatusBarNotification sbnA : getActiveNotifications())
            {
                //Log.d("mytest", "package = " + sbnA.getPackageName());
                //100 chars
                //Log.d("mytest", "id = " + sbnA.getId());
                //id 32 bit int




                //NotificationCompat.WearableExtender wearableExtender =
                //        new NotificationCompat.WearableExtender(sbnA.getNotification());
                //Log.d("weaeae", "aaaa");
                //wearableExtender.


                //Log.d("arweaw", "getting group key");
                //Log.d("wearaarf", .extras.getString(NotificationCompatExtras.EXTRA_GROUP_KEY));

                boolean found = false;
                for(int i = 0; i < 5; i++)
                {
                    if(!found) {
                        if (topNotifs[i] != null) {
                            if (topNotifs[i].getNotification().priority <= sbnA.getNotification().priority){
                                for(int j = 4; j > i; j--){
                                    topNotifs[j] = topNotifs[j-1];
                                }
                                topNotifs[i] = sbnA;
                                found = true;
                            }
                        } else {
                            topNotifs[i] = sbnA;
                            found = true;
                        }
                    }
                }
            }

            for(int i = 0; i < 5; i++)
            {
                if(topNotifs[i] != null)
                    newNotification(topNotifs[i],i);
            }
        }

        public void sendList()
        {
            StatusBarNotification[] watchNotifications = new StatusBarNotification[5];
            for(StatusBarNotification sbnA : getActiveNotifications())
            {
                boolean found = false;
                for(int i = 0; i < 5; i++)
                {
                    if(!found) {
                        if (watchNotifications[i] != null) {
                            if (watchNotifications[i].getNotification().priority <= sbnA.getNotification().priority){
                                for(int j = 4; j > i; j--){
                                    watchNotifications[j] = watchNotifications[j-1];
                                }
                                watchNotifications[i] = sbnA;
                                found = true;
                            }
                        } else {
                            watchNotifications[i] = sbnA;
                            found = true;
                        }
                    }
                }
            }

            PebbleDictionary message = new PebbleDictionary();
            for(int i = 0; i < 5; i++) {
                if(watchNotifications[i] != null) {
                    //message.addString(i, getTitleContent(watchNotifications[i].getNotification().extras));
                }
                else {
                    message.addString(i, " ");
                }
            }
            MessageInterface.send(getApplicationContext(),message);

            Log.d("Wear","sent titles");


            message = new PebbleDictionary();
            for(int i = 0; i < 5; i++) {
                if(watchNotifications[i] != null) {
                    //message.addString(5+i,getTextContent(watchNotifications[i].getNotification().extras) );
                }
                else {
                    message.addString(5+i, " ");
                }
            }
            MessageInterface.send(getApplicationContext(),message);

            Log.d("Wear","sent text");
        }

    private static byte[] getTextByes(Bundle extras)
    {
        CharSequence title = getTitleContent(extras);
        CharSequence text = getTextContent(extras);
        byte[] output = new byte[TITLE_SIZE+TEXT_SIZE];


        String[] titleWholeChars = new String[title.length()];
        String[] textWholeChars = new String[text.length()];
        int textLength = 0;
        //Log.d("length", "length: " + text.length());
        for(int i = 0; i < text.length(); i++)
        {
            textWholeChars[textLength] += text.charAt(i);
            if(Character.isSurrogate(text.charAt(i)))
            {
                if(textWholeChars[textLength].length()>1)
                    textLength++;
                //else don't add to length
            }
            else
                textLength++;
            //Log.d("Char " + i, "is surrogate? " + (Character.isSurrogate(text.charAt(i))?"yes":"no"));
        }
        Log.d("text", "length: "+text.length());
        Log.d("textWholeChars", "length: "+textLength);

        try {

            byte[] titleBytes = title.toString().getBytes("UTF-8");
            byte[] textBytes = text.toString().getBytes("UTF-8");

            //Log.d("length", "length: " + title.length());
            //Log.d("Char 0",  "Is surrogate? " + (Character.isSurrogate(text.charAt(0))?"Yes":"No"));
            //Log.d("Char 1",  "Is surrogate? " + (Character.isSurrogate(text.charAt(1))?"Yes":"No"));
            //Log.d("Char 0,1", "Is surrogate pair?" + (Character.isSurrogatePair(text.charAt(0),text.charAt(1))?"Yes":"No"));
            //Log.d("Length of char 0", "length: " + text.toString().toCharArray().length);//(text.toString().toCharArray()[0] + "").getBytes().length);

            for(int i=0; i<TITLE_SIZE; i++) {
                if (titleBytes.length > i){
                    output[i] = titleBytes[i];
                }else
                    output[i] = '\0';
            }
            for(int i=0; i<TEXT_SIZE; i++)
            {
                if(textBytes.length>i) {
                    output[TITLE_SIZE + i] = textBytes[i];
                }else
                    output[TITLE_SIZE+i] = '\0';
            }

        }catch(java.io.UnsupportedEncodingException e){}

        /*for(int i=0; i < TITLE_SIZE; i++) {
            if(i < title.length())
                //if(title.charAt(i) < 255)
                    output[i] = (byte) title.charAt(i);
                //else
                    //output[i] = (byte)'?';
            else
                output[i] = '\0';
        }
        for(int i=0;i<TEXT_SIZE;i++){
            if(i < text.length())
                if(text.charAt(i) < 255)
                    output[i+TITLE_SIZE] = (byte) text.charAt(i);
                else {
                    output[i + TITLE_SIZE] = (byte) '?';
                    try {
                        Log.d("jfshauf", "char = " + text.charAt(i) + " (" + (int) text.charAt(i) + ")" + title.toString().getBytes("UTF-8")[0]);
                    }catch(java.io.UnsupportedEncodingException e){}
                }
            else
                output[i+TITLE_SIZE] = '\0';
        }*/
        return output;
    }

    private static CharSequence getTitleContent(Bundle extras)
    {
        CharSequence titleContent = "";
        if(extras.get(Notification.EXTRA_TITLE) != null)
        {
            int messageLength = extras.getCharSequence(Notification.EXTRA_TITLE).length();
            if (messageLength > TITLE_SIZE)
                messageLength = TITLE_SIZE;
            titleContent = extras.getCharSequence(Notification.EXTRA_TITLE).subSequence(0, messageLength).toString();
        }
        return titleContent;

    }

    static CharSequence getTextContent(Bundle extras)
    {
        CharSequence textContent = " ";
        if(extras.get(Notification.EXTRA_TEXT) != null)
        {
            int messageLength = extras.getCharSequence(Notification.EXTRA_TEXT).length();
            if (messageLength > TEXT_SIZE)
                messageLength = TEXT_SIZE;
            textContent = extras.getCharSequence(Notification.EXTRA_TEXT).subSequence(0, messageLength).toString();
        }
        return textContent;
    }

    @Override
    public void onDestroy()
    {
        Log.d("Wear", "destroyed");
    }
}
