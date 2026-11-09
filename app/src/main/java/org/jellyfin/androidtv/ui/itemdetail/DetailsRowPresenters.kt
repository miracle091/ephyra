package org.jellyfin.androidtv.ui.itemdetail

import org.jellyfin.androidtv.ui.presentation.CardPresenter

// Ephyra: under the 252dp cover the first row has room for 120dp posters with their title and subtitle
private const val FIRST_ROW_CARD_HEIGHT_DP = 120

/**
 * Ephyra: the cards of the first row under a details screen's cover (a person's films, series and episodes, a series' seasons),
 * small enough that the row, its titles included, fits on screen without scrolling.
 */
fun createFirstRowCardPresenter(): CardPresenter = CardPresenter().apply { cardHeight = FIRST_ROW_CARD_HEIGHT_DP }
