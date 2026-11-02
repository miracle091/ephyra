package org.jellyfin.androidtv.util.permission

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RuntimePermissionTests : FunSpec({
	test("granted permission proceeds") {
		decidePermissionRequest(granted = true, shouldShowRationale = false, alreadyRequested = false) shouldBe
			PermissionRequestAction.PROCEED
		decidePermissionRequest(granted = true, shouldShowRationale = true, alreadyRequested = true) shouldBe
			PermissionRequestAction.PROCEED
	}

	test("first denial asks the user") {
		decidePermissionRequest(granted = false, shouldShowRationale = false, alreadyRequested = false) shouldBe
			PermissionRequestAction.REQUEST
	}

	test("denial with rationale asks again") {
		decidePermissionRequest(granted = false, shouldShowRationale = true, alreadyRequested = true) shouldBe
			PermissionRequestAction.REQUEST
	}

	test("permanent denial after a request is reported as denied") {
		decidePermissionRequest(granted = false, shouldShowRationale = false, alreadyRequested = true) shouldBe
			PermissionRequestAction.DENIED
	}
})
