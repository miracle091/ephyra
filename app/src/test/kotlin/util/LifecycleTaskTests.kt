package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LifecycleTaskTests : FunSpec({
	test("Runs pre-execute, background work and post-execute in order") {
		runBlocking {
			val calls = mutableListOf<String>()
			val done = CompletableDeferred<Int>()
			val task = object : LifecycleTask<Int, Int>(this) {
				override fun onPreExecute() {
					calls += "pre"
				}

				override fun doInBackground(params: Array<out Int>): Int {
					calls += "background"
					return params.sum()
				}

				override fun onPostExecute(result: Int) {
					calls += "post"
					done.complete(result)
				}
			}

			task.execute(2, 3)

			withTimeout(5_000) { done.await() } shouldBe 5
			calls shouldBe listOf("pre", "background", "post")
		}
	}

	test("Stops when its scope is cancelled and skips post-execute") {
		val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
		val started = CountDownLatch(1)
		val stopped = CountDownLatch(1)
		var postExecuted = false
		val task = object : LifecycleTask<Unit, Unit>(scope) {
			override fun doInBackground(params: Array<out Unit>) {
				started.countDown()
				while (!isCancelled()) Thread.sleep(1)
				stopped.countDown()
			}

			override fun onPostExecute(result: Unit) {
				postExecuted = true
			}
		}

		task.execute()
		started.await(5, TimeUnit.SECONDS) shouldBe true
		scope.cancel()

		stopped.await(5, TimeUnit.SECONDS) shouldBe true
		Thread.sleep(50)
		postExecuted shouldBe false
	}

	test("Cancel stops the background work") {
		val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
		val started = CountDownLatch(1)
		val stopped = CountDownLatch(1)
		val task = object : LifecycleTask<Unit, Unit>(scope) {
			override fun doInBackground(params: Array<out Unit>) {
				started.countDown()
				while (!isCancelled()) Thread.sleep(1)
				stopped.countDown()
			}
		}

		task.execute()
		started.await(5, TimeUnit.SECONDS) shouldBe true
		task.cancel()

		stopped.await(5, TimeUnit.SECONDS) shouldBe true
	}
})
