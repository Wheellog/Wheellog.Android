package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.cooper.wheellog.views.WheelView

enum class Page { Main, Params, Trips, Events, BMS }

@Composable
fun MainScreen() {
    val pages = listOf( Page.Main, Page.Params, Page.Trips, Page.Events, Page.BMS )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold { padding ->
        Column(Modifier.padding(padding)) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = pages.size // все страницы кешируются и не перерендериваются
            ) { index ->
                // Our page content
                when (pages[index]) {
                    Page.Main -> LegacyMainView()
                    Page.Params -> ParamsListScreen()
                    Page.Trips -> TripsScreen()
                    Page.Events -> EventsScreen()
                    Page.BMS -> SmartBmsScreen()
                }
            }
        }
    }
}


@Composable
fun LegacyMainView() {
    AndroidView(
        factory = { ctx ->
            WheelView(ctx, null).apply {
                setSpeed(696)
                setPwm(12.3)
                setVoltage(76.5)
            }
        },
        update = { view -> /* обновить при рекомпозиции */ }
    )
}