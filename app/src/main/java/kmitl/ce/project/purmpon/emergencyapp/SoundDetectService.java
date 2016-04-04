package kmitl.ce.project.purmpon.emergencyapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IllegalFormatException;

public class SoundDetectService extends Service implements  RecognitionListener{

    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    protected AudioManager mAudioManager;
    private onResultsReady mListener;
    protected boolean mIsListening;
    private boolean mIsStreamSolo;
    private boolean mMute=true;
    private boolean mRunning;

    private String LOG_TAG = "VoiceRecognitionActivity";

    public void onCreate() {

        mRunning = false;

        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        try{
            mListener=new onResultsReady() {
                @Override
                public void onResults(ArrayList<String> results) {



                    if(results!=null && results.size()>0)
                    {

                        if(results.size()==1)
                        {
                            destroy();
                            showResult("LOCATION_UPDATED", "results", results.get(0));
                            //result_tv.setText(results.get(0));
                            Log.d(LOG_TAG,results.get(0));
                        }
                        else {
                            StringBuilder sb = new StringBuilder();
                            if (results.size() > 5) {
                                results = (ArrayList<String>) results.subList(0, 5);
                            }
                            for (String result : results) {
                                if(result.trim().equals("open")){
                                    Log.i(LOG_TAG, "correct key word");
                                    openApp();
                                }
                                Log.i(LOG_TAG,"Show word"+result);
                                sb.append(result).append("\n");
                            }
                            //result_tv.setText(sb.toString());
                            showResult("LOCATION_UPDATED", "results", sb.toString());
                        }
                    }
                    else
                        //result_tv.setText(getString(R.string.no_results_found));
                        showResult("LOCATION_UPDATED", "results", "No results found");

                }
            };
        }
        catch(ClassCastException e)
        {
            Log.e(LOG_TAG,e.toString());
        }


        startListening();
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!mRunning) {
            mRunning = true;
            // do something
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG,"service running");
                }
            }, 1000);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    protected void openApp(){
        try {
            Log.i(LOG_TAG, "Open App");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //           intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            this.startActivity(intent);
        }catch (Exception e){
            Log.e(LOG_TAG,e.toString());
        }

    }

    private void showResult(String location,String key,String text){
        Intent i = new Intent(location);
        i.putExtra(key, text);
        sendBroadcast(i);
    }

    private void listenAgain()
    {
        if(mIsListening) {
            mIsListening = false;
            speech.cancel();
            startListening();
        }
    }
    private void startListening()
    {
        if(!mIsListening)
        {
            mIsListening = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // turn off beep sound
                if (!mIsStreamSolo && mMute) {
                    mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                    mIsStreamSolo = true;
                }
            }
            speech.startListening(recognizerIntent);
        }
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        Intent i = new Intent("LOCATION_UPDATED");
        i.putExtra("results", "beginningOfSpeech");
        sendBroadcast(i);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        if(error==SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        {
            if(mListener!=null) {
                ArrayList<String> errorList=new ArrayList<String>(1);
                errorList.add("ERROR RECOGNIZER BUSY");
                if(mListener!=null)
                    mListener.onResults(errorList);
            }
            return;
        }

        if(error==SpeechRecognizer.ERROR_NO_MATCH)
        {
            if(mListener!=null)
                mListener.onResults(null);
        }

        if(error==SpeechRecognizer.ERROR_NETWORK)
        {
            ArrayList<String> errorList=new ArrayList<String>(1);
            errorList.add("STOPPED LISTENING");
            if(mListener!=null)
                mListener.onResults(errorList);
        }
        Log.d(LOG_TAG, "error = " + error);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listenAgain();
            }
        }, 1000);

    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results)
    {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches) {
            if(result.trim().equals("open")){
                Log.i(LOG_TAG, "correct key word");
                openApp();
            }
            Log.i(LOG_TAG,"Show word"+result);
            text += result + "\n";
        }
        showResult("LOCATION_UPDATED","results",text);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listenAgain();
            }
        }, 1000);

    }

    @Override
    public void onRmsChanged(float rmsdB) {
       // Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);


    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void destroy(){
        mIsListening=false;
        if (!mIsStreamSolo) {
            mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            mIsStreamSolo = true;
        }
        Log.d(LOG_TAG, "onDestroy");
        if (speech != null)
        {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech=null;
        }

    }
    public boolean ismIsListening() {
        return mIsListening;
    }


    public interface onResultsReady
    {
        public void onResults(ArrayList<String> results);
    }

    public void mute(boolean mute)
    {
        mMute=mute;
    }

    public boolean isInMuteMode()
    {
        return mMute;
    }
}
