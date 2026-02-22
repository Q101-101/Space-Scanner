package bg.sofia.uni.fmi.mjt.space.algorithm;

import bg.sofia.uni.fmi.mjt.space.exception.CipherException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;

public class Rijndael implements SymmetricBlockCipher {

    private static final int BUFFER_SIZE = 1024;
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private SecretKey secretKey;

    public Rijndael(SecretKey secretKey) {
        if (secretKey == null) {
            throw new IllegalArgumentException("secretKey cannot be null");
        }
        this.secretKey = secretKey;
    }

    @Override
    public void encrypt(InputStream inputStream, OutputStream outputStream) throws CipherException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
                if (encryptedChunk != null) {
                    outputStream.write(encryptedChunk);
                }
            }

            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                outputStream.write(finalBytes);
            }
        } catch (Exception e) {
            throw new CipherException("Encryption failed", e);
        }
    }

    @Override
    public void decrypt(InputStream inputStream, OutputStream outputStream) throws CipherException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] decryptedChunk = cipher.update(buffer, 0, bytesRead);
                if (decryptedChunk != null) {
                    outputStream.write(decryptedChunk);
                }
            }

            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                outputStream.write(finalBytes);
            }
        } catch (Exception e) {
            throw new CipherException("Decryption failed", e);
        }
    }

}
