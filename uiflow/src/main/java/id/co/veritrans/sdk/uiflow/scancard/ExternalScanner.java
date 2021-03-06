package id.co.veritrans.sdk.uiflow.scancard;

import android.app.Activity;

import id.co.veritrans.sdk.coreflow.core.IScanner;

/**
 * Abstract class of external scanner implementation.
 *
 * @author rakawm
 */
public abstract class ExternalScanner implements IScanner {

    public static final String EXTRA_SCAN_DATA = "scan.data";

}
