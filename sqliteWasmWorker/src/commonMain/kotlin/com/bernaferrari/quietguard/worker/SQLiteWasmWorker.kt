package com.bernaferrari.quietguard.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver