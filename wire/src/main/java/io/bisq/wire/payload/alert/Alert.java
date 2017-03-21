/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.wire.payload.alert;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import io.bisq.common.app.Version;
import io.bisq.common.crypto.Sig;
import io.bisq.wire.payload.StoragePayload;
import io.bisq.wire.proto.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode
@Slf4j
public final class Alert implements StoragePayload {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.P2P_NETWORK_VERSION;
    private static final long TTL = TimeUnit.DAYS.toMillis(30);

    // Payload
    public final String message;
    public final String version;
    public final boolean isUpdateInfo;
    @Getter
    private String signatureAsBase64;
    private byte[] storagePublicKeyBytes;
    // Should be only used in emergency case if we need to add data but do not want to break backward compatibility 
    // at the P2P network storage checks. The hash of the object will be used to verify if the data is valid. Any new 
    // field in a class would break that hash and therefore break the storage mechanism.
    @Getter
    @Nullable
    private Map<String, String> extraDataMap;

    // Domain
    private transient PublicKey storagePublicKey;

    // Called from domain
    public Alert(String message,
                 boolean isUpdateInfo,
                 String version) {
        this.message = message;
        this.isUpdateInfo = isUpdateInfo;
        this.version = version;
    }

    // Called from PB
    public Alert(String message,
                 boolean isUpdateInfo,
                 String version,
                 byte[] storagePublicKeyBytes,
                 String signatureAsBase64,
                 @Nullable Map<String, String> extraDataMap) {
        this(message, isUpdateInfo, version);
        this.storagePublicKeyBytes = storagePublicKeyBytes;
        this.signatureAsBase64 = signatureAsBase64;
        this.extraDataMap = extraDataMap;

        init();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            in.defaultReadObject();
            init();
        } catch (Throwable t) {
            log.warn("Exception at readObject: " + t.getMessage());
        }
    }

    private void init() {
        try {
            storagePublicKey = KeyFactory.getInstance(Sig.KEY_ALGO, "BC")
                    .generatePublic(new X509EncodedKeySpec(storagePublicKeyBytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("Couldn't create the storage public key", e);
        }
    }

    public void setSigAndPubKey(String signatureAsBase64, PublicKey storagePublicKey) {
        this.signatureAsBase64 = signatureAsBase64;
        this.storagePublicKey = storagePublicKey;
        this.storagePublicKeyBytes = new X509EncodedKeySpec(this.storagePublicKey.getEncoded()).getEncoded();
    }

    public String getSignatureAsBase64() {
        return signatureAsBase64;
    }

    public boolean isNewVersion() {
        return isNewVersion(Version.VERSION);
    }

    @VisibleForTesting
    protected boolean isNewVersion(String appVersion) {
        // Usually we use 3 digits (0.4.8) but to support also 4 digits in case of hotfixes (0.4.8.1) we 
        // add a 0 at all 3 digit versions to allow correct comparison: 0.4.8 -> 480; 0.4.8.1 -> 481; 481 > 480
        String myVersionString = appVersion.replace(".", "");
        if (myVersionString.length() == 3)
            myVersionString += "0";
        int versionNum = Integer.valueOf(myVersionString);

        String alertVersionString = version.replace(".", "");
        if (alertVersionString.length() == 3)
            alertVersionString += "0";
        int alertVersionNum = Integer.valueOf(alertVersionString);
        return versionNum < alertVersionNum;
    }

    @Override
    public long getTTL() {
        return TTL;
    }

    @Override
    public PublicKey getOwnerPubKey() {
        return storagePublicKey;
    }


    @Override
    public Messages.StoragePayload toProtoBuf() {
        final Messages.Alert.Builder builder = Messages.Alert.newBuilder()
                .setTTL(TTL)
                .setMessage(message)
                .setVersion(version)
                .setIsUpdateInfo(isUpdateInfo)
                .setSignatureAsBase64(signatureAsBase64)
                .setStoragePublicKeyBytes(ByteString.copyFrom(storagePublicKeyBytes));
        Optional.ofNullable(extraDataMap).ifPresent(builder::putAllExtraDataMap);
        return Messages.StoragePayload.newBuilder().setAlert(builder).build();
    }

    @Override
    public String toString() {
        return "Alert{" +
                "message='" + message + '\'' +
                ", version='" + version + '\'' +
                ", isUpdateInfo=" + isUpdateInfo +
                ", signatureAsBase64='" + signatureAsBase64 + '\'' +
                ", storagePublicKeyBytes=" + Hex.toHexString(storagePublicKeyBytes) +
                ", storagePublicKey=" + Hex.toHexString(storagePublicKey.getEncoded()) +
                ", extraDataMap=" + extraDataMap +
                '}';
    }
}
