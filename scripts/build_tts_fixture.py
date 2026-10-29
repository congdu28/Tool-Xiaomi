"""Builds a synthetic test TTS APK. Use only in an isolated emulator."""
import argparse, os, subprocess, zipfile
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--sdk',required=True);p.add_argument('--java-home',required=True)
p.add_argument('--keystore',required=True);p.add_argument('--alias',required=True)
a=p.parse_args()
assert os.environ.get('MITOOL_QA_SIGNING_PASSWORD'), 'Set MITOOL_QA_SIGNING_PASSWORD for your test keystore.'
root=Path(__file__).resolve().parents[1]; sdk=Path(a.sdk); bt=sdk/'build-tools/34.0.0'; java=Path(a.java_home)/'bin'
src=root/'scripts/fixtures/tts'; out=root/'app/build/audio-qa'
(out/'classes').mkdir(parents=True,exist_ok=True);(out/'dex').mkdir(exist_ok=True)
def run(args): subprocess.run([str(x) for x in args],check=True)
run([java/'javac.exe','-source','8','-target','8','-classpath',sdk/'platforms/android-35/android.jar','-d',out/'classes',src/'ProbeTtsEngine.java'])
run([java/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',sdk/'platforms/android-35/android.jar','--min-api','24','--output',out/'dex',*list((out/'classes').rglob('*.class'))])
run([bt/'aapt.exe','package','-f','-M',src/'AndroidManifest.xml','-S',src/'res','-I',sdk/'platforms/android-35/android.jar','-F',out/'tts-unsigned.apk'])
with zipfile.ZipFile(out/'tts-unsigned.apk','a') as z: z.write(out/'dex/classes.dex','classes.dex')
run([bt/'zipalign.exe','-f','4',out/'tts-unsigned.apk',out/'tts-aligned.apk'])
run([bt/'apksigner.bat','sign','--ks',a.keystore,'--ks-pass','env:MITOOL_QA_SIGNING_PASSWORD','--ks-key-alias',a.alias,'--out',out/'tts-probe.apk',out/'tts-aligned.apk'])
print(out/'tts-probe.apk')
