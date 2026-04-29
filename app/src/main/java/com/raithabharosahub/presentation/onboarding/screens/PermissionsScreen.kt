package com.raithabharosahub.presentation.onboarding.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raithabharosahub.R
import com.raithabharosahub.presentation.onboarding.OnboardingViewModel
import com.raithabharosahub.util.findActivity

@Composable
fun PermissionsScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    var deniedCount by remember { mutableStateOf(0) }

    val permissionsToRequest = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        deniedCount = result.values.count { granted -> !granted }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = stringResource(id = R.string.permissions_title))
        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = stringResource(id = R.string.permission_location_title),
            description = stringResource(id = R.string.permission_location_desc)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = stringResource(id = R.string.permission_internet_title),
            description = stringResource(id = R.string.permission_internet_desc)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            title = stringResource(id = R.string.permission_notifications_title),
            description = stringResource(id = R.string.permission_notifications_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                launcher.launch(permissionsToRequest.toTypedArray())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.request_permissions))
        }

        if (deniedCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(id = R.string.limited_functionality_warning))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.markOnboardingDone(onCompleted = onFinish)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.finish_onboarding))
        }
    }
}

@Composable
private fun PermissionCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
    }
}
