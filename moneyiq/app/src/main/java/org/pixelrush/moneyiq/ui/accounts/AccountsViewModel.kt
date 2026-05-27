package org.pixelrush.moneyiq.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Double = 0.0
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repo: AccountRepository
) : ViewModel() {

    val state: StateFlow<AccountsUiState> = combine(
        repo.getAllAccounts(),
        repo.getTotalBalance().map { it ?: 0.0 }
    ) { accounts, total ->
        AccountsUiState(accounts = accounts, totalBalance = total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

    fun add(
        name: String,
        type: AccountType,
        balance: Double,
        color: String,
        currency: String = "UAH",
        description: String = "",
        includeInTotal: Boolean = true
    ) {
        viewModelScope.launch {
            val isFirst = repo.getAllAccounts().first().isEmpty()
            repo.save(
                AccountEntity(
                    name = name, type = type, balance = balance,
                    colorHex = color, currency = currency,
                    isDefault = isFirst,
                    description = description,
                    includeInTotal = includeInTotal
                )
            )
        }
    }

    fun update(account: AccountEntity) {
        viewModelScope.launch { repo.update(account) }
    }

    fun delete(account: AccountEntity) {
        viewModelScope.launch { repo.delete(account) }
    }

    fun setDefault(account: AccountEntity) {
        viewModelScope.launch { repo.setDefault(account.id) }
    }
}
