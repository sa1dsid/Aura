package com.aura.feature.transactions.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.transactions.domain.model.TransactionFilter

@Composable
fun TransactionFilterRow(
    selected: TransactionFilter,
    onFilterClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TransactionFilter.entries.forEach { filter ->
            AuraPill(
                text = stringResource(filter.labelRes),
                contentColor = if (filter == selected) colors.accentBlue else colors.textDisabled,
                borderColor = colors.accentBlue.copy(alpha = 0.55f),
                backgroundColor = colors.accentBlue.copy(alpha = 0.22f),
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp,
                borderWidth = 0.5.dp,
                textStyle = AuraTheme.typography.caption.copy(letterSpacing = 0.08.sp),
                onClick = { onFilterClick(filter) },
            )
        }
    }
}
