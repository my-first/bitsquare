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

package io.bisq.core.trade.protocol.tasks.seller;

import com.google.common.base.Preconditions;
import io.bisq.common.taskrunner.TaskRunner;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.trade.Trade;
import io.bisq.core.trade.protocol.tasks.TradeTask;
import org.bitcoinj.core.Coin;
import org.bitcoinj.crypto.DeterministicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public class SignPayoutTx extends TradeTask {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SignPayoutTx.class);

    @SuppressWarnings({"WeakerAccess", "unused"})
    public SignPayoutTx(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            Preconditions.checkNotNull(trade.getTradeAmount(), "trade.getTradeAmount() must not be null");
            Preconditions.checkNotNull(trade.getDepositTx(), "trade.getDepositTx() must not be null");
            Coin sellerPayoutAmount = trade.getOffer().getSellerSecurityDeposit();
            Coin buyerPayoutAmount = trade.getOffer().getBuyerSecurityDeposit()
                    .add(trade.getTradeAmount());

            BtcWalletService walletService = processModel.getWalletService();
            String id = processModel.getOffer().getId();
            String sellerPayoutAddressString = walletService.getOrCreateAddressEntry(id, AddressEntry.Context.TRADE_PAYOUT).getAddressString();
            DeterministicKey multiSigKeyPair = walletService.getMultiSigKeyPair(id, processModel.getMyMultiSigPubKey());
            byte[] sellerMultiSigPubKey = processModel.getMyMultiSigPubKey();
            checkArgument(Arrays.equals(sellerMultiSigPubKey,
                            walletService.getOrCreateAddressEntry(id, AddressEntry.Context.MULTI_SIG).getPubKey()),
                    "sellerMultiSigPubKey from AddressEntry must match the one from the trade data. trade id =" + id);

            byte[] payoutTxSignature = processModel.getTradeWalletService().sellerSignsPayoutTx(
                    trade.getDepositTx(),
                    buyerPayoutAmount,
                    sellerPayoutAmount,
                    processModel.tradingPeer.getPayoutAddressString(),
                    sellerPayoutAddressString,
                    multiSigKeyPair,
                    processModel.tradingPeer.getMultiSigPubKey(),
                    sellerMultiSigPubKey,
                    trade.getArbitratorPubKey());

            processModel.setPayoutTxSignature(payoutTxSignature);

            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}

