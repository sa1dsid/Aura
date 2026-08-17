package com.aura.feature.account.presentation.menu.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.account.domain.model.AccountProfile
import com.aura.feature.onboarding.domain.model.AuthProvider

private val HeaderHeight = 48.dp

private val RowContentHeight = 24.dp

private val RowIconSize = 24.dp

private val DividerInset = 3.5.dp

private const val ICON_ALPHA = 0.8f

@Composable
fun AccountMenuHeader(
    profile: AccountProfile?,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = profile?.let { stringResource(R.string.account_handle, it.handle) }.orEmpty(),
                style = AuraTheme.typography.cardTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )

            Text(
                text = profile?.let {
                    stringResource(
                        R.string.menu_account_meta,
                        it.email,
                        stringResource(it.authProvider.labelRes()),
                    )
                }.orEmpty(),
                style = AuraTheme.typography.body,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AccountMenuRow(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = AuraTheme.colors.textBright,
    iconColor: Color = AuraTheme.colors.textBright,
    role: Role? = null,
    trailingGap: Dp = 14.dp,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = if (isPressed) colors.surfaceElevated else Color.Transparent,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "menu-row-background",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = role,
                onClick = onClick,
            )
            .padding(top = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RowContentHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconColor.copy(alpha = ICON_ALPHA),
                modifier = Modifier.size(RowIconSize),
            )

            Spacer(Modifier.size(8.dp))

            Text(
                text = label,
                style = AuraTheme.typography.cardTitle,
                color = labelColor,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            if (trailing != null) {
                Spacer(Modifier.size(trailingGap))
                trailing()
            }
        }

        Spacer(Modifier.height(14.dp))

        AccountMenuDivider()
    }
}

@Composable
fun AccountMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = DividerInset)
            .height(1.dp)
            .background(AuraTheme.colors.border)
    )
}

private fun AuthProvider.labelRes(): Int = when (this) {
    AuthProvider.GOOGLE -> R.string.menu_provider_google
    AuthProvider.EMAIL -> R.string.menu_provider_email
}
