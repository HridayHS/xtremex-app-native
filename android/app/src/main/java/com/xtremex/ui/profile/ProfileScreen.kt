package com.xtremex.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary

// Profile tab, ported from (tabs)/profile.tsx — placeholder in the old app
// (hardcoded name/email, rows only fire haptics). Kept as-is.

@Composable
fun ProfileScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        item {
            Text("Profile", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(bottom = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("Hriday Sharma", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("hridaysharma42@gmail.com", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        items(listOf("My Orders", "Saved Addresses", "Payment Methods", "Support", "Logout")) { label ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Primary, modifier = Modifier.weight(1f))
                Text("›", fontSize = 18.sp, color = Muted)
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
