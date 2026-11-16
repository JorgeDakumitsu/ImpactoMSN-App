package com.impactomsn.app.pages

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.impactomsn.app.AppLanguage
import com.impactomsn.app.LanguagePrefs
import com.impactomsn.app.R
import com.impactomsn.app.pages.components.PageContentList

@Composable
fun LanguageSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(LanguagePrefs.current(context)) }
    val options = listOf(AppLanguage(LanguagePrefs.AUTOMATIC, "")) + LanguagePrefs.languages

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        options.forEachIndexed { index, language ->
            item(key = "language-${language.tag.ifEmpty { "auto" }}") {
                SegmentedListItem(
                    selected = selected == language.tag,
                    onClick = {
                        if (selected != language.tag) {
                            selected = language.tag
                            if (LanguagePrefs.set(context, language.tag)) {
                                (context as? Activity)?.recreate()
                            }
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index, options.size),
                    modifier = Modifier.settingsRow().testTag("language-${language.tag.ifEmpty { "auto" }}"),
                    trailingContent = { RadioButton(selected = selected == language.tag, onClick = null) },
                    content = {
                        Text(
                            if (language.tag.isEmpty()) stringResource(R.string.language_automatic)
                            else language.nativeName,
                        )
                    },
                )
            }
        }
    }
}
