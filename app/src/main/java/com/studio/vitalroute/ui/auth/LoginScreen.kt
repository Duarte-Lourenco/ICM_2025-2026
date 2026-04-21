package com.studio.vitalroute.ui.auth

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

    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

        // ── Logo / Título ─────────────────────────────────────
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

        // ── Título do modo ────────────────────────────────────
        Text(
            text       = if (uiState.isRegisterMode) "Criar conta" else "Entrar",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // ── Campo Nome (só no registo) ────────────────────────
        if (uiState.isRegisterMode) {
            AuthTextField(
                value         = name,
                onValueChange = { name = it },
                label         = "Nome",
                icon          = Icons.Default.Person,
                imeAction     = ImeAction.Next
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Email ─────────────────────────────────────────────
        AuthTextField(
            value         = email,
            onValueChange = { email = it },
            label         = "Email",
            icon          = Icons.Default.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next
        )

        Spacer(Modifier.height(12.dp))

        // ── Password ──────────────────────────────────────────
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

        Spacer(Modifier.height(8.dp))

        // ── Erro ──────────────────────────────────────────────
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

        // ── Botão principal ───────────────────────────────────
        Button(
            onClick = {
                if (uiState.isRegisterMode) viewModel.signUp(name, email, password)
                else                        viewModel.signIn(email, password)
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

        // ── Toggle login / registo ────────────────────────────
        TextButton(onClick = {
            viewModel.toggleMode()
            name = ""; email = ""; password = ""
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

        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────
//  Componentes privados
// ─────────────────────────────────────────────────────────────

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
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
        modifier   = Modifier.fillMaxWidth(),
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
