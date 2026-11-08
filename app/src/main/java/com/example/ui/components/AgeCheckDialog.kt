package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun AgeCheckDialog(
    hasConfirmedAge: Boolean,
    onConfirmAdult: () -> Unit
) {
    val context = LocalContext.current
    var showUnderageWarning by remember { mutableStateOf(false) }

    if (!hasConfirmedAge) {
        if (!showUnderageWarning) {
            AlertDialog(
                onDismissRequest = { /* Require deliberate interaction */ },
                title = {
                    Text(
                        text = stringResource(R.string.age_check_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.age_check_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmAdult,
                        modifier = Modifier.testTag("age_confirm_adult_button")
                    ) {
                        Text(stringResource(R.string.age_check_adult))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showUnderageWarning = true },
                        modifier = Modifier.testTag("age_confirm_underage_button")
                    ) {
                        Text(stringResource(R.string.age_check_underage))
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = {
                    (context as? Activity)?.finishAffinity()
                },
                title = {
                    Text(
                        text = stringResource(R.string.age_check_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.age_check_underage_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            (context as? Activity)?.finishAffinity()
                        },
                        modifier = Modifier.testTag("age_underage_exit_button")
                    ) {
                        Text(stringResource(R.string.age_check_close_app))
                    }
                }
            )
        }
    }
}
