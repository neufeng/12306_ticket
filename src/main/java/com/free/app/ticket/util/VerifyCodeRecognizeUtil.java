package com.free.app.ticket.util;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class VerifyCodeRecognizeUtil {
    
    public static String RecognizeImage(File file) {
        Tesseract instance = Tesseract.getInstance();
        
        String result = null;
        try {
            result = instance.doOCR(file);
        }
        catch (TesseractException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return result;
    }
    
    public static void main(String[] arg0) {
        String curDir = System.getProperty("user.dir");
        System.out.println(curDir);
        File file = new File(curDir + File.separator + "passcode" + File.separator + "1421721817759.login.png");
        String result = RecognizeImage(file);
        System.out.println(result);
    }
    
}
