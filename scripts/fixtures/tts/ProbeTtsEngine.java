package app.cd.mitool.ttsprobe;
import android.speech.tts.*;
import android.content.*;
import android.media.AudioFormat;
/** Synthetic PCM test engine. It does NOT synthesize Vietnamese speech. */
public class ProbeTtsEngine extends TextToSpeechService {
 public static class ModeReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
   c.getSharedPreferences("qa",0).edit().putBoolean("fail",i.getBooleanExtra("fail",false)).apply();
  }
 }
 @Override protected int onIsLanguageAvailable(String l,String c,String v) {
  return l.equals("vi")||l.equals("vie") ? TextToSpeech.LANG_COUNTRY_AVAILABLE : TextToSpeech.LANG_NOT_SUPPORTED;
 }
 @Override protected String[] onGetLanguage() { return new String[]{"vie","VNM",""}; }
 @Override protected int onLoadLanguage(String l,String c,String v) { return onIsLanguageAvailable(l,c,v); }
 @Override protected void onStop() { }
 @Override protected void onSynthesizeText(SynthesisRequest r,SynthesisCallback c) {
  if (getSharedPreferences("qa",0).getBoolean("fail",false)) { c.error(TextToSpeech.ERROR_SYNTHESIS); c.done(); return; }
  c.start(16000,AudioFormat.ENCODING_PCM_16BIT,1);
  byte[] pcm=new byte[1600];
  for(int j=0;j<pcm.length/2;j++){short s=(short)(Math.sin(j*2*Math.PI*440/16000)*6000);pcm[2*j]=(byte)s;pcm[2*j+1]=(byte)(s>>8);}
  for(int j=0;j<10;j++) if(c.audioAvailable(pcm,0,pcm.length)!=TextToSpeech.SUCCESS) return;
  c.done();
 }
}
