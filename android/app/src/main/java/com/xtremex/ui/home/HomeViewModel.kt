package com.xtremex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xtremex.data.Product
import com.xtremex.data.ShopifyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val newArrivals: List<Product> = emptyList(),
    val bestSellers: List<Product> = emptyList(),
    val error: String? = null,
    val wishlistIds: Set<String> = emptySet()
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val (arrivals, sellers) = withContext(Dispatchers.IO) {
                    Pair(ShopifyRepository.newArrivals(), ShopifyRepository.bestSellers())
                }
                _state.value = _state.value.copy(loading = false, newArrivals = arrivals, bestSellers = sellers)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Couldn't load products. Pull to refresh.")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true, error = null)
            try {
                val (arrivals, sellers) = withContext(Dispatchers.IO) { ShopifyRepository.refreshHome() }
                _state.value = _state.value.copy(refreshing = false, newArrivals = arrivals, bestSellers = sellers)
            } catch (e: Exception) {
                _state.value = _state.value.copy(refreshing = false, error = "Couldn't refresh. Try again.")
            }
        }
    }

    fun toggleWishlist(productId: String) {
        val cur = _state.value.wishlistIds
        _state.value = _state.value.copy(
            wishlistIds = if (cur.contains(productId)) cur - productId else cur + productId
        )
    }
}
