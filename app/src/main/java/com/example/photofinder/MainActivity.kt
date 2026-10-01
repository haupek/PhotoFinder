package com.example.photofinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.photofinder.domain.model.Photo
import com.example.photofinder.ui.search.SearchScreen
import com.example.photofinder.ui.search.SearchViewModel
import com.example.photofinder.ui.theme.PhotoFinderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels()

    private val readImagesPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun startupReadPermissions(): Array<String> {
        val perms = mutableListOf(readImagesPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoFinderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var granted by remember { mutableStateOf(hasReadPermission()) }

                    // Step 2: media location must be requested AFTER photo access is held.
                    val mediaLocationLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { ok ->
                        if (ok) viewModel.onMediaLocationGranted()
                    }

                    // Step 1: photo access (+ notifications).
                    val readLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        val ok = result[readImagesPermission] == true || hasReadPermission()
                        granted = ok
                        if (ok && !hasMediaLocationPermission()) {
                            mediaLocationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                        }
                    }

                    if (granted) {
                        LaunchedEffect(Unit) { viewModel.onPermissionGranted() }
                        SearchScreen(
                            viewModel = viewModel,
                            onOpenPhoto = ::openPhoto
                        )
                    } else {
                        PermissionGate(onRequest = { readLauncher.launch(startupReadPermissions()) })
                    }
                }
            }
        }
    }

    private fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, readImagesPermission) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasMediaLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun openPhoto(photo: Photo) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(photo.uri), "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.permission_needed))
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}
