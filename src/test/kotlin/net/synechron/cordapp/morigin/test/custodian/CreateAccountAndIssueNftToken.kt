package net.synechron.cordapp.morigin.test.custodian

import net.synechron.cordapp.morigin.test.AbstractFlowUnitTests
import org.junit.Test

class CreateAccountAndIssueNftToken : AbstractFlowUnitTests() {
    @Test
    fun createAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)

        val acc2 = "BankCreditAdminDept1"
        bankNode.createAccount(acc2)
        //Find account else throw error.
        bankNode.services.accountByName(acc2)
    }
}