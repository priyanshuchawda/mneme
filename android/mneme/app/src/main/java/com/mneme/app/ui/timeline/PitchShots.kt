package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mneme.app.data.CardType
import com.mneme.app.data.IngestSources
import com.mneme.app.data.NoteEntity

enum class PitchOverlay { None, Messy, Pulse, Focus, Gallery, Action, Live }

@Composable
fun PitchLiveFlowScreen() {
    var stage by remember { mutableStateOf(0) }
    val paper = Color(0xFFF3F1EB)
    val ink = Color(0xFF14202B)
    val green = Color(0xFF2E806D)
    val stages = listOf("Your context", "Retenqo Brief", "Ask memory", "Cited answer", "Conflict check", "Trust boundary", "Close the loop")
    val captions = listOf("Messages are scattered across five apps.", "Retenqo turns them into a focused Brief.", "Ask one question instead of searching everywhere.", "Every answer keeps its source visible.", "Two sources disagree? Both stay visible.", "No match? Retenqo refuses to guess.", "One tap removes the commitment from today’s queue.")
    Column(Modifier.fillMaxSize().background(paper).statusBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PRIVATE MEMORY OS", color = green, fontWeight = FontWeight.Bold)
            Text("RETENQO", color = ink.copy(alpha = .55f), fontWeight = FontWeight.Bold)
        }
        Text(stages[stage], style = MaterialTheme.typography.headlineMedium, color = ink)
        Text(captions[stage], style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = .7f))
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFD7DDD9), RoundedCornerShape(18.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (stage) {
                0 -> { Text("Chats · unread 47", color = ink, fontWeight = FontWeight.Bold); MessyBubble("Rahul: pay me ₹500 by Friday for dinner", "WhatsApp · Today 09:14"); MessyBubble("Priya: can we do 5 instead?", "WhatsApp · Today 11:02") }
                1 -> { Text("BRIEF · 3 OPEN · CATCH-UP READY", color = green, fontWeight = FontWeight.Bold); Text("Pay Rahul ₹500 · Friday", color = ink); Text("Submit Chapter 11 · 25 Aug", color = ink); Text("Priya · 4pm Calendar vs 5pm WhatsApp", color = ink) }
                2 -> { Text("Ask your private memory", color = ink, fontWeight = FontWeight.Bold); Text("what do I owe?", color = ink, modifier = Modifier.background(Color(0xFFE9EEE9), RoundedCornerShape(12.dp)).padding(12.dp)) }
                3 -> { Text("You owe / have due:", color = ink, fontWeight = FontWeight.Bold); Text("[1] Rahul — ₹500 by Friday\n[2] Priya — Thursday 4pm\n[3] Chapter 11 — submit by 25 Aug\n[4] Netflix — ₹1,299 charged", color = ink); Text("4 cited notes · answered on-device", color = green, fontWeight = FontWeight.Bold) }
                4 -> { Text("Two times, two sources:", color = ink, fontWeight = FontWeight.Bold); Text("Calendar — Priya, Thu 4:00pm\nWhatsApp — Priya, Thu 5:00pm", color = ink); Text("Conflict preserved · no silent overwrite", color = Color(0xFFB55B2A), fontWeight = FontWeight.Bold) }
                5 -> { Text("What’s my passport number?", color = ink); Text("I don’t have that in your memory.", color = ink, modifier = Modifier.background(Color(0xFFE9EEE9), RoundedCornerShape(12.dp)).padding(12.dp)); Text("No match · no hallucination", color = green, fontWeight = FontWeight.Bold) }
                else -> { Text("Pay Rahul ₹500", style = MaterialTheme.typography.titleLarge, color = ink); Text(if (stage == 6) "Due Friday · WhatsApp source [1]" else "Removed from today’s due list ✓", color = if (stage == 6) ink.copy(alpha = .7f) else green, fontWeight = if (stage == 6) FontWeight.Normal else FontWeight.Bold); if (stage == 6) Button(onClick = { stage = 7 }) { Text("Mark done") } else Text("No more reminder · today’s queue is focused.", color = green) }
            }
        }
        if (stage < 6) Button(onClick = { stage++ }, modifier = Modifier.fillMaxWidth()) { Text(if (stage == 0) "Build my Brief" else "Continue") }
        if (stage == 6) Text("ONE-TAP ACTION · close the commitment", color = Color(0xFFB55B2A), fontWeight = FontWeight.Bold)
        if (stage == 7) Text("ACTION COMPLETE · attention queue reduced", color = green, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PitchActionScreen(done: Boolean, onDone: () -> Unit) {
    val paper = Color(0xFFF3F1EB)
    val ink = Color(0xFF14202B)
    val green = Color(0xFF2E806D)
    Column(
        modifier = Modifier.fillMaxSize().background(paper).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BRIEF · 3 OPEN · CATCH-UP READY", color = green, fontWeight = FontWeight.Bold)
        Text("Close the loop in one tap.", style = MaterialTheme.typography.displaySmall, color = ink)
        Text("The commitment becomes an outcome—not another item to remember.", style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = .7f))
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFD7DDD9), RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pay Rahul ₹500", style = MaterialTheme.typography.headlineSmall, color = ink)
            Text("Due Friday · WhatsApp source [1]", color = ink.copy(alpha = .7f))
            if (!done) {
                Button(onClick = onDone) { Text("Mark done") }
            } else {
                Text("Removed from today’s due list ✓", color = green, fontWeight = FontWeight.Bold)
                Text("No more reminder for this item · the next Brief stays focused.", color = green)
            }
        }
        Text(if (done) "ACTION COMPLETE · attention queue reduced" else "ONE-TAP ACTION · close the commitment", color = if (done) green else Color(0xFFB55B2A), fontWeight = FontWeight.Bold)
    }
}

