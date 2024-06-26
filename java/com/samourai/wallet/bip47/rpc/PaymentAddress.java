package com.samourai.wallet.bip47.rpc;

import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPoint;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class PaymentAddress {

    private static ISecretPointFactory secretPointFactory;
    private PaymentCode paymentCode = null;
    private int index = 0;
    private byte[] privKey = null;
    private NetworkParameters params;

    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    private PaymentAddress()    { ; }

    public PaymentAddress(PaymentCode paymentCode, int index, byte[] privKey, NetworkParameters params, ISecretPointFactory secretPointFactory) throws AddressFormatException {
        this.paymentCode = paymentCode;
        this.index = index;
        this.privKey = privKey;
        this.params = params;
        if (PaymentAddress.secretPointFactory == null) {
            PaymentAddress.secretPointFactory = secretPointFactory;
        }
    }

    public ECKey getSendECKey() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException, NotSecp256k1Exception {
        return getSendECKey(getSecretPoint());
    }

    public ECKey getReceiveECKey() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException, NotSecp256k1Exception {
        return getReceiveECKey(getSecretPoint());
    }

    public ISecretPoint getSharedSecret() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException   {
        return sharedSecret();
    }

    public BigInteger getSecretPoint() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException, NotSecp256k1Exception    {
        return secretPoint();
    }

    public ECPoint getECPoint() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException    {
        ECKey ecKey = ECKey.fromPublicOnly(paymentCode.addressAt(index, params).getPubKey());
        return ecKey.getPubKeyPoint();
    }

    public byte[] hashSharedSecret() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(getSharedSecret().ECDHSecretAsBytes());
        return hash;
    }

    public SegwitAddress getSegwitAddressSend() throws Exception {
        SegwitAddress segwitAddress = new SegwitAddress(getSendECKey().getPubKey(), params);
        return segwitAddress;
    }

    public SegwitAddress getSegwitAddressReceive() throws Exception {
        SegwitAddress segwitAddress = new SegwitAddress(getReceiveECKey(), params);
        return segwitAddress;
    }

    public SegwitAddress getSegwitAddress(boolean send) throws Exception {
        return send ? getSegwitAddressSend() : getSegwitAddressReceive();
    }

    private ECPoint get_sG(BigInteger s) {
        return CURVE_PARAMS.getG().multiply(s);
    }

    private ECKey getSendECKey(BigInteger s) throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IllegalStateException, InvalidKeySpecException{
        ECPoint ecPoint = getECPoint();
        ECPoint sG = get_sG(s);
        ECKey ecKey = ECKey.fromPublicOnly(ecPoint.add(sG).getEncoded(true));
        return ecKey;
    }

    private ECKey getReceiveECKey(BigInteger s)  {
        BigInteger privKeyValue = ECKey.fromPrivate(privKey).getPrivKey();
        ECKey ecKey = ECKey.fromPrivate(addSecp256k1(privKeyValue, s));
        return ecKey;
    }

    private BigInteger addSecp256k1(BigInteger b1, BigInteger b2) {

        BigInteger ret = b1.add(b2);

        if(ret.compareTo(CURVE.getN()) > 0) {
            return ret.mod(CURVE.getN());
        }

        return ret;
    }

    private ISecretPoint sharedSecret() throws AddressFormatException, InvalidKeySpecException, InvalidKeyException, IllegalStateException, NoSuchAlgorithmException, NoSuchProviderException {
        return secretPointFactory.newSecretPoint(privKey, paymentCode.addressAt(index, params).getPubKey());
    }

    private boolean isSecp256k1(BigInteger b) {

        if(b.compareTo(BigInteger.ONE) > 0 && b.compareTo(CURVE.getN()) < 0) {
            return true;
        }

        return false;
    }

    private BigInteger secretPoint() throws AddressFormatException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, NotSecp256k1Exception  {
        //
        // convert hash to value 's'
        //
        BigInteger s = new BigInteger(1, hashSharedSecret());
        //
        // check that 's' is on the secp256k1 curve
        //
        if(!isSecp256k1(s))    {
            throw new NotSecp256k1Exception("secret point not on Secp256k1 curve");
        }

        return s;
    }

}
