//go:build android

package mobile

/*
#include <dlfcn.h>

typedef int (*android_fdsan_set_error_level_fn)(int);

// fdsan (file descriptor sanitizer) aborts the process when it detects an
// fd ownership conflict. The Go core closes system-delivered descriptors
// (e.g. the VpnService tun fd) with raw syscalls, which bypasses libc's
// ownership bookkeeping; on some devices (notably deep OEM customizations)
// the stale tag then trips fdsan inside libbinder on unrelated binder
// traffic and kills the app seconds after connecting.
//
// Downgrading fdsan to warn-only keeps the process alive and turns each
// detection into a log line (naming the fd and its owner tag) that we can
// use to root-cause the real conflict.
//
// Resolved via dlsym because the symbol only exists on API 29+; on older
// devices the call is simply skipped (minSdk is 24).
static void setFdsanWarnAlways() {
	android_fdsan_set_error_level_fn fn =
		(android_fdsan_set_error_level_fn)dlsym(RTLD_DEFAULT, "android_fdsan_set_error_level");
	if (fn != NULL) {
		// ANDROID_FDSAN_ERROR_LEVEL_WARN_ALWAYS == 2
		fn(2);
	}
}
*/
import "C"

func init() {
	C.setFdsanWarnAlways()
}
