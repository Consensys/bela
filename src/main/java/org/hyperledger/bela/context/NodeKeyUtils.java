package org.hyperledger.bela.context;

import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.cryptoservices.KeyPairSecurityModule;
import org.hyperledger.besu.cryptoservices.NodeKey;

class NodeKeyUtils {

    public static NodeKey createFrom(final KeyPair keyPair) {
        return new NodeKey(new KeyPairSecurityModule(keyPair));
    }

    public static NodeKey createFrom(final Bytes32 privateKey) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithmFactory.getInstance();
        final KeyPair keyPair =
                signatureAlgorithm.createKeyPair(signatureAlgorithm.createPrivateKey(privateKey));
        return new NodeKey(new KeyPairSecurityModule(keyPair));
    }

    public static NodeKey generate() {
        return new NodeKey(
                new KeyPairSecurityModule(SignatureAlgorithmFactory.getInstance().generateKeyPair()));
    }
}