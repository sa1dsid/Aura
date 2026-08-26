package com.aura.feature.ioni.data

import android.content.Context
import com.aura.R
import com.aura.feature.ioni.domain.model.FaqEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IoniFaqRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun entries(): List<FaqEntry> {
        val questions = context.resources.getStringArray(R.array.ioni_faq_questions)
        val answers = context.resources.getStringArray(R.array.ioni_faq_answers)

        return questions.zip(answers) { question, answer -> FaqEntry(question, answer) }
    }

    fun stubAnswer(): String = context.getString(R.string.ioni_stub_answer)
}
