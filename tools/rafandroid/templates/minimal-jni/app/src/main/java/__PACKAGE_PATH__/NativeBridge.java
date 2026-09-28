package __PACKAGE__;

final class NativeBridge {
    static {
        System.loadLibrary("rafminimal");
    }

    private NativeBridge() {}

    static native int nativeHealth();

    static String health() {
        return nativeHealth() == 0x52414631 ? "PASS" : "FAIL";
    }
}
