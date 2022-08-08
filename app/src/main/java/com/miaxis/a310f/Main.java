package com.miaxis.a310f;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fingerprint.zzFingerAlgID;

import org.zz.api.MXFingerAPI;
import org.zz.bean.IDCardRecord;
import org.zz.bean.IdCardParser;
import org.zz.idcard_hid_driver.BMP;
import org.zz.idcard_hid_driver.IdCardDriver;
import org.zz.idcard_hid_driver.R;

import java.util.Arrays;
import java.util.Calendar;

import static org.zz.bean.IdCardParser.fingerPositionCovert;

/**
 * @author ZJL
 * @date 2022/6/13 18:20
 * @des
 * @updateAuthor
 * @updateDes
 */
public class Main extends Activity {
    TextView cardType,chineseName,englishName,sex,nation,borth,address,number,passnumber,authority,
            authorityCount,data,finger1,finger2,cardVersion;
    TextView tip;

    TextView scanTxt;
    Button btn_cardFullInfo;
    ImageView cardImg;

    ImageView fingerImg;
    Button camparRe;
    Spinner fingerSpinner;
    ArrayAdapter<String> adapter;



    final String TAG="MainActivity";
    IDCardRecord idCard=new IDCardRecord();

    IdCardDriver idCardDriver=null;
    private static final int FINGER_DATA_SIZE = 512;
    int pid=0x7;
    int vid=0x10c4;
    boolean scanFlag=false;
    ScanThread scanThread=null;

    boolean fingerFlag=false;
    GetImageThread m_GetImageThread;
    private ProgressDialog m_progressDlg;
    int IMAGE_X_BIG = 256;
    int IMAGE_Y_BIG = 360;
    private  final  int IMAGE_SIZE_BIG  = IMAGE_X_BIG*IMAGE_Y_BIG;
    private static  int  TIME_OUT     = 15 * 1000;
    int iVID = 0x821B;
    int iPID = 0x0202;
    byte[] bFingerImage = new byte[IMAGE_SIZE_BIG];
    MXFingerAPI fingerAPI;

    private boolean verifyRun = true;
    private static final int TZ_SIZE = 512;          // 指纹特征长度  BASE64


    private SoundPool soundPool;
    private SparseIntArray soundMap;
    public static final int FINGER_RIGHT_0 = 0;
    public static final int FINGER_RIGHT_1 = 1;
    public static final int FINGER_RIGHT_2 = 2;
    public static final int FINGER_RIGHT_3 = 3;
    public static final int FINGER_RIGHT_4 = 4;

    public static final int FINGER_LEFT_0 = 5;
    public static final int FINGER_LEFT_1 = 6;
    public static final int FINGER_LEFT_2 = 7;
    public static final int FINGER_LEFT_3 = 8;
    public static final int FINGER_LEFT_4 = 9;

