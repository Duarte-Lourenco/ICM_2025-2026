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

        // logo / título
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

        // título do modo
        Text(
            text       = if (uiState.isRegisterMode) "Criar conta" else "Entrar",
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // campos extra (só no registo)
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

            // seletor de género
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

        // email
        AuthTextField(
            value         = email,
            onValueChange = { email = it },
            label         = "Email",
            icon          = Icons.Default.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next
        )

        Spacer(Modifier.height(12.dp))

        // password
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

        // erro
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

        // botão principal
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

        // toggle login / registo
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

        // divisor
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A2A))
            Text("  ou  ", color = Color(0xFF444444), fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A2A))
        }

        Spacer(Modifier.height(12.dp))

        // botão convidado
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
