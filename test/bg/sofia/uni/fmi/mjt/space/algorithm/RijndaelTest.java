package bg.sofia.uni.fmi.mjt.space.algorithm;

import bg.sofia.uni.fmi.mjt.space.exception.CipherException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RijndaelTest {

    private static final SecureRandom RNG = new SecureRandom();

    private SecretKey secretKey;
    private SymmetricBlockCipher cipher;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        secretKey = generateAesKey(128);
        cipher = new Rijndael(secretKey);
    }

    @Test
    void testConstructor_whenSecretKeyIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Rijndael(null),
                "Constructor should throw IllegalArgumentException when secretKey is null"
        );
    }

    @Test
    void testEncryptDecrypt_whenPlainTextProvided() throws CipherException {
        byte[] plaintext = "Hello AES Rijndael".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = encryptToBytes(cipher, plaintext);
        byte[] decrypted = decryptToBytes(cipher, ciphertext);

        assertArrayEquals(
                plaintext,
                decrypted,
                "Decrypted plaintext should be equal to the original plaintext"
        );
    }

    @Test
    void testEncryptDecrypt_whenRandomBytesProvided() throws CipherException {
        byte[] plaintext = randomBytes(10_000);

        byte[] ciphertext = encryptToBytes(cipher, plaintext);
        byte[] decrypted = decryptToBytes(cipher, ciphertext);

        assertArrayEquals(
                plaintext,
                decrypted,
                "Decrypted data should be identical to the original random byte array"
        );
    }

    @Test
    void testEncryptDecrypt_whenInputIsEmpty() throws CipherException {
        byte[] plaintext = new byte[0];

        byte[] ciphertext = encryptToBytes(cipher, plaintext);
        byte[] decrypted = decryptToBytes(cipher, ciphertext);

        assertArrayEquals(
                plaintext,
                decrypted,
                "Encrypting and decrypting empty input should result in empty output"
        );
    }

    @Test
    void testEncrypt_whenInputStreamThrowsIOException() throws Exception {
        InputStream failingIn = mock(InputStream.class);
        when(failingIn.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("boom"));

        OutputStream out = new ByteArrayOutputStream();

        assertThrows(
                CipherException.class,
                () -> cipher.encrypt(failingIn, out),
                "Encrypt should throw CipherException when input stream throws IOException"
        );
    }

    @Test
    void testEncrypt_whenOutputStreamThrowsIOException() throws Exception {
        InputStream in = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        OutputStream failingOut = mock(OutputStream.class);
        doThrow(new IOException("boom"))
                .when(failingOut)
                .write(any(byte[].class), anyInt(), anyInt());

        assertThrows(
                CipherException.class,
                () -> cipher.encrypt(in, failingOut),
                "Encrypt should throw CipherException when output stream throws IOException"
        );
    }

    @Test
    void testDecrypt_whenCiphertextIsInvalid() {
        byte[] invalidCiphertext = "not-a-valid-ciphertext".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                CipherException.class,
                () -> decryptToBytes(cipher, invalidCiphertext),
                "Decrypt should throw CipherException when ciphertext is invalid"
        );
    }

    @Test
    void testDecrypt_whenInputStreamThrowsIOException() throws Exception {
        InputStream failingIn = mock(InputStream.class);
        when(failingIn.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("boom"));

        OutputStream out = new ByteArrayOutputStream();

        assertThrows(
                CipherException.class,
                () -> cipher.decrypt(failingIn, out),
                "Decrypt should throw CipherException when input stream throws IOException"
        );
    }

    @Test
    void testDecrypt_whenOutputStreamThrowsIOException() throws Exception {
        byte[] ciphertext = encryptToBytes(cipher, "data".getBytes(StandardCharsets.UTF_8));
        InputStream in = new ByteArrayInputStream(ciphertext);

        OutputStream failingOut = mock(OutputStream.class);
        doThrow(new IOException("boom"))
                .when(failingOut)
                .write(any(byte[].class), anyInt(), anyInt());

        assertThrows(
                CipherException.class,
                () -> cipher.decrypt(in, failingOut),
                "Decrypt should throw CipherException when output stream throws IOException"
        );
    }

    // ---------- helpers ----------

    private static SecretKey generateAesKey(int bits) throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits);
        return kg.generateKey();
    }

    private static byte[] randomBytes(int size) {
        byte[] b = new byte[size];
        RNG.nextBytes(b);
        return b;
    }

    private static byte[] encryptToBytes(SymmetricBlockCipher cipher, byte[] plaintext)
            throws CipherException {
        try (InputStream in = new ByteArrayInputStream(plaintext);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            cipher.encrypt(in, out);
            return out.toByteArray();

        } catch (IOException e) {
            fail("Unexpected IOException from in-memory streams in test helper method encryptToBytes");
            return null;
        }
    }

    private static byte[] decryptToBytes(SymmetricBlockCipher cipher, byte[] ciphertext)
            throws CipherException {
        try (InputStream in = new ByteArrayInputStream(ciphertext);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            cipher.decrypt(in, out);
            return out.toByteArray();

        } catch (IOException e) {
            fail("Unexpected IOException from in-memory streams in test helper method decryptToBytes");
            return null;
        }
    }
}
