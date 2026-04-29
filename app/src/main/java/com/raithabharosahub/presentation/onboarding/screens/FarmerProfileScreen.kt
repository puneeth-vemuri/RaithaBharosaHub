package com.raithabharosahub.presentation.onboarding.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raithabharosahub.R
import com.raithabharosahub.presentation.onboarding.OnboardingViewModel
import com.raithabharosahub.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerProfileScreen(
    onNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    var name by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var crop by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }

    var cropExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    val crops = listOf(
        stringResource(id = R.string.crop_sugarcane),
        stringResource(id = R.string.crop_ragi),
        stringResource(id = R.string.crop_paddy),
        stringResource(id = R.string.crop_custom)
    )

    val districts = listOf(
        stringResource(id = R.string.district_bengaluru_urban),
        stringResource(id = R.string.district_bengaluru_rural),
        stringResource(id = R.string.district_mysuru),
        stringResource(id = R.string.district_belagavi),
        stringResource(id = R.string.district_kalaburagi),
        stringResource(id = R.string.district_davanagere),
        stringResource(id = R.string.district_ballari),
        stringResource(id = R.string.district_tumakuru),
        stringResource(id = R.string.district_shivamogga),
        stringResource(id = R.string.district_hassan)
    )

    val validName = name.trim().isNotEmpty()
    val validMobile = mobile.length == 10 && mobile.all { it.isDigit() }
    val canProceed = validName && validMobile && crop.isNotEmpty() && district.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = stringResource(id = R.string.farmer_profile_title))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.full_name)) },
            isError = !validName && name.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = {
                if (it.length <= 10 && it.all(Char::isDigit)) {
                    mobile = it
                }
            },
            label = { Text(stringResource(id = R.string.mobile_number)) },
            isError = mobile.isNotEmpty() && !validMobile,
            supportingText = {
                if (mobile.isNotEmpty() && !validMobile) {
                    Text(text = stringResource(id = R.string.mobile_validation_error))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = crop,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.primary_crop)) },
            trailingIcon = {
                TextButton(onClick = { cropExpanded = true }) {
                    Text(text = stringResource(id = R.string.select))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = cropExpanded, onDismissRequest = { cropExpanded = false }) {
            crops.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        crop = item
                        cropExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = district,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.district)) },
            trailingIcon = {
                TextButton(onClick = { districtExpanded = true }) {
                    Text(text = stringResource(id = R.string.select))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
            districts.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        district = item
                        districtExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.saveProfile(name, mobile, crop, district)
                onNext()
            },
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.next))
        }
    }
}
