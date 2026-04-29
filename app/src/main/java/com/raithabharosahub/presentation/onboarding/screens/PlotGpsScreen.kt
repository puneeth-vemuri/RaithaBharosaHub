package com.raithabharosahub.presentation.onboarding.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.raithabharosahub.R
import com.raithabharosahub.presentation.onboarding.OnboardingViewModel
import com.raithabharosahub.util.findActivity

@Composable
fun PlotGpsScreen(
    onNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    val context = LocalContext.current
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var plotLabel by rememberSaveable { mutableStateOf("") }

    val canProceed = latitude.toDoubleOrNull() != null &&
        longitude.toDoubleOrNull() != null &&
        plotLabel.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = stringResource(id = R.string.plot_location_title))
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val hasFine = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    fusedClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            latitude = location.latitude.toString()
                            longitude = location.longitude.toString()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.use_current_location))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text(stringResource(id = R.string.latitude)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text(stringResource(id = R.string.longitude)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = plotLabel,
            onValueChange = { plotLabel = it },
            label = { Text(stringResource(id = R.string.plot_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(
                id = R.string.selected_coordinates,
                latitude.ifBlank { stringResource(id = R.string.not_set) },
                longitude.ifBlank { stringResource(id = R.string.not_set) }
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.savePlotPin(latitude, longitude, plotLabel)
                onNext()
            },
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.next))
        }
    }
}
