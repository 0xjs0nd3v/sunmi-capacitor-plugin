package com.veridata.plugins.sunmi.utils;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** NIST Special Publication 800-38B — Recommendation for Block Cipher Modes ofOperation: The CMAC Mode for Authentication */
public final class CMacUtil {

    /** CBC calculate mac */
    public static byte[] calcMac(byte[] key, byte[] iv, byte[] data) {
        try {
            int nBlocks = data.length / 16;
            int lastBlen = data.length % 16;
            byte[] lastState = new byte[16];
            byte[] lastBData = new byte[16];
            boolean padding = false;

            if (lastBlen > 0) {
                padding = true;
                nBlocks++;
            }

            if (nBlocks > 1) {
                byte[] cbcdata = Arrays.copyOf(data, (nBlocks - 1) * 16);
                //CBC
                SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
                IvParameterSpec ips = new IvParameterSpec(iv);
                Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey, ips);
                byte[] cbcCt = cipher.doFinal(cbcdata);
                //Get cbc last state
                lastState = Arrays.copyOfRange(cbcCt, (nBlocks - 2) * 16, (nBlocks - 1) * 16);
                lastBData = Arrays.copyOfRange(data, (nBlocks - 1) * 16, (nBlocks) * 16);
            } else {
                if (data.length == 0) {
                    padding = true;
                } else {
                    lastBData = Arrays.copyOfRange(data, (nBlocks - 1) * 16, (nBlocks) * 16);
                }
            }
            //Add Padding if needed
            if (lastBlen != 0 || padding) {
                lastBData[lastBlen] = (byte) 0x80;
            }

            byte[][] KS = generateSubKey(key);
            byte[] K1 = KS[0];
            byte[] K2 = KS[1];

            if (padding) {
                for (int i = 0; i < 16; i++) {
                    lastBData[i] = (byte) (lastBData[i] ^ K2[i]);
                }
            } else {
                for (int i = 0; i < 16; i++) {
                    lastBData[i] = (byte) (lastBData[i] ^ K1[i]);
                }
            }
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ips = new IvParameterSpec(lastState);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ips);
            return cipher.doFinal(lastBData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static byte[][] generateSubKey(byte[] key) {
        byte[] L = new byte[16];
        byte[] Z = new byte[16];
        byte[] iv = new byte[16];
        byte[] constRb = new byte[16];
        constRb[15] = (byte) 0x87;
        try {
            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ips = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ips);
            L = cipher.doFinal(Z);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] K1 = null, K2 = null;
        if ((L[0] & 0x80) == 0) { //If MSB(L) = 0, then K1 = L << 1
            K1 = leftShiftOneBit(L);
        } else { //Else K1 = ( L << 1 ) (+) Rb
            byte[] temp = leftShiftOneBit(L);
            K1 = xor(temp, constRb);
        }
        if ((K1[0] & 0x80) == 0) {
            K2 = leftShiftOneBit(K1);
        } else {
            byte[] temp = leftShiftOneBit(K1);
            K2 = xor(temp, constRb);
        }
        return new byte[][]{K1, K2};
    }

    private static byte[] leftShiftOneBit(byte[] input) {
        byte[] result = new byte[input.length];
        byte overflow = 0;
        for (int i = input.length - 1; i >= 0; i--) {
            result[i] = (byte) (input[i] << 1);
            result[i] |= overflow;
            if ((input[i] & 0x80) > 0) {
                overflow = 1;
            } else {
                overflow = 0;
            }
        }
        return result;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
}
