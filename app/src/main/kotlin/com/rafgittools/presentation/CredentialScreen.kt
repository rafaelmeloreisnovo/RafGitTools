package com.rafgittools.presentation.auth

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun CredentialScreen(vm: CredentialViewModel) {
    var token by remember { mutableStateOf("") }

    Column {
        Text("Authentication")
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Token") })
        Button(onClick = { vm.saveToken(token) }) {
            Text("Save")
        }
    }
}