object PitchCards {
    private fun note(
        id: Long,
        title: String,
        body: String,
        source: String,
        type: CardType,
        who: String? = null,
        createdAgoMs: Long = 3_600_000L,
    ) = NoteEntity(
        id = id,
        title = title,
        body = body,
        source = source,
        createdAt = System.currentTimeMillis() - createdAgoMs,
        contentHash = "pitch-$id",
        cardType = type.storage,
        who = who,
    )

    val rahul = note(
        1, "Pay Rahul ₹500",
        "Friday · dinner last week · WhatsApp",
        IngestSources.WA_NOTIF, CardType.DUE, "Rahul",
    )
    val priyaCal = note(
        2, "Priya · Thursday 4pm",
        "Calendar · cafe near gate 2",
        IngestSources.CALENDAR_NOTIF, CardType.ASK_FOR_YOU, "Priya",
    )
    val priyaWa = note(
        3, "Priya · Thursday 5pm",
        "WhatsApp · “can we do 5 instead?”",
        IngestSources.WA_NOTIF, CardType.WAITING, "Priya",
        createdAgoMs = 2_000_000L,
    )
    val neha = note(
        4, "Invoice from Neha",
        "Promised “tonight” · 3 days quiet",
        IngestSources.WA_NOTIF, CardType.WAITING, "Neha",
        createdAgoMs = 4L * 24 * 3600_000,
    )
    val chapter = note(
        5, "Submit Chapter 11",
        "LMS · by 25 Aug · viva Monday",
        "LMS notif", CardType.DUE,
    )
    val netflix = note(
        6, "Netflix ₹1,299",
        "Gmail · INR 1,299 charged",
        IngestSources.GMAIL_NOTIF, CardType.DUE,
    )
    val otp = note(
        7, "OTP screen (dummy)",
        "Text card kept. Pixel file unlinked. Gallery has no capture.",
        IngestSources.PULSE, CardType.FACT,
    )
    val deck = note(
        8, "Ship deck · Monday",
        "From Focus · spoken standup",
        "Focus", CardType.DUE, "Priya",
    )

    val home = listOf(rahul, priyaCal, priyaWa, neha)
    val dues = listOf(rahul, priyaCal, chapter, netflix)
    val conflict = listOf(priyaCal, priyaWa)

    const val OWE_ANSWER = """You owe / have due:
[1] Rahul — ₹500 by Friday (dinner)
[2] Priya — Thursday 4pm (Calendar)
[3] Chapter 11 — submit by 25 Aug
[4] Netflix — ₹1,299 charged"""

    const val CONFLICT_ANSWER = """Two times, two sources:
[1] Calendar — Priya, Thu 4:00pm, cafe near gate 2
[2] WhatsApp — Priya, Thu 5:00pm “running late, 5?”"""

    const val REFUSE_ANSWER = "I don't have that in your memory."

    const val BRIEF_HEADLINE = "3 open · catch-up ready"
    const val BRIEF_BODY = """Due
• Pay Rahul ₹500 — Friday · WA notif
• Submit Chapter 11 — 25 Aug · LMS

Conflict
• Priya Thu 4pm (Calendar) vs 5pm (WhatsApp)

Waiting
• Invoice from Neha — 3 days quiet"""
}

@Composable
fun PitchMessyScreen() {
    val paper = Color(0xFFF3F0E8)
    val ink = Color(0xFF121A17)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Text("Chats — unread 47", style = MaterialTheme.typography.titleLarge, color = ink)
        Spacer(Modifier.height(14.dp))
        MessyBubble("Priya: cafe near gate 2 at 4pm thu?", "Calendar ping forwarded · Yesterday")
        MessyBubble("Priya: wait can we do 5 instead I’m in lab", "WhatsApp · Today 11:02")
        MessyBubble("Rahul: pay me ₹500 by Friday for dinner", "WhatsApp · Today 09:14")
        MessyBubble("Mam: submit ch-11 by 25/08 and viva monday", "TY BTech · 2d")
        MessyBubble("Netflix: INR 1,299 charged", "Email notification")
        Spacer(Modifier.weight(1f))
        Text(
            "Five apps. Zero cards. Friday is the cost.",
            color = Color(0xFFB55B2A),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MessyBubble(text: String, meta: String) {
    Column(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth(0.92f)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFCFC8BC), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF121A17))
        Text(meta, style = MaterialTheme.typography.labelLarge, color = Color(0xFF5C584E))
    }
}

@Composable
fun PitchPulseBody() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                "DUMMY SCREEN — not real\n\nOTP 4821 for login\nValid 30s · do not share",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun PitchFocusBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("Hearing + seeing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "“Ship the deck by Monday, Priya has the figures.”\n\nFrame sample · Whisper · RulesExtract",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun PitchGalleryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F0E8))
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recents", style = MaterialTheme.typography.headlineMedium)
            Text("Photos", style = MaterialTheme.typography.labelLarge, color = Color(0xFF5C584E))
        }
        Spacer(Modifier.weight(1f))
        Text("No Pulse capture here", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Screenshot was processed, then deleted.\nThe commitment lives as a text card.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5C584E),
        )
        Spacer(Modifier.weight(1f))
    }
}
