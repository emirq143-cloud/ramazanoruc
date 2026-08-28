package com.emir.oructakibi.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun WorshipScreen(
    ctx: Context,
    zikirCount: Int,
    onZikirChange: (Int) -> Unit,
    quranPage: Int,
    onQuranPageChange: (Int) -> Unit,
    prayerList: List<String>,
    checkedPrayers: SnapshotStateList<String>,
    today: LocalDate
) {
    Column {
        /* ===================== İBADET ===================== */
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Günün İbadetleri", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                prayerList.forEach { prayer ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checkedPrayers.contains(prayer)) checkedPrayers.remove(prayer)
                                else checkedPrayers.add(prayer)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = checkedPrayers.contains(prayer),
                            onCheckedChange = {
                                if (it) checkedPrayers.add(prayer) else checkedPrayers.remove(prayer)
                            }
                        )
                        Text(prayer, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Zikirmatik", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(60.dp))
                        .background(Color(0xFFE8F5E9))
                        .clickable {
                            onZikirChange(zikirCount + 1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        zikirCount.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Artırmak için dokunun", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    onZikirChange(0)
                }) {
                    Text("Sıfırla", color = Color.Red)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // KUR'AN TAKİBİ
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Kur'an-ı Kerim Takibi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Şu an $quranPage. sayfadasınız", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { quranPage / 604f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF43A047)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1. Sayfa", fontSize = 10.sp, color = Color.Gray)
                    Text("604. Sayfa (Hatim)", fontSize = 10.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { 
                        onQuranPageChange((quranPage - 1).coerceAtLeast(1))
                    }, modifier = Modifier.weight(1f)) { Text("-1 Sayfa") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { 
                        onQuranPageChange((quranPage + 1).coerceAtMost(604))
                    }, modifier = Modifier.weight(1f)) { Text("+1 Sayfa") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // GÜNÜN ESMASI VE DUASI
        val esmaList = listOf(
            "er-Rahmân" to "Dünyada bütün mahlukata şefkat gösteren.",
            "er-Rahîm" to "Ahirette sadece mü'minlere merhamet eden.",
            "el-Melik" to "Bütün kainatın sahibi ve mutlak hükümdarı.",
            "el-Kuddûs" to "Her türlü eksiklikten uzak, tertemiz."
        )
        val currentEsma = esmaList[today.dayOfMonth % esmaList.size]

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F8E9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Günün Esma-ül Hüsna'sı", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(Modifier.height(4.dp))
                Text(currentEsma.first, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                Text(currentEsma.second, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFC8E6C9))
                
                Text("Günün Duası", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("Allah'ım! Senin rızan için oruç tuttum, Sana inandım ve Senin rızkınla iftar ettim.", fontSize = 13.sp)
            }
        }
    }
}
