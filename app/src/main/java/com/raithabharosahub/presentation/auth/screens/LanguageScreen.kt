package com.raithabharosahub.presentation.auth.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.raithabharosahub.R
import com.raithabharosahub.presentation.onboarding.OnboardingViewModel
import com.raithabharosahub.util.findActivity

private val BrandGreen = Color(0.086275f, 0.639216f, 0.290196f, 1f)

/**
 * Pre-login language picker screen.
 *
 * This screen is shown BEFORE authentication when no language preference has
 * been persisted yet. It is separate from the onboarding LanguagePickerScreen
 * so that the auth flow remains independent of onboarding state.
 *
 * On selection it writes the choice to DataStore (via OnboardingViewModel.setLanguage)
 * and calls [onLanguageSelected] to proceed to LoginScreen.
 */
@Composable
fun LanguageScreen(
    onLanguageSelected: () -> Unit,
    onboardingViewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo / name
            Text(
                text = stringResource(R.string.logo_placeholder),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ರೈತ ಭರೋಸಾ ಹಬ್",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = BrandGreen.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // English button
            Button(
                onClick = {
                    onboardingViewModel.setLanguage("en")
                    onLanguageSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text(
                    text = stringResource(R.string.language_english),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kannada button
            Button(
                onClick = {
                    onboardingViewModel.setLanguage("kn")
                    onLanguageSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text(
                    text = stringResource(R.string.language_kannada),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
