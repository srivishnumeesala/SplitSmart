package splitsmart.logic;

import splitsmart.model.Member;
import splitsmart.model.Transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates an optimal debt settlement plan with the minimum number of transfers.
 * Uses a greedy matching algorithm: repeatedly pairs the largest creditor (receiver)
 * with the largest debtor (payer) until all balances reach zero.
 */
public class SettlementPlanner {

    private static class ParticipantNode {
        final Member member;
        long amountPaise;

        ParticipantNode(Member member, long amountPaise) {
            this.member = member;
            this.amountPaise = amountPaise;
        }
    }

    private SettlementPlanner() {
        // Utility class
    }

    /**
     * Computes the list of suggested transfers to settle all group debts.
     * Takes the list of member balance summaries and produces at most (N - 1) transfers.
     */
    public static List<Transfer> planSettlements(List<BalanceCalculator.MemberBalance> balances) {
        List<Transfer> transfers = new ArrayList<>();
        if (balances == null || balances.isEmpty()) {
            return transfers;
        }

        List<ParticipantNode> receivers = new ArrayList<>();
        List<ParticipantNode> payers = new ArrayList<>();

        // Segregate into receivers (net > 0) and payers (net < 0)
        for (int i = 0; i < balances.size(); i++) {
            BalanceCalculator.MemberBalance b = balances.get(i);
            if (b.getNetPaise() > 0) {
                receivers.add(new ParticipantNode(b.getMember(), b.getNetPaise()));
            } else if (b.getNetPaise() < 0) {
                payers.add(new ParticipantNode(b.getMember(), -b.getNetPaise()));
            }
        }

        // Greedy matching loop
        while (!receivers.isEmpty() && !payers.isEmpty()) {
            // Find receiver with largest positive balance
            int maxReceiverIdx = 0;
            for (int i = 1; i < receivers.size(); i++) {
                if (receivers.get(i).amountPaise > receivers.get(maxReceiverIdx).amountPaise) {
                    maxReceiverIdx = i;
                }
            }

            // Find payer with largest debt
            int maxPayerIdx = 0;
            for (int i = 1; i < payers.size(); i++) {
                if (payers.get(i).amountPaise > payers.get(maxPayerIdx).amountPaise) {
                    maxPayerIdx = i;
                }
            }

            ParticipantNode receiver = receivers.get(maxReceiverIdx);
            ParticipantNode payer = payers.get(maxPayerIdx);

            long transferAmount = Math.min(receiver.amountPaise, payer.amountPaise);
            if (transferAmount > 0) {
                transfers.add(new Transfer(payer.member, receiver.member, transferAmount));
            }

            receiver.amountPaise -= transferAmount;
            payer.amountPaise -= transferAmount;

            // Remove participants who have fully settled
            if (receiver.amountPaise == 0) {
                receivers.remove(maxReceiverIdx);
            }
            if (payer.amountPaise == 0) {
                payers.remove(maxPayerIdx);
            }
        }

        return transfers;
    }
}
