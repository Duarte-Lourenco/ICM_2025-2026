package com.studio.vitalroute.ui.auth


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.vitalroute.ui.theme.*

@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isForgotPassword) {
        ForgotPasswordDialog(
            email     = uiState.forgotEmail,
            isLoading = uiState.forgotLoading,
            error     = uiState.forgotError,
            success   = uiState.forgotSuccess,
            onEmailChange = { viewModel.updateForgotEmail(it) },
            onSend    = { viewModel.sendPasswordReset() },
            onDismiss = { viewModel.cancelForgotPassword() }
        )
    }

    if (uiState.isEmailVerificationPending) {
        EmailVerificationScreen(
            email     = uiState.pendingEmail,
            isLoading = uiState.isLoading,
            error     = uiState.error,
            emailSent = uiState.verificationEmailSent,
            onCheck   = { viewModel.checkEmailVerification() },
            onResend  = { viewModel.resendVerificationEmail() },
            onBack    = { viewModel.signOut() }
        )
        return
    }

    var name         by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var weightInput  by remember { mutableStateOf("") }
    var heightInput  by remember { mutableStateOf("") }
    var gender       by remember { mutableStateOf("male") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(60.dp))

        // logo titulo
        Icon(
            imageVector = Icons.Default.DirectionsBike,
            contentDescription = null,
            tint     = VitalGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text       = "VitalRoute",
            color      = Color.White,
            fontSize   = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text     = "Navegação para ciclistas e corredores",
            color    = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        // titulo do modo
        Text(
            text       = if (uiState.isRegisterMode) "Criar conta" else "Entrar",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // campos extra so no registo
        if (uiState.isRegisterMode) {
            AuthTextField(
                value         = name,
                onValueChange = { name = it },
                label         = "Nome",
                icon          = Icons.Default.Person,
                imeAction     = ImeAction.Next
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AuthTextField(
                    value         = weightInput,
                    onValueChange = { weightInput = it },
                    label         = "Peso (kg)",
                    icon          = Icons.Default.MonitorWeight,
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Next,
                    modifier      = Modifier.weight(1f)
                )
                AuthTextField(
                    value         = heightInput,
                    onValueChange = { heightInput = it },
                    label         = "Altura (cm)",
                    icon          = Icons.Default.Height,
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Next,
                    modifier      = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            // seletor de genero
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                listOf("male" to "Masculino", "female" to "Feminino").forEach { (key, label) ->
                    FilterChip(
                        selected = gender == key,
                        onClick  = { gender = key },
                        label    = { Text(label) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VitalGreen.copy(alpha = 0.2f),
                            selectedLabelColor     = VitalGreen,
                            containerColor         = Color(0xFF111111),
                            labelColor             = Color.Gray
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // campo email
        AuthTextField(
            value         = email,
            onValueChange = { email = it },
            label         = "Email",
            icon          = Icons.Default.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next
        )

        Spacer(Modifier.height(12.dp))

        // campo password
        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            label         = { Text("Password") },
            leadingIcon   = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
            trailingIcon  = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = authTextFieldColors(),
            singleLine = true
        )

        if (uiState.isRegisterMode) {
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "Mínimo 8 caracteres, com maiúscula, minúscula e número",
                color    = Color(0xFF666666),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TextButton(
                onClick  = { viewModel.startForgotPassword(email) },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Esqueci-me da password", color = Color(0xFF888888), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // erro de validacao
        uiState.error?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFF2A0000),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text     = err,
                    color    = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(12.dp, 10.dp),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // botao principal
        Button(
            onClick = {
                if (uiState.isRegisterMode) viewModel.signUp(
                    name     = name,
                    email    = email,
                    password = password,
                    weightKg = weightInput.toFloatOrNull() ?: 0f,
                    heightCm = heightInput.toIntOrNull() ?: 0,
                    gender   = gender
                )
                else viewModel.signIn(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalGreen),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = Color.Black,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text       = if (uiState.isRegisterMode) "CRIAR CONTA" else "ENTRAR",
                    color      = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // toggle login registo
        TextButton(onClick = {
            viewModel.toggleMode()
            name = ""; email = ""; password = ""; weightInput = ""; heightInput = ""; gender = "male"
        }) {
            Text(
                text  = if (uiState.isRegisterMode)
                            "Já tens conta? Entra aqui"
                        else
                            "Não tens conta? Regista-te",
                color = VitalGreen,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // divisor visual
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A2A))
            Text("  ou  ", color = Color(0xFF444444), fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A2A))
        }

        Spacer(Modifier.height(12.dp))

        // botao convidado
        OutlinedButton(
            onClick  = { viewModel.signInAsGuest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape  = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
            enabled = !uiState.isLoading
        ) {
            Text(
                text  = "Continuar como Convidado",
                color = Color(0xFF888888),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
            shape  = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.LockReset, null, tint = VitalGreen, modifier = Modifier.size(24.dp))
                    Text("Recuperar password", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))

                if (success) {
                    Surface(color = Color(0xFF002A00), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp, 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                                Text("Email enviado!", color = VitalGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Verifica a caixa de entrada de $email e segue as instruções para criar uma nova password.",
                                color = Color(0xFF88CC88), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                        Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Introduz o teu email e enviamos um link para criares uma nova password.",
                        color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSend() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = authTextFieldColors()
                    )
                    error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                        ) { Text("Cancelar") }
                        Button(onClick = onSend, enabled = !isLoading, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = VitalGreen)) {
                            if (isLoading)
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            else
                                Text("Enviar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailVerificationScreen(
    email: String,
    isLoading: Boolean,
    error: String?,
    emailSent: Boolean,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailUnread,
            contentDescription = null,
            tint     = VitalGreen,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text       = "Confirma o teu email",
            color      = Color.White,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "Enviámos um link de confirmação para",
            color     = Color.Gray,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text       = email,
            color      = VitalGreen,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "Abre o email e clica no link para ativar a conta.",
            color     = Color.Gray,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color    = Color(0xFF1A1A00),
            shape    = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFCC00), modifier = Modifier.size(16.dp))
                Text(
                    text     = "Não encontras o email? Verifica também a pasta de spam ou lixo.",
                    color    = Color(0xFFCCAA00),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (emailSent) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFF002A00),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                    Text(
                        text     = "Email enviado com sucesso!",
                        color    = VitalGreen,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        error?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFF2A0000),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text     = err,
                    color    = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(12.dp, 10.dp),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick  = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalGreen),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.5.dp)
            } else {
                Text("JÁ VERIFIQUEI", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick  = onResend,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape  = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
            enabled = !isLoading
        ) {
            Text("Reenviar email", color = Color(0xFF888888), fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Voltar ao início de sessão", color = Color(0xFF555555), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = Color.Gray) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction    = imeAction
        ),
        modifier   = modifier,
        shape      = RoundedCornerShape(12.dp),
        colors     = authTextFieldColors(),
        singleLine = true
    )
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = VitalGreen,
    unfocusedBorderColor = Color(0xFF333333),
    focusedLabelColor    = VitalGreen,
    unfocusedLabelColor  = Color.Gray,
    cursorColor          = VitalGreen,
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    focusedContainerColor   = Color(0xFF111111),
    unfocusedContainerColor = Color(0xFF111111)
)