package com.example.week2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------
// A. VERİ KATMANI: tüm alanlar val + @Immutable
// ---------------------------------------------------------------
@Immutable
data class HardwareItem(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val stockCount: Int
)

// Sabit test veri kümesi
private val initialHardware = listOf(
    HardwareItem(id = "101", name = "MacBook Pro M3 Max", category = "Geliştirici Bilgisayarı", cost = 3499.0, stockCount = 5),
    HardwareItem(id = "102", name = "NVIDIA RTX 4090 Sunucu", category = "Yapay Zeka / GPU", cost = 1599.0, stockCount = 2),
    HardwareItem(id = "103", name = "Google Pixel 8 Pro", category = "Mobil Test Cihazı", cost = 999.0, stockCount = 8),
    HardwareItem(id = "104", name = "Dell UltraSharp 49\"", category = "Monitör", cost = 1199.0, stockCount = 4),
    HardwareItem(id = "105", name = "Sony WH-1000XM5 Kulaklık", category = "Odaklanma Ekipmanı", cost = 399.0, stockCount = 12),
    HardwareItem(id = "106", name = "Raspberry Pi 5 8GB", category = "IoT Geliştirme Kiti", cost = 85.0, stockCount = 25),
    HardwareItem(id = "107", name = "HHKB Professional Keyboard", category = "Ergonomik Klavye", cost = 299.0, stockCount = 0), // Stok sınırı test verisi
    HardwareItem(id = "108", name = "AMD Ryzen Threadripper", category = "Yapay Zeka / GPU", cost = 4999.0, stockCount = 1)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DevClusterScreen()
            }
        }
    }
}

// ---------------------------------------------------------------
// B. DURUM YÖNETİMİ: tek merkezi Stateful kapsayıcı
// ---------------------------------------------------------------
@Composable
fun DevClusterScreen() {
    // Ekran döndürülünce (Portrait -> Landscape) arama metni kaybolmasın
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Donanım listesi Slot Table içinde kilitli
    val hardwareList = remember { mutableStateListOf<HardwareItem>().apply { addAll(initialHardware) } }

    // Turnike (memoization): filtre sadece searchQuery (veya liste) değişince çalışır
    val filteredList = remember(searchQuery, hardwareList.toList()) {
        if (searchQuery.isBlank()) {
            hardwareList.toList()
        } else {
            hardwareList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Toplam harcanan ödenek: sadece liste değişince yeniden hesaplanır
    val totalBudget = remember(hardwareList.toList()) {
        hardwareList.sumOf { it.cost * it.stockCount }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "DevCluster Kaynak Yönetimi",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Toplam Ödenek: $${"%.2f".format(totalBudget)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // State Down (query) / Events Up (onQueryChange)
            HardwareSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            // C. LİSTE MİMARİSİ: LazyColumn + key
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = filteredList, key = { it.id }) { item ->
                    HardwareCard(
                        item = item,
                        // Karta tıklayınca stok 1 azalır (listenin ortasındaki eleman güncellenir)
                        onClick = {
                            val index = hardwareList.indexOfFirst { it.id == item.id }
                            if (index != -1 && hardwareList[index].stockCount > 0) {
                                hardwareList[index] =
                                    hardwareList[index].copy(stockCount = hardwareList[index].stockCount - 1)
                            }
                        },
                        onDelete = { hardwareList.removeAll { it.id == item.id } }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------
// D. STATELESS UI BİLEŞENLERİ: içlerinde remember yok
// ---------------------------------------------------------------
@Composable
fun HardwareSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Donanım ara...") },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun HardwareCard(
    item: HardwareItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        // Sıralama: önce clickable, sonra padding
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = item.category, style = MaterialTheme.typography.bodySmall)
                Text(text = "Maliyet: $${item.cost}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (item.stockCount == 0) "Stokta yok" else "Stok: ${item.stockCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.stockCount == 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onDelete) {
                Text("Sil")
            }
        }
    }
}
