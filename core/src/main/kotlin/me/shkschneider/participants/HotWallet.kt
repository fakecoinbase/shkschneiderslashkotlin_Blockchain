package me.shkschneider.participants

import me.shkschneider.blockchain.Chain
import me.shkschneider.blockchain.TransactionOutput
import me.shkschneider.consensus.BlockchainException
import me.shkschneider.consensus.validate
import me.shkschneider.crypto.KeyPair
import me.shkschneider.data.Address
import me.shkschneider.data.Coin
import me.shkschneider.data.toCoin
import me.shkschneider.stringOf

class HotWallet(
    private val chain: Chain,
    keyPair: KeyPair
) : ColdWallet(keyPair) {

    fun balance(): Coin =
        chain.utxos.filter { it.to == address() }.toCoin()

    fun send(to: Address, amount: Coin, fees: Coin = Coin(sat = 1)) {
        if (balance() < amount + fees) throw BlockchainException.WalletException("balance")
        val inputs = mutableListOf<TransactionOutput>()
        chain.utxos.filter { it.to == address() }.sortedBy { it.amount }.forEach { utxo ->
            inputs += utxo
            if (inputs.toCoin() >= amount + fees) {
                return@forEach
            }
        }
        val tx = outgoing(inputs, to, amount, fees)
        tx.validate()
        chain.add(tx)
    }

    fun flush(to: Address, fees: Coin = Coin(sat = 1)) {
        if (balance() < fees) throw BlockchainException.WalletException("balance")
        val tx = outgoing(chain.utxos.filter { it.to == address() }, to, balance(), fees)
        tx.validate()
        chain.add(tx)
    }

    override fun toString(): String = "HotWallet {" + stringOf(
        " address=${address()}",
        " balance=[bit=${balance().bit},sat=${balance().sat}]"
    ) + " }"

}
