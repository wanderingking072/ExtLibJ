package com.samourai.wallet.segwit;

import com.samourai.wallet.segwit.bech32.Bech32Segwit;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

public class SegwitAddress {

    public static final int TYPE_P2SH_P2WPKH = 0;
    public static final int TYPE_P2WPKH = 1;
    public static final int TYPE_P2WSH = 2;
    public static final int TYPE_P2TR = 3;

    protected int DEFAULT_TO = TYPE_P2SH_P2WPKH;

    protected ECKey ecKey = null;
    protected NetworkParameters params = null;

    private SegwitAddress()   { ; }

    public SegwitAddress(NetworkParameters params) {
        this.params = params;
    }

    public SegwitAddress(ECKey ecKey, NetworkParameters params) {
        this.ecKey = ecKey;
        this.params = params;
    }

    //
    // use only compressed public keys for SegWit
    //
    public SegwitAddress(byte[] pubkey, NetworkParameters params) {
        this.ecKey = ECKey.fromPublicOnly(pubkey);
        this.params = params;
    }

    public SegwitAddress(ECKey ecKey, NetworkParameters params, int type) {
        this.ecKey = ecKey;
        this.params = params;
        this.DEFAULT_TO = type;
    }

    public SegwitAddress(byte[] pubkey, NetworkParameters params, int type) {
        this.ecKey = ECKey.fromPublicOnly(pubkey);
        this.params = params;
        this.DEFAULT_TO = type;
    }

    public int getDefaultTo() {
        return DEFAULT_TO;
    }

    public void setDefaultTo(int type) {
        this.DEFAULT_TO = type;
    }

    public ECKey getECKey() {
        return ecKey;
    }

    public void setECKey(ECKey ecKey) {
        this.ecKey = ecKey;
    }

    public Address getAddress()    {

        try {
          return Address.fromP2SHScript(params, segwitOutputScript());
        }
        catch(java.security.NoSuchAlgorithmException nsae) {
            return null;
        }

    }

    public String getAddressAsString()    {

      return getAddress().toString();

    }

    public String getBech32AsString()    {

        String address = null;

        try {
            address = Bech32Segwit.encode(params instanceof TestNet3Params ? "tb" : "bc", (byte)0x00, Utils.sha256hash160(ecKey.getPubKey()));
        }
        catch(Exception e) {
            ;
        }

        return address;
    }

    public String getDefaultToAddressAsString()  {

        if(DEFAULT_TO == TYPE_P2SH_P2WPKH)    {
            return getAddressAsString();
        }
        else    {
            return getBech32AsString();
        }

    }

    public Script segwitOutputScript() throws java.security.NoSuchAlgorithmException  {

        //
        // OP_HASH160 hash160(redeemScript) OP_EQUAL
        //
        byte[] hash = Utils.sha256hash160(this.segwitRedeemScript().getProgram());
        byte[] buf = new byte[3 + hash.length];
        buf[0] = (byte)0xa9;    // HASH160
        buf[1] = (byte)0x14;    // push 20 bytes
        System.arraycopy(hash, 0, buf, 2, hash.length); // keyhash
        buf[22] = (byte)0x87;   // OP_EQUAL

        return new Script(buf);
    }

    public Script segwitRedeemScript()    {

        //
        // The P2SH segwit redeemScript is always 22 bytes. It starts with a OP_0, followed by a canonical push of the keyhash (i.e. 0x0014{20-byte keyhash})
        //
        byte[] hash = Utils.sha256hash160(ecKey.getPubKey());
        byte[] buf = new byte[2 + hash.length];
        buf[0] = (byte)0x00;  // OP_0
        buf[1] = (byte)0x14;  // push 20 bytes
        System.arraycopy(hash, 0, buf, 2, hash.length); // keyhash

        return new Script(buf);
    }

    public String segwitRedeemScriptToString() {
        return Hex.toHexString(segwitRedeemScript().getProgram());
    }

}
