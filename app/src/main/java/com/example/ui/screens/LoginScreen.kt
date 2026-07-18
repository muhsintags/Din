package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.ScriptureViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ScriptureViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showGoogleSetupDialog by remember { mutableStateOf(false) }
    var lastGoogleError by remember { mutableStateOf<String?>(null) }

    // Email/Password States
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Setup Google Sign In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        isLoading = true
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        try {
                            val auth = FirebaseAuth.getInstance()
                            auth.signInWithCredential(credential)
                                .addOnCompleteListener { authTask ->
                                    isLoading = false
                                    if (authTask.isSuccessful) {
                                        val firebaseUser = authTask.result?.user
                                        if (firebaseUser != null) {
                                            viewModel.signInWithFirebaseUser(firebaseUser)
                                            Toast.makeText(context, "Giriş Başarılı: ${firebaseUser.displayName}", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                    } else {
                                        val errorMsg = authTask.exception?.localizedMessage ?: "Firebase kimlik doğrulama başarısız."
                                        lastGoogleError = errorMsg
                                        showGoogleSetupDialog = true
                                    }
                                }
                        } catch (e: Exception) {
                            isLoading = false
                            lastGoogleError = "Firebase başlatılamadı veya yapılandırılmadı: ${e.localizedMessage}"
                            showGoogleSetupDialog = true
                        }
                    } else {
                        lastGoogleError = "ID Token alınamadı."
                        showGoogleSetupDialog = true
                    }
                } else {
                    lastGoogleError = "Google hesabı alınamadı."
                    showGoogleSetupDialog = true
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val errorMsg = when (statusCode) {
                    7 -> "Ağ hatası (Kod: 7). Lütfen internet bağlantınızı kontrol edin."
                    10 -> "Geliştirici yapılandırma hatası (Developer Error - Kod: 10). Lütfen Firebase Console'da SHA-1 parmak izinizi doğru girdiğinizden emin olun."
                    12500 -> "Google Play Hizmetleri bu cihazda desteklenmiyor veya yapılandırılmamış (Kod: 12500)."
                    12501 -> "Giriş işlemi iptal edildi veya Google hesabı seçilmedi (Kod: 12501)."
                    12502 -> "Giriş işlemi zaten devam ediyor (Kod: 12502)."
                    else -> "Google Giriş Hatası (Kod: $statusCode): ${e.localizedMessage}"
                }
                lastGoogleError = errorMsg
                showGoogleSetupDialog = true
            } catch (e: Exception) {
                lastGoogleError = "Hata oluştu: ${e.localizedMessage}"
                showGoogleSetupDialog = true
            }
        } else {
            lastGoogleError = "Google Giriş Penceresi kapatıldı veya veri dönmedi (Result Code: ${result.resultCode})."
            showGoogleSetupDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Logo and Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Scriptorium Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "Kutsal Metinler",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "İnsanlığın kadim bilgelik kütüphanesine açılan kapı.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Main Login Area
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = SacredGold)
                    Text(
                        text = "Giriş yapılıyor...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Google Sign-In Button
                    Button(
                        onClick = {
                            try {
                                isLoading = true
                                val webClientId = context.getString(com.Muhsin.kutuphane.R.string.default_web_client_id)
                                if (webClientId == "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com") {
                                    isLoading = false
                                    lastGoogleError = "Uygulamada varsayılan test istemci kimliği (default_web_client_id) bulunuyor. Gerçek Google Girişi için Firebase Client ID tanımlanmalıdır."
                                    showGoogleSetupDialog = true
                                } else {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(webClientId)
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    // Sign out first to force account chooser
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                lastGoogleError = e.localizedMessage
                                showGoogleSetupDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("google_login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Custom high-quality CSS-like Google icon
                            GoogleIconVector()
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Google ile Giriş Yap",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "veya",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // Email/Password Authentication Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Tabs for Login / Sign Up
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { isSignUpMode = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Giriş Yap",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { isSignUpMode = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Hesap Aç",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignUpMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSignUpMode) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Ad Soyad") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("E-posta") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Şifre") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Şifreyi gizle" else "Şifreyi göster"
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        // Do nothing, let user click button
                                    }
                                )
                            )

                            Button(
                                onClick = {
                                    if (emailInput.isBlank() || passwordInput.isBlank()) {
                                        Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (isSignUpMode && nameInput.isBlank()) {
                                        Toast.makeText(context, "Lütfen adınızı girin.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    val authInstance = FirebaseAuth.getInstance()
                                    if (isSignUpMode) {
                                        authInstance.createUserWithEmailAndPassword(emailInput, passwordInput)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val user = task.result?.user
                                                    if (user != null) {
                                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                            .setDisplayName(nameInput)
                                                            .build()
                                                        user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                                                            isLoading = false
                                                            viewModel.signInWithFirebaseUser(user)
                                                            Toast.makeText(context, "Kayıt Başarılı: $nameInput", Toast.LENGTH_SHORT).show()
                                                            onLoginSuccess()
                                                        }
                                                    } else {
                                                        isLoading = false
                                                    }
                                                } else {
                                                    isLoading = false
                                                    val errorMsg = task.exception?.localizedMessage ?: "Kayıt başarısız oldu."
                                                    Toast.makeText(context, "Hata: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    } else {
                                        authInstance.signInWithEmailAndPassword(emailInput, passwordInput)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    val user = task.result?.user
                                                    if (user != null) {
                                                        viewModel.signInWithFirebaseUser(user)
                                                        Toast.makeText(context, "Giriş Başarılı: ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    }
                                                } else {
                                                    val errorMsg = task.exception?.localizedMessage ?: "Giriş başarısız oldu."
                                                    Toast.makeText(context, "Hata: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("auth_submit_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Kayıt Ol ve Başla" else "Güvenli Giriş Yap",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    if (showGoogleSetupDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSetupDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Google Oturum Açma Kılavuzu",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Google Play Servisleri veya Firebase entegrasyonu ek yapılandırma gerektirmektedir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (lastGoogleError != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Durum/Hata Detayı:\n$lastGoogleError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "Gerçek Google Girişini Aktifleştirme Adımları:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "1. Firebase Console'da bir Android projesi oluşturun.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. SHA-1 parmak izinizi Firebase projenize tanımlayın.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. google-services.json dosyasını indirip projenin /app/ dizinine ekleyin.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "4. strings.xml içindeki 'default_web_client_id' değerini Web Client ID'niz ile değiştirin.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGoogleSetupDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
fun GoogleIconVector() {
    // Elegant custom drawing of the Google multi-color G logo
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val width = size.width
        val height = size.height
        // We can draw a beautiful colored representation or use high fidelity vectors
        // Instead of hard drawing shapes that might look slightly off, let's draw a nice vector
    }
    // Alternatively, let's use an elegant composition of colored elements or a high-quality icon.
    // To make it look extremely clean and precise, we can use icons or draw Google logo elements:
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Red, Green, Blue, Yellow circles overlapping beautifully to signify Google
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEA4335)))
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF34A853)))
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4285F4)))
        }
    }
}
