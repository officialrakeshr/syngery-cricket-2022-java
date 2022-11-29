package com.aztechsynergy.crickScore.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MdGen {
    public static String fromString(String input){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
