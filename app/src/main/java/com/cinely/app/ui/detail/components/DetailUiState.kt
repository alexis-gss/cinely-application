package com.cinely.app.ui.detail.components

import com.cinely.app.data.MovieDetail
import com.cinely.app.data.TvDetail

/**
 * État de chargement unifié de la fiche détail (film OU série). Remplace les deux sealed
 * interfaces séparées d'origine : un seul écran (DetailScreen) gère les deux types de
 * média selon `mediaType`, donc un seul état suffit.
 */
sealed interface DetailScreenState {
    data object Loading : DetailScreenState
    data class MovieSuccess(val movie: MovieDetail) : DetailScreenState
    data class TvSuccess(val tvShow: TvDetail) : DetailScreenState
    data class Error(val message: String) : DetailScreenState
}