    private int mCurSoundId;
    private boolean continuePlaySoundFlag = true;
    public static final float LEFT_VOLUME = 1.0f, RIGHT_VOLUME = 1.0f;
    public static final int PRIORITY = 1, LOOP = 0;
    public static final float SOUND_RATE = 1.0f;//正常速率
    private int soundPos=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置无标题
        setContentView(R.layout.activity_main2);
        initView();
        initData();
        initSound();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlay();
        stopVerifyFinger();
    }

    private void initView(){
        String strInfo = "获取指纹图像，请按手指...";
        m_progressDlg  = new ProgressDialog(this);
        m_progressDlg.setTitle("提示信息");
        m_progressDlg.setMessage(strInfo);
        m_progressDlg.setCancelable(false);
        m_progressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        fingerFlag=false;
                        stopVerifyFinger();
                    }
                });


        cardType=findViewById(R.id.cardType_txt);
        chineseName=findViewById(R.id.chineseName_txt);
        englishName=findViewById(R.id.englishName_txt);
        sex=findViewById(R.id.sex_txt);
        nation=findViewById(R.id.nation_txt);
        borth=findViewById(R.id.borth_txt);
        address=findViewById(R.id.adress_txt);
        number=findViewById(R.id.number_txt);
        passnumber=findViewById(R.id.passnumber_txt);
        authority=findViewById(R.id.authority_txt);
        authorityCount=findViewById(R.id.authorityCount_txt);
        data=findViewById(R.id.data_txt);
        finger1=findViewById(R.id.finger1_txt);
        finger2=findViewById(R.id.finger2_txt);
        cardVersion=findViewById(R.id.cardver_txt);
        btn_cardFullInfo=findViewById(R.id.btn_cardFullInfo);
        cardImg=findViewById(R.id.cardImg);

        tip=findViewById(R.id.Tip);
        scanTxt=findViewById(R.id.scan_txt);

        fingerImg=findViewById(R.id.fingerImg);
        fingerSpinner=findViewById(R.id.fingerSpinner);
        camparRe=findViewById(R.id.btn_camparRe);
    }

    private void initData(){
        fingerAPI= new MXFingerAPI(this, iPID, iVID);
        idCardDriver=new IdCardDriver(this);
        //        try {
        //            idCardDriver.StartCard();
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }

        adapter= new ArrayAdapter<String>(this, R.layout.spinner_item_main,IdCardParser.fingerPosition());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fingerSpinner.setAdapter(adapter);
        fingerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                soundPos=position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initSound(){
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundMap = new SparseIntArray();
        soundMap.put(FINGER_RIGHT_0, soundPool.load(this, R.raw.finger_right_0, 1));
        soundMap.put(FINGER_RIGHT_1, soundPool.load(this, R.raw.finger_right_1, 1));
        soundMap.put(FINGER_RIGHT_2, soundPool.load(this, R.raw.finger_right_2, 1));
        soundMap.put(FINGER_RIGHT_3, soundPool.load(this, R.raw.finger_right_3, 1));
        soundMap.put(FINGER_RIGHT_4, soundPool.load(this, R.raw.finger_right_4, 1));
        soundMap.put(FINGER_LEFT_0, soundPool.load(this, R.raw.finger_left_0, 1));
        soundMap.put(FINGER_LEFT_1, soundPool.load(this, R.raw.finger_left_1, 1));
        soundMap.put(FINGER_LEFT_2, soundPool.load(this, R.raw.finger_left_2, 1));
        soundMap.put(FINGER_LEFT_3, soundPool.load(this, R.raw.finger_left_3, 1));
        soundMap.put(FINGER_LEFT_4, soundPool.load(this, R.raw.finger_left_4, 1));
    }

    private void sendMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tip.setText(message);
            }
        });
    }

    /**
     * 二代证
     * */
    public void OnClickCardOpen(View view) {
        int re=idCardDriver.StartCard();
        if (re==0){
            sendMessage("打开成功");
        }else {
            sendMessage("打开失败，错误："+re);
        }
    }

    public void OnClickCardClose(View view) {
        int re=idCardDriver.CloseCard();
        if (re==0){
            sendMessage("关闭成功");
        }else {
            sendMessage("关闭失败，错误："+re);
        }
    }

    public void OnClickDevVersion(View view) {
        byte[] bDevVersion = new byte[64];
        int nRet = idCardDriver.CardVersion(bDevVersion);
        if (nRet != 0) {
            if (nRet == -100) {
                sendMessage("无设备");
            } else {
                sendMessage("失败:" + nRet);
            }
        } else {
            sendMessage("二代证模组版本信息：" + new String(bDevVersion));
        }
    }

    public void OnClickCardInfo(View view){
        byte[] bCardFullInfo = new byte[256 + 1024 + 1024];
        IDCardRecord idCardRecord=new IDCardRecord();
        Calendar time1, time2;
        String type = "";
        long bt_time;
        //idCardDriver.mxSetTraceLevel(1);
        time1 = Calendar.getInstance();
        int re = idCardDriver.mxReadCardFullInfo(bCardFullInfo);
        time2 = Calendar.getInstance();
        bt_time = time2.getTimeInMillis() - time1.getTimeInMillis();
        if (re == 1 || re == 0) {
            if ("I".equals(type)) {
                idCardRecord=IdGreenCardOarser(bCardFullInfo);
            } else if ("-1".equals(type)){
                Toast.makeText(this, "身份证号码读取失败", Toast.LENGTH_SHORT).show();
            } else {
                idCardRecord=IdCardParser(bCardFullInfo);
            }
            updateCardInfo(idCardRecord);
            sendMessage("");
        } else {
            Log.e("readCard:","=="+"失败"+"----re="+re);
            //            Toast.makeText(this, re+"读卡失败，重新读取", Toast.LENGTH_SHORT).show();
            sendMessage(re+"读卡失败，重新读取");
        }
    }

    public void OnClickCardFullInfo(View view){
        byte[] bCardFullInfo = new byte[256 + 1024 + 1024];
        IDCardRecord idCardRecord=new IDCardRecord();
        String type = null;
        try {
            int re = idCardDriver.mxReadCardFullInfo(bCardFullInfo);
            type = IdCardParser.isGreenCard(bCardFullInfo);
            if (re == 1 || re == 0) {
                if ("I".equals(type)) {
                    idCardRecord=IdGreenCardOarser(bCardFullInfo);
                } else if ("-1".equals(type)){
                    Toast.makeText(this, "身份证号码读取失败", Toast.LENGTH_SHORT).show();
                } else {
                    idCardRecord=IdCardParser(bCardFullInfo);
                }
                if (re == 0) {
                    byte[] bFingerData0 = new byte[FINGER_DATA_SIZE];
                    byte[] bFingerData1 = new byte[FINGER_DATA_SIZE];
                    int iLen = 256 + 1024;
                    try {
                        System.arraycopy(bCardFullInfo, iLen, bFingerData0, 0, bFingerData0.length);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    iLen += 512;
                    try {
                        System.arraycopy(bCardFullInfo, iLen, bFingerData1, 0, bFingerData1.length);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        idCardRecord.setFingerprintPosition0(fingerPositionCovert(bFingerData0[5]));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        idCardRecord.setFingerprint0(Base64.encodeToString(bFingerData0, Base64.NO_WRAP));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        idCardRecord.setFingerprintPosition1(fingerPositionCovert(bFingerData1[5]));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        idCardRecord.setFingerprint1(Base64.encodeToString(bFingerData1, Base64.NO_WRAP));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Log.e(TAG, "idCardRecord==" + idCardRecord);
                updateCardInfo(idCardRecord);
                idCard=idCardRecord;
                sendMessage("");
            } else {
                Log.e("readCard:","=="+"失败"+"----re="+re);
                //            Toast.makeText(this, re+"读卡失败，重新读取", Toast.LENGTH_SHORT).show();
                sendMessage(re+"读卡失败，重新读取");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 中国
     * */
    public IDCardRecord IdCardParser(byte[] bCardInfo){
        IDCardRecord idCardRecord = new IDCardRecord();
        idCardRecord.setName(IdCardParser.getName(bCardInfo));
        if(IdCardParser.getGender(bCardInfo).equals("1")){
            idCardRecord.setSex("男");
        }else {
            idCardRecord.setSex("女");
        }
        idCardRecord.setNation(TextUtils.isEmpty(IdCardParser.getNation(bCardInfo).trim())?"":IdCardParser.FOLK[Integer.parseInt(IdCardParser.getNation(bCardInfo))-1]);
        idCardRecord.setBirthday(IdCardParser.getBirthday(bCardInfo));
        idCardRecord.setAddress(IdCardParser.getAddress(bCardInfo));
        idCardRecord.setCardNumber(IdCardParser.getCardNum(bCardInfo));
        idCardRecord.setIssuingAuthority(IdCardParser.getIssuingAuthority(bCardInfo));
        idCardRecord.setValidateStart(IdCardParser.getStartTime(bCardInfo));
        idCardRecord.setValidateEnd(IdCardParser.getEndTime(bCardInfo));
        idCardRecord.setPassNumber(IdCardParser.getPassNum(bCardInfo));
        idCardRecord.setIssueCount(IdCardParser.getIssueNum(bCardInfo));
        idCardRecord.setCardType(getCardType(bCardInfo));
        idCardRecord.setCardBitmap(IdCardParser.getFaceBit(bCardInfo));
        return idCardRecord;
    }

    /**
     * 外国人
     * */
    public IDCardRecord IdGreenCardOarser(byte[] bCardInfo ){
        IDCardRecord idCardRecord = new IDCardRecord();
        idCardRecord.setName(IdCardParser.getEnglishName(bCardInfo));
        if(IdCardParser.getEnglishGender(bCardInfo).equals("1")){
            idCardRecord.setSex("男");
        }else {
            idCardRecord.setSex("女");
        }
        idCardRecord.setCardNumber(IdCardParser.getCardNum(bCardInfo));
        idCardRecord.setNation(IdCardParser.getNationality(bCardInfo));
        idCardRecord.setChineseName(IdCardParser.getChineseName(bCardInfo));
        idCardRecord.setValidateStart(IdCardParser.getStartTime(bCardInfo));
        idCardRecord.setValidateEnd(IdCardParser.getEndTime(bCardInfo));
        idCardRecord.setBirthday(IdCardParser.getEnglishBir(bCardInfo));
        idCardRecord.setVersion(IdCardParser.getVersion(bCardInfo));
        idCardRecord.setIssuingAuthority(IdCardParser.getAcceptMatter(bCardInfo));
        idCardRecord.setCardType(IdCardParser.getCardType(bCardInfo));
        idCardRecord.setCardBitmap(IdCardParser.getFaceBit(bCardInfo));
        return idCardRecord;
    }

    private String getCardType(byte[] idCardData){
        if(TextUtils.isEmpty(IdCardParser.getCardType(idCardData))){
            if(TextUtils.isEmpty(IdCardParser.getPassNum(idCardData))&&!TextUtils.isEmpty(IdCardParser.getNation(idCardData))){
                return "";
            }else {
                return "J";
            }
        }else {
            return IdCardParser.getCardType(idCardData);
        }
    }

    private void updateCardInfo(final IDCardRecord idCardRecord){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ("I".equals(idCardRecord.getCardType())){
                    cardType.setText("外国人永久居留证");
                }else if ("J".equals(idCardRecord.getCardType())){
                    cardType.setText("港澳台通行证");
                }else {
                    cardType.setText("二代证");
                }
                if ("I".equals(idCardRecord.getCardType())){
                    chineseName.setText(idCardRecord.getChineseName());
                    englishName.setText(idCardRecord.getName());
                }else {
                    chineseName.setText(idCardRecord.getName());
                    englishName.setText("");
                }
                cardImg.setImageBitmap(idCardRecord.getCardBitmap());
                sex.setText(idCardRecord.getSex());
                nation.setText(idCardRecord.getNation());
                borth.setText(idCardRecord.getBirthday());
                address.setText(idCardRecord.getAddress());
                number.setText(idCardRecord.getCardNumber());
                passnumber.setText(idCardRecord.getPassNumber());
                authority.setText(idCardRecord.getIssuingAuthority());
                authorityCount.setText(idCardRecord.getIssueCount());
                data.setText(idCardRecord.getValidateStart()+"——"+idCardRecord.getValidateEnd());
                finger1.setText(idCardRecord.getFingerprint0());
                finger2.setText(idCardRecord.getFingerprint1());
                cardVersion.setText(idCardRecord.getVersion());
            }
        });
    }

    /**
     * 扫码
     * */
    public void showScanMessage(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanTxt.setText(str);
            }
        });
    }

    public void  OnClickOpenScan(View view){
        if (idCardDriver==null){
            return;
        }
        int re=idCardDriver.OpenScan();
        if (re==0){
            sendMessage("扫码打开成功");
        }else {
            sendMessage("扫码打开失败，错误："+re);
        }
    }

    public void  OnClickStartScan(View view){
        if (scanThread!=null){
            scanThread.interrupt();
            scanThread=null;
        }
        scanThread=new ScanThread();
        scanFlag=true;
        scanThread.start();
        sendMessage("正在扫码");
    }

    public void  OnClickCancelScan(View view){
        if (scanThread!=null){
            scanThread.interrupt();
            scanThread=null;
            scanFlag=false;
        }
        showScanMessage("");
    }

    public void  OnClickCloseScan(View view){
        int re=idCardDriver.StopScan();
        if (re==0){
            sendMessage("扫码关闭成功");
        }else {
            sendMessage("扫码关闭失败，错误："+re);
        }
    }

    private class ScanThread extends Thread {

        @Override
        public void run() {
            while (scanFlag) {
                SystemClock.sleep(500);
                startScan();
            }
        }
    }

    private void startScan(){
        try {
            byte[] scan=new byte[512];
            idCardDriver.StartScan(scan);
            final String result=new String(scan);
            Log.e(TAG, "scan:==" + result);
            if (scanFlag){
                showScanMessage( result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 指纹
     * */
    public void FingerCompa(final boolean re){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (re){
                    camparRe.setText("比对成功");
                    camparRe.setBackgroundResource(R.drawable.verify_succ);
                    camparRe.setTextColor(Color.parseColor("#55B777"));
                }else {
                    camparRe.setText("比对失败");
                    camparRe.setBackgroundResource(R.drawable.verify_fail);
                    camparRe.setTextColor(Color.parseColor("#EA605A"));
                }
            }
        });
    }

    public void OnClickFingerImg(View view){
        if (m_GetImageThread != null) {
            m_GetImageThread.interrupt();
            m_GetImageThread = null;
        }
        m_GetImageThread =new GetImageThread();
        m_GetImageThread.start();
    }

    private class GetImageThread extends Thread {
        public void run() {
            try {
                GetImage();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void GetImage() {
        int ret = 0;
        Arrays.fill(bFingerImage, (byte) 0);
        ret = fingerAPI.mxCaptureFingerprint(bFingerImage, TIME_OUT, 0);
        if (ret == 0) {
            Log.e(TAG, "成功" );
            final Bitmap m_bitmap = BMP.Raw2Bimap(bFingerImage,IMAGE_X_BIG,IMAGE_Y_BIG);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    m_progressDlg.dismiss();
                    fingerImg.setImageBitmap(m_bitmap);
                    fingerFlag=false;
                }
            });
        } else {
            Log.e(TAG, "失败" );
        }
    }

    public void OnClickFingerCompar(View view){
        if (TextUtils.isEmpty(idCard.getName())){
            sendMessage("请先读卡");
            return;
        }
        if (TextUtils.isEmpty(idCard.getFingerprint0())||TextUtils.isEmpty(idCard.getFingerprint1())){
            sendMessage("无指纹");
            return;
        }
        startVerifyFinger(Base64.decode(idCard.getFingerprint0(), Base64.NO_WRAP),
                Base64.decode(idCard.getFingerprint1(), Base64.NO_WRAP));
    }

    public void OnClickVoice(View view){
        playSound(soundPos);
    }

    public void OnCLickFingerVer(View view){
        byte[] ver =new byte[30];
        int finerVer = idCardDriver.getFinerVer(ver);
        Log.e(TAG, "指纹版本：" +new String(ver) );
    }

    public void startVerifyFinger(byte[] fingerprint0, byte[] fingerprint1) {
        stopVerifyFinger();
        verifyRun = true;
        fingerImg.setImageResource(R.drawable.fingerimg);
        m_progressDlg.show();
        new VerifyCardFingerThread(fingerprint0, fingerprint1).start();
    }

    public void stopVerifyFinger() {
        verifyRun = false;
        if (fingerAPI != null) {
            fingerAPI.mxCancelCapture();
        }
    }

    private class VerifyCardFingerThread extends Thread {

        private byte[] fingerprint0;
        private byte[] fingerprint1;

        VerifyCardFingerThread(byte[] fingerprint0, byte[] fingerprint1) {
            this.fingerprint0 = fingerprint0;
            this.fingerprint1 = fingerprint1;
        }

        @Override
        public void run() {
            try {
                if (fingerAPI == null) {
                    fingerAPI = new MXFingerAPI(Main.this, pid, vid);
                }
                byte[] bImgBuf = new byte[IMAGE_SIZE_BIG];
                byte[] printFingerFeature = new byte[TZ_SIZE];
                int result = -1;
                //                while (result != 0 && verifyRun) {
                //                    result = idCardDriver.mxGetImage(bImgBuf, IMAGE_X_BIG*IMAGE_Y_BIG, TIME_OUT,0);
                //                }
                //                result = fingerAPI.mxExtractFeatureID(bImgBuf, printFingerFeature);
                result = fingerAPI.mxCaptureFingerprint(bFingerImage, TIME_OUT, 0);
                int mxGetTz512 = zzFingerAlgID.mxGetTz512(bFingerImage, printFingerFeature);
                result = mxGetTz512 == 1 ? 0 : mxGetTz512;
                if (result == 0 && verifyRun) {
                    final Bitmap bitmap = fingerAPI.Raw2Bimap(bImgBuf, IMAGE_X_BIG, IMAGE_Y_BIG);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            m_progressDlg.dismiss();
                            fingerImg.setImageBitmap(bitmap);
                        }
                    });
                    //                    int score0 = verifyFingerScore(fingerprint0, printFingerFeature);
                    //                    int score1 = verifyFingerScore(fingerprint1, printFingerFeature);
                    result = zzFingerAlgID.mxFingerMatch512(fingerprint0, printFingerFeature, 3);
                    if (result!=0){
                        result = zzFingerAlgID.mxFingerMatch512(fingerprint1, printFingerFeature, 3);
                    }
                    FingerCompa(result==0);
                    return;

                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }

    //    private int verifyFingerScore(byte[] alpha, byte[] beta) {
    //        int[] score = new int[] {0};
    //        int result = fingerAPI.mxMatchFeatureScoreID(alpha, beta, score);
    //        if (result == 0) {
    //            return score[0];
    //        } else {
    //            return 0;
    //        }
    //    }

    public void playSound(int soundID) {
        continuePlaySoundFlag = false;
        soundPool.stop(mCurSoundId);
        mCurSoundId = soundPool.play(soundMap.get(soundID), LEFT_VOLUME, RIGHT_VOLUME, PRIORITY, LOOP, SOUND_RATE);
    }

    public void stopPlay() {
        soundPool.stop(mCurSoundId);
    }

}
