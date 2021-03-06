package so.asch.sdk.security;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import so.asch.sdk.codec.Decoding;
import so.asch.sdk.codec.Encoding;
import so.asch.sdk.dbc.Argument;
import so.asch.sdk.impl.AschConst;
import so.asch.sdk.impl.AschSDKConfig;
import so.asch.sdk.impl.Validation;
import so.asch.sdk.security.ripemd.RipeMD160;
import so.asch.sdk.transaction.TransactionInfo;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by eagle on 17-7-18.
 */
public class DefaultSecurityStrategy implements SecurityStrategy{

    private static final String SHA256_DIGEST_ALGORITHM = "SHA-256";
    private static final int MAX_BUFFER_SIZE = 1024;
    private static  Date beginEpoch = new Date();

    private static AschSDKConfig config = AschSDKConfig.getInstance();

    private static MessageDigest sha256Digest;
    private static RipeMD160 ripemd160Digest;

    static
    {
        try {
            sha256Digest = MessageDigest.getInstance(SHA256_DIGEST_ALGORITHM);
            ripemd160Digest = new RipeMD160();

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            beginEpoch = df.parse("2016-06-27T20:00:00Z");
        }
        catch (Exception ex) {
            //
        }
    }



    @Override
    public KeyPair generateKeyPair(String secure) throws SecurityException {
        try {
            Argument.notNullOrEmpty(secure, "secure");
            byte[] hash = sha256Hash(Decoding.utf8(secure));
            return Ed25519.generateKeyPairBySeed(hash);
        }
        catch (Exception ex){
            throw new SecurityException("generate key pair failed", ex);
        }
    }

    @Override
    public String encodePublicKey(PublicKey publicKey)throws SecurityException {
        try {
            Argument.notNull(publicKey, "publicKey");

            return Encoding.hex(((EdDSAPublicKey) publicKey).getAbyte());
        }
        catch (Exception ex){
            throw new SecurityException("encode public key failed", ex);
        }
    }

    @Override
    public String getAddress(String publicKey) throws SecurityException{
        try{
            Argument.require(Validation.isValidPublicKey(publicKey), "invalid public key");

            byte[] hash1 = sha256Hash(Decoding.hex(publicKey));
            byte[] hash2 = ripemd160Hash(hash1);
            return AschConst.BASE58_ADDRESS_PREFIX + Encoding.base58(hash2);
        }
        catch (Exception ex){
            throw new SecurityException("generate key pair failed", ex);
        }
    }

    @Override
    public String generateTransactionId(TransactionInfo transaction)throws SecurityException {
        try {
            byte[] transactionBytes = transaction.getBytes(false, false);
            byte[] hash = sha256Hash(transactionBytes);

            if (config.isLongTransactionIdEnabled())
                return Encoding.hex(hash);

            byte[] transactionIdBytes = new byte[8];
            for (int i = 0; i < 7; i++) {
                transactionIdBytes[i] = hash[7 - 1];
            }

            return new BigInteger(transactionIdBytes).toString();

        } catch (Exception ex) {
            throw new SecurityException("generate transaction id failed", ex);
        }
    }

    @Override
    public String Signature(TransactionInfo transaction, PrivateKey privateKey) throws SecurityException {
        try {
            Argument.require(transaction != null, "transaction can not be null");
            Argument.require(privateKey != null, "private key can not be null");

            byte[] transactionBytes = transaction.getBytes(true, true);
            byte[] hash = sha256Hash(transactionBytes);
            return Encoding.hex(Ed25519.signature(hash, privateKey));
        }
        catch (Exception ex){
            throw new SecurityException("setSignature transaction failed", ex);
        }
    }

    @Override
    public String SignSignature(TransactionInfo transaction, PrivateKey privateKey) throws SecurityException {
        try {
            Argument.require(transaction != null, "transaction can not be null");
            Argument.require(privateKey != null, "private key can not be null");

            byte[] transactionBytes = transaction.getBytes( false, true);
            byte[] hash = sha256Hash(transactionBytes);
            return Encoding.hex(Ed25519.signature(hash, privateKey));
        }
        catch (Exception ex){
            throw new SecurityException("setSignature transaction failed", ex);
        }
    }

    @Override
    public int getTimestamp() {
        //calendar.add(Calendar.MILLISECOND, (zoneOffset+dstOffset));
        return (int)((new Date().getTime() - beginEpoch.getTime())/1000 - AschConst.CLIENT_DRIFT_SECONDS);
    }

    private byte[] sha256Hash(byte[] message){
        sha256Digest.update(message);
        return sha256Digest.digest();
    }

    private byte[] ripemd160Hash(byte[] message){
        ripemd160Digest.update(message);
        return ripemd160Digest.digest();
    }



}
