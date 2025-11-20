package com.alvin.pulselink.presentation.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterForm(
    uiState: RegisterUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAgreeToTermsChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    buttonColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Username Field
        Text(
            text = "Username",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your username", color = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = buttonColor
            ),
            isError = uiState.usernameError != null,
            supportingText = {
                uiState.usernameError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Email Field
        Text(
            text = "Email",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your email", color = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = buttonColor
            ),
            isError = uiState.emailError != null,
            supportingText = {
                uiState.emailError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Phone Number Field
        Text(
            text = "Phone Number",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Phone number", color = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = buttonColor
            ),
            isError = uiState.phoneError != null,
            supportingText = {
                uiState.phoneError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password Field
        Text(
            text = "Password",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your password", color = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = buttonColor
            ),
            isError = uiState.passwordError != null,
            supportingText = {
                uiState.passwordError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Confirm Password Field
        Text(
            text = "Confirm Password",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Confirm your password", color = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = buttonColor
            ),
            isError = uiState.confirmPasswordError != null,
            supportingText = {
                uiState.confirmPasswordError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Terms and Conditions Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.agreeToTerms,
                onCheckedChange = onAgreeToTermsChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = buttonColor
                )
            )
            Text(
                text = "I agree to the Privacy Policy\nand Terms of Service",
                fontSize = 14.sp,
                color = Color(0xFF2C3E50),
                lineHeight = 20.sp
            )
        }
        
        // Error Message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Register Button
        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Register",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = Color(0xFF2C3E50)
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Login",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonColor
                )
            }
        }
    }
}
