package org.jellyfin.androidtv.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ephyra: drop-in replacement for AsyncTask in the remaining Java screens. The work runs in [scope] (pass the view's
 * lifecycle scope), so it stops when the screen goes away instead of keeping it in memory, and [onPostExecute] never
 * runs on a dead screen.
 */
abstract class LifecycleTask<Params, Result>(private val scope: CoroutineScope) {
	private var job: Job? = null

	@Volatile
	private var backgroundJob: Job? = null

	protected open fun onPreExecute() = Unit
	protected abstract fun doInBackground(params: Array<out Params>): Result
	protected open fun onPostExecute(result: Result) = Unit

	/** For [doInBackground]: true once the task or its scope is cancelled. */
	protected fun isCancelled(): Boolean = backgroundJob?.isActive == false

	fun execute(vararg params: Params) {
		onPreExecute()
		job = scope.launch {
			val result = withContext(Dispatchers.Default) {
				backgroundJob = coroutineContext.job
				doInBackground(params)
			}
			onPostExecute(result)
		}
	}

	fun cancel() {
		job?.cancel()
	}
}
