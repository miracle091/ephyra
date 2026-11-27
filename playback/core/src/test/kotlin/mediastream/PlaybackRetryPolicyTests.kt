package org.jellyfin.playback.core.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.model.PlaybackError.Reason
import org.jellyfin.playback.core.queue.StreamFallback
import org.jellyfin.playback.core.queue.allowsDirectPlay
import org.jellyfin.playback.core.queue.allowsDirectStream
import kotlin.time.Duration.Companion.seconds

class PlaybackRetryPolicyTests : FunSpec({
	var now = 0L
	fun policy() = PlaybackRetryPolicy(clock = { now }).also { now = 0L }

	test("a stream error drops direct play first, then asks for a transcode, then gives up") {
		val policy = policy()

		policy.onError(Reason.SOURCE, null) shouldBe StreamFallback.NO_DIRECT_PLAY
		now += 5.seconds.inWholeMilliseconds
		policy.onError(Reason.SOURCE, StreamFallback.NO_DIRECT_PLAY) shouldBe StreamFallback.TRANSCODE
		now += 5.seconds.inWholeMilliseconds
		policy.onError(Reason.SOURCE, StreamFallback.TRANSCODE) shouldBe null
	}

	test("a decoder error goes to a transcode at once: a remux carries the same codec") {
		policy().onError(Reason.DECODER, null) shouldBe StreamFallback.TRANSCODE
	}

	test("an entry never climbs back to a lighter stream than the one it already falls back to") {
		policy().onError(Reason.SOURCE, StreamFallback.TRANSCODE) shouldBe StreamFallback.TRANSCODE
	}

	test("after 30 s without errors the attempts count again from the start") {
		val policy = policy()
		policy.onError(Reason.SOURCE, null)
		now += 5.seconds.inWholeMilliseconds
		policy.onError(Reason.SOURCE, StreamFallback.NO_DIRECT_PLAY)

		now += 31.seconds.inWholeMilliseconds
		policy.onError(Reason.SOURCE, StreamFallback.TRANSCODE) shouldBe StreamFallback.TRANSCODE
	}

	test("each fallback tells the server which streams it may still offer") {
		val nothing: StreamFallback? = null
		nothing.allowsDirectPlay shouldBe true
		nothing.allowsDirectStream shouldBe true
		StreamFallback.NO_DIRECT_PLAY.allowsDirectPlay shouldBe false
		StreamFallback.NO_DIRECT_PLAY.allowsDirectStream shouldBe true
		StreamFallback.TRANSCODE.allowsDirectPlay shouldBe false
		StreamFallback.TRANSCODE.allowsDirectStream shouldBe false
	}

	test("a new entry starts with no attempts") {
		val policy = policy()
		policy.onError(Reason.SOURCE, null)
		policy.onError(Reason.SOURCE, StreamFallback.NO_DIRECT_PLAY)

		policy.reset()
		policy.onError(Reason.SOURCE, null) shouldBe StreamFallback.NO_DIRECT_PLAY
	}
})
