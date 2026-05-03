package com.asyria.security.ui.screens

  import androidx.compose.animation.*
  import androidx.compose.animation.core.*
  import androidx.compose.foundation.*
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.graphics.vector.ImageVector
  import androidx.compose.ui.platform.LocalHapticFeedback
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.draw.alpha
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import com.asyria.security.ui.theme.*
  import androidx.compose.ui.hapticfeedback.HapticFeedbackType
  import com.asyria.security.data.prayer.SupplicationEntity
  import androidx.lifecycle.viewmodel.compose.viewModel

  @Composable
  fun SpiritualHub(
      onClose: () -> Unit,
      viewModel: PrayerViewModel = viewModel()
  ) {
      val uiState by viewModel.uiState.collectAsState()
      var activeTab by remember { mutableIntStateOf(0) }
      val haptic = LocalHapticFeedback.current

      Surface(
          modifier = Modifier.fillMaxSize(),
          color = VoidBlack.copy(alpha = 0.98f)
      ) {
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .statusBarsPadding()
          ) {
              HeaderBlock(uiState.city, onClose)

              Box(modifier = Modifier.weight(1f)) {
                  Crossfade(targetState = activeTab, label = "TabContent") { tab ->
                      when(tab) {
                          0 -> DetailedPrayerTimes(uiState)
                          1 -> EnhancedAzkarModule(uiState.supplications)
                      }
                  }
              }

              SpiritualBottomNav(
                  currentTab = activeTab,
                  onTabChange = {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      activeTab = it
                  }
              )
          }
      }
  }

  @Composable
  fun HeaderBlock(city: String, onClose: () -> Unit) {
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
          Column {
              Text(
                  text = "NEURAL SPIRITUAL HUB",
                  style = MaterialTheme.typography.titleSmall,
                  color = AmberZen,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 2.sp
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.LocationOn, null, tint = TextGray, modifier = Modifier.size(10.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                      text = city,
                      style = MaterialTheme.typography.labelSmall,
                      color = TextGray
                  )
              }
          }
          IconButton(
              onClick = onClose,
              modifier = Modifier
                  .clip(CircleShape)
                  .background(GlassWhite)
          ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = AmberZen)
          }
      }
  }

  @Composable
  fun DetailedPrayerTimes(uiState: PrayerUiState) {
      val timings = uiState.timings
      val prayers = remember(timings) {
          if (timings == null) emptyList()
          else listOf(
              "Fajr" to timings.Fajr,
              "Dhuhr" to timings.Dhuhr,
              "Asr" to timings.Asr,
              "Maghrib" to timings.Maghrib,
              "Isha" to timings.Isha
          )
      }

      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          // Error state
          if (uiState.error != null && uiState.timings == null) {
              Surface(
                  modifier = Modifier.fillMaxWidth(),
                  color = Color.Red.copy(alpha = 0.1f),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
              ) {
                  Column(
                      modifier = Modifier.padding(20.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                      Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                      Spacer(modifier = Modifier.height(8.dp))
                      Text(text = uiState.error, color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(text = "يرجى التحقق من الاتصال بالإنترنت", color = TextGray, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                  }
              }
              Spacer(modifier = Modifier.height(24.dp))
          }

          // Loading state
          if (uiState.isLoading) {
              Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = AmberZen)
              }
          } else {
              // Next Prayer Hero Card
              Surface(
                  modifier = Modifier.fillMaxWidth().height(180.dp),
                  shape = RoundedCornerShape(32.dp),
                  color = AmberZen.copy(alpha = 0.05f),
                  border = BorderStroke(2.dp, Brush.linearGradient(listOf(AmberZen, Color.Transparent, AmberZen)))
              ) {
                  Column(
                      modifier = Modifier.fillMaxSize(),
                      verticalArrangement = Arrangement.Center,
                      horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                      if (uiState.nextPrayerName.isNotBlank()) {
                          Text(
                              text = "NEXT: ${uiState.nextPrayerName.uppercase()}",
                              style = MaterialTheme.typography.labelMedium,
                              color = AmberZen,
                              fontWeight = FontWeight.Black,
                              letterSpacing = 2.sp
                          )
                          Spacer(modifier = Modifier.height(12.dp))
                          Text(
                              text = uiState.countdown,
                              style = MaterialTheme.typography.displayMedium,
                              color = Color.White,
                              fontWeight = FontWeight.Light,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                          )
                      } else {
                          Text(text = "جارٍ تحميل مواقيت الصلاة...", color = TextGray, style = MaterialTheme.typography.bodyMedium)
                      }
                  }
              }
          }

          Spacer(modifier = Modifier.height(32.dp))

          if (prayers.isNotEmpty()) {
              Text(
                  text = "DAILY INTERVALS",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextGray,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Start,
                  letterSpacing = 2.sp
              )
              Spacer(modifier = Modifier.height(16.dp))

              androidx.compose.foundation.lazy.LazyColumn(
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.fillMaxWidth()
              ) {
                  items(prayers.size) { index ->
                      val (name, time) = prayers[index]
                      val isNext = name == uiState.nextPrayerName
                      val arabicName = when(name) {
                          "Fajr" -> "الفجر"; "Dhuhr" -> "الظهر"; "Asr" -> "العصر"
                          "Maghrib" -> "المغرب"; "Isha" -> "العشاء"; else -> name
                      }
                      Surface(
                          modifier = Modifier.fillMaxWidth(),
                          color = if (isNext) AmberZen.copy(alpha = 0.15f) else GlassWhite,
                          shape = RoundedCornerShape(20.dp),
                          border = BorderStroke(1.dp, if (isNext) AmberZen else GlassBorder)
                      ) {
                          Row(
                              modifier = Modifier.padding(20.dp),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                              Column {
                                  Text(text = name, color = if (isNext) AmberZen else Color.White, fontWeight = FontWeight.Bold)
                                  Text(text = arabicName, color = TextGray, style = MaterialTheme.typography.labelSmall)
                              }
                              Text(
                                  text = time.substringBefore(" "),
                                  color = if (isNext) AmberZen else TextGray,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                  fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                  style = MaterialTheme.typography.titleMedium
                              )
                          }
                      }
                  }
              }
          }
      }
  }

  @Composable
  fun EnhancedAzkarModule(supplications: List<SupplicationEntity>) {
      // Derive categories dynamically from actual supplications data
      val categories = remember(supplications) {
          supplications.map { it.category }.distinct()
      }
      var selectedCategory by remember(categories) {
          mutableStateOf(categories.firstOrNull() ?: "")
      }

      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp)
      ) {
          if (supplications.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = AmberZen)
              }
              return@Column
          }

          // Category Selector — uses ACTUAL category names from database
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
              categories.forEach { cat ->
                  val isSelected = selectedCategory == cat
                  val shortName = when {
                      cat.startsWith("Morning") -> "Morning"
                      cat.startsWith("Quranic") -> "Quran"
                      cat.startsWith("Comprehensive") -> "Comprehensive"
                      cat.startsWith("For Ease") -> "Soul"
                      cat.startsWith("For Knowledge") -> "Knowledge"
                      cat.startsWith("For Forgiveness") -> "Forgiveness"
                      cat.startsWith("For Family") -> "Family"
                      cat.startsWith("For Protection") -> "Protection"
                      cat.startsWith("Misc") -> "Misc"
                      else -> if (cat.length > 10) cat.substring(0, 10) else cat
                  }
                  Surface(
                      modifier = Modifier.clickable { selectedCategory = cat },
                      color = if (isSelected) AmberZen else GlassWhite,
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, if (isSelected) AmberZen else GlassBorder)
                  ) {
                      Text(
                          text = shortName,
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                          color = if (isSelected) VoidBlack else Color.White,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold
                      )
                  }
              }
          }

          Spacer(modifier = Modifier.height(24.dp))

          val filtered = supplications.filter { it.category == selectedCategory }

          androidx.compose.foundation.lazy.LazyColumn(
              verticalArrangement = Arrangement.spacedBy(16.dp),
              modifier = Modifier.fillMaxWidth()
          ) {
              items(filtered.size) { index ->
                  SupplicationCard(filtered[index])
              }
          }
      }
  }

  @Composable
  fun SpiritualBottomNav(currentTab: Int, onTabChange: (Int) -> Unit) {
      Surface(
          modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
          color = GlassWhite,
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, GlassBorder)
      ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceEvenly
          ) {
              SpiritualNavItem(
                  icon = Icons.Default.Schedule,
                  label = "Prayer Times",
                  isSelected = currentTab == 0,
                  onClick = { onTabChange(0) }
              )
              SpiritualNavItem(
                  icon = Icons.Default.AutoAwesome,
                  label = "Azkar",
                  isSelected = currentTab == 1,
                  onClick = { onTabChange(1) }
              )
          }
      }
  }

  @Composable
  fun SpiritualNavItem(
      icon: ImageVector,
      label: String,
      isSelected: Boolean,
      onClick: () -> Unit
  ) {
      val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "Alpha")
      val color = if (isSelected) AmberZen else TextGray

      Column(
          modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .clickable(onClick = onClick)
              .padding(vertical = 8.dp, horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          Icon(icon, contentDescription = label, tint = color.copy(alpha = alpha))
          Text(
              text = label,
              color = color.copy(alpha = alpha),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
      }
  }
  