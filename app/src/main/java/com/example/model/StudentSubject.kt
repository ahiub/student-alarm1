package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.SubjectAssignment
import com.example.ui.theme.SubjectChemistry
import com.example.ui.theme.SubjectComputer
import com.example.ui.theme.SubjectExam
import com.example.ui.theme.SubjectGeneral
import com.example.ui.theme.SubjectHistory
import com.example.ui.theme.SubjectLiterature
import com.example.ui.theme.SubjectMath
import com.example.ui.theme.SubjectPhysics

data class SubjectInfo(
    val name: String,
    val color: Color,
    val icon: ImageVector,
    val description: String
)

object StudentSubjects {
    val ALL = listOf(
        SubjectInfo("Math", SubjectMath, Icons.Default.Calculate, "Calculus, Algebra, Geometry"),
        SubjectInfo("Physics", SubjectPhysics, Icons.Default.Science, "Mechanics, Quantum, Thermodynamics"),
        SubjectInfo("Chemistry", SubjectChemistry, Icons.Default.Science, "Organic, Lab, Reactions"),
        SubjectInfo("Computer Science", SubjectComputer, Icons.Default.Code, "Coding, Data Structures, Algorithms"),
        SubjectInfo("Literature", SubjectLiterature, Icons.Default.MenuBook, "Reading, Essays, Languages"),
        SubjectInfo("History", SubjectHistory, Icons.Default.Psychology, "World History, Civics, Events"),
        SubjectInfo("Exam", SubjectExam, Icons.Default.WarningAmber, "Midterms, Finals, Pop Quizzes"),
        SubjectInfo("Assignment", SubjectAssignment, Icons.Default.Assignment, "Homework, Projects, Papers"),
        SubjectInfo("General", SubjectGeneral, Icons.Default.EditNote, "Daily wake-up, Routine, Study block")
    )

    fun getByName(name: String): SubjectInfo {
        return ALL.find { it.name.equals(name, ignoreCase = true) } ?: ALL.last()
    }
}
