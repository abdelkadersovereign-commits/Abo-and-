package com.asyria.security.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.R
import com.asyria.security.data.SessionManager
import com.asyria.security.ui.theme.CyberCyan
import com.asyria.security.ui.theme.GlassBorder
import com.asyria.security.ui.theme.GlassWhite
import com.asyria.security.ui.theme.VoidBlack
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "login_alpha"
    )
    LaunchedEffect(Unit) { startAnimation = true }

    val firebaseAuthWithGoogle = { account: GoogleSignInAccount ->
        coroutineScope.launch {
            try {
                isLoading = true
                error = null
                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
                Firebase.auth.signInWithCredential(credential).await()
                SessionManager(context).setLoggedIn(true)
                onLoginSuccess()
            } catch (e: Exception) {
                error = "خطأ في Firebase: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                error = when (e.statusCode) {
                    10 -> "Google Sign-In غير متاح في هذا الإصدار. استخدم البريد الإلكتروني وكلمة المرور."
                    else -> "خطأ Google Sign-In (${e.statusCode}): ${e.localizedMessage}"
                }
            } catch (e: Exception) {
                error = e.localizedMessage
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 48.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "A.SYRIA SECURITY",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            Text(
                text = if (isRegisterMode) "إنشاء حساب جديد" else "تسجيل الدخول",
                fontSize = 14.sp,
                color = CyberCyan.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Neural ID (Email)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyberCyan,
                    unfocusedContainerColor = GlassWhite,
                    focusedContainerColor = GlassWhite,
                )
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access Key (Password)") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyberCyan,
                    unfocusedContainerColor = GlassWhite,
                    focusedContainerColor = GlassWhite,
                )
            )

            // Confirm password - only in register mode
            AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تأكيد كلمة المرور") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberCyan,
                        unfocusedContainerColor = GlassWhite,
                        focusedContainerColor = GlassWhite,
                    )
                )
            }

            // Error / Success messages
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = CyberCyan,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            // Main action button
            Button(
                onClick = {
                    error = null
                    successMessage = null
                    if (email.isBlank() || password.isBlank()) {
                        error = "الرجاء إدخال البريد الإلكتروني وكلمة المرور"
                        return@Button
                    }
                    if (isRegisterMode) {
                        if (password.length < 6) {
                            error = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            error = "كلمتا المرور غير متطابقتين"
                            return@Button
                        }
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                Firebase.auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
                                SessionManager(context).setLoggedIn(true)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                error = when {
                                    e.message?.contains("email-already-in-use") == true -> "هذا البريد الإلكتروني مستخدم بالفعل"
                                    e.message?.contains("invalid-email") == true -> "البريد الإلكتروني غير صالح"
                                    e.message?.contains("weak-password") == true -> "كلمة المرور ضعيفة جداً"
                                    else -> e.localizedMessage ?: "فشل إنشاء الحساب"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                Firebase.auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                                SessionManager(context).setLoggedIn(true)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                error = when {
                                    e.message?.contains("user-not-found") == true -> "لا يوجد حساب بهذا البريد. أنشئ حساباً جديداً."
                                    e.message?.contains("wrong-password") == true -> "كلمة المرور خاطئة"
                                    e.message?.contains("invalid-email") == true -> "البريد الإلكتروني غير صالح"
                                    e.message?.contains("invalid-credential") == true -> "البيانات غير صحيحة. تحقق من البريد وكلمة المرور."
                                    else -> e.localizedMessage ?: "فشل تسجيل الدخول"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isRegisterMode) "إنشاء الحساب" else "تسجيل الدخول",
                        color = VoidBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Toggle between login / register
            TextButton(onClick = {
                isRegisterMode = !isRegisterMode
                error = null
                successMessage = null
                password = ""
                confirmPassword = ""
            }) {
                Text(
                    text = if (isRegisterMode) "لديك حساب؟ سجّل دخولك" else "ليس لديك حساب؟ أنشئ حساباً جديداً",
                    color = CyberCyan,
                    fontSize = 14.sp
                )
            }

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(color = GlassBorder, modifier = Modifier.weight(1f))
                Text(" أو ", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
                Divider(color = GlassBorder, modifier = Modifier.weight(1f))
            }

            // Google Sign-In
            OutlinedButton(
                onClick = {
                    error = null
                    try {
                        val webClientId = context.getString(R.string.default_web_client_id)
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } catch (e: Exception) {
                        error = "خطأ في إعداد Google Sign-In: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Text("المتابعة باستخدام Google", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}
