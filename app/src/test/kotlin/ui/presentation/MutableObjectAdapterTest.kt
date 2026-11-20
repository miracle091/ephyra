package org.jellyfin.androidtv.ui.presentation

import android.database.Observable
import androidx.leanback.widget.ObjectAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly

class MutableObjectAdapterTest : FunSpec({
	class RecordingObserver : ObjectAdapter.DataObserver() {
		val events = mutableListOf<String>()

		override fun onChanged() {
			events += "changed"
		}

		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			events += "inserted $positionStart+$itemCount"
		}

		override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
			events += "removed $positionStart+$itemCount"
		}

		override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
			events += "changed $positionStart+$itemCount"
		}

		override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
			events += "changed $positionStart+$itemCount"
		}

		override fun onItemMoved(fromPosition: Int, toPosition: Int) {
			events += "moved $fromPosition>$toPosition"
		}
	}

	// The JVM's android.database.Observable is a stub with no observer list: give the adapter's observable a real one
	fun MutableObjectAdapter<*>.observe(): RecordingObserver {
		val observable = ObjectAdapter::class.java.getDeclaredField("mObservable").apply { isAccessible = true }.get(this)
		val observer = RecordingObserver()
		Observable::class.java.getDeclaredField("mObservers").apply { isAccessible = true }.set(observable, arrayListOf(observer))
		return observer
	}

	test("addAll appends the page after the loaded items with one insertion notice") {
		val adapter = MutableObjectAdapter<String>()
		val observer = adapter.observe()
		adapter.addAll(listOf("a", "b"))
		observer.events.clear()

		adapter.addAll(listOf("c", "d", "e"))

		adapter.toList() shouldContainExactly listOf("a", "b", "c", "d", "e")
		observer.events shouldContainExactly listOf("inserted 2+3")
	}

	test("addAll with no elements notifies nothing") {
		val adapter = MutableObjectAdapter<String>()
		val observer = adapter.observe()

		adapter.addAll(emptyList())

		adapter.toList().shouldBeEmpty()
		observer.events.shouldBeEmpty()
	}
})
