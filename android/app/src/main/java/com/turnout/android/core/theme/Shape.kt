package com.turnout.android.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// NOTE: RoundedCornerShape(Int) without .dp is interpreted as a PERCENTAGE of the
// shape's own size, not a dp value. The previous version of this file was missing
// .dp entirely, so "8" meant "8% corner radius," not "8dp" — likely not the intended
// look. Every value below is now an explicit Dp.
val TurnoutShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Material3's Shapes() data class has no built-in "full/pill" slot — it only defines
// the five sizes above. Exposed separately here for any component that explicitly
// wants a pill shape (e.g. a status badge or chip), since MaterialTheme.shapes has
// nowhere to put it.
val TurnoutPillShape = RoundedCornerShape(50.dp)
