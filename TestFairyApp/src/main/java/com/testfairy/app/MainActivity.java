package com.testfairy.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.webkit.CookieManager;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.content.Context;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.util.Map;

public class MainActivity extends Activity {

	private static final String MIME_TYPE_APK = "application/vnd.android.package-archive";
	private static final String USER_AGENT = "TestFairy App " + Config.BUILD + " android mobile";

//	private static final String LOGIN_URL = "https://app.testfairy.com/login/";
	private static final String LOGIN_URL = "http://my.giltsl.gs.dev.testfairy.net/my/";
	private static final String TEMP_DOWNLOAD_FILE = "testfairy-app-download.apk";

	private File localFile;
	private WebView webView;
	private ProgressDialog dialog;
	private FileDownloader downloader;
	private ProgressBar progressBar;

	private boolean isLoginPage = false;

	private class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.contains("/download/")) {
				startDownload(url);
			}
			return false;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			setPageProgress(100);

			if (url.contains("/login")) {
				isLoginPage = true;
			} else {
				if (isLoginPage) {
					//if the last page was login delete the browser history, so the cant go back to this page
					Log.d("console", "clearHistory");
					view.clearHistory();
				}
				isLoginPage = false;
			}
			//webView.loadUrl("javascript:window.TFAPP.foo(document.getElementsByTagName('html')[0].innerHTML);");

			/*
			String cookies = CookieManager.getInstance().getCookie(url);
			if (cookies != null) {
				CookieUtils utils = new CookieUtils();
				Map<String, String> map = utils.parseCookieString(cookies);
//				Log.v(Config.TAG, "COOKIE: " + map.get("u") + ", url=" + url);

				if (map.containsKey("u") && !url.startsWith("https://app.testfairy.com")) {
					CookieManager.getInstance().setCookie("https://app.testfairy.com/login/", map.get("u"));
				}
			}
			*/
		}
	}

	long backPressedTime = 0;
	public class WebAppInterface {

		Activity activity;

		public WebAppInterface(Activity activity) {
			this.activity = activity;
		}

		@JavascriptInterface
		public void doBackPressed() {
			if (webView.canGoBack()) {
				Log.d("console WebAppInterface", "canGoBack -> go back");
				webView.goBack();
			} else {
				//exit the app
				if (backPressedTime + 2000 > System.currentTimeMillis()) {
					Log.d("console WebAppInterface", "exit the app -> exit");
					activity.finish();
				} else {
					Log.d("console WebAppInterface", "exit the app -> start timer");
					Toast.makeText(activity, "Press once again to exit!", Toast.LENGTH_SHORT).show();
					backPressedTime = System.currentTimeMillis();
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (isLoginPage) {
			if (backPressedTime + 2000 > System.currentTimeMillis()) {
				Log.d("onBackPressed LoginPage", "exit the app -> exit");
				finish();
			} else {
				Log.d("onBackPressed LoginPage", "exit the app -> start timer");
				Toast.makeText(this, "Press once again to exit!", Toast.LENGTH_SHORT).show();
				backPressedTime = System.currentTimeMillis();
			}
		} else {
			webView.loadUrl("javascript:MyController.onAndroidBackPressed()");

		}


	}

	private class MyWebChromeClient extends WebChromeClient {


		public void onProgressChanged(WebView view, int progress) {
			setPageProgress(progress);
		}

		public void onConsoleMessage(String message, int lineNumber, String sourceID) {
			Log.d("console ", message + " -- From line " + lineNumber + " of " + sourceID);
		}
	}

	private void setPageProgress(int progress) {
		progressBar.setProgress(progress);
		progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.INVISIBLE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setVisibility(View.GONE);

		webView = (WebView) findViewById(R.id.webView);
		webView.setWebViewClient(new MyWebViewClient());
		webView.setWebChromeClient(new MyWebChromeClient());

		webView.addJavascriptInterface(new WebAppInterface(this), "Android");

		// enable javascript. must be called before addJavascriptInterface
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSavePassword(false);


//		webSettings.setUserAgentString(USER_AGENT);
//		webView.addJavascriptInterface(new MyJSObject(), "TFAPP");

//		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
//		String uCookie = prefs.getString("u", null);
//		if (uCookie != null) {
//			editor.putBoolean(Strings.TERMS_APPROVED_PREF, true);
//			editor.commit();
//			CookieManager.getInstance().setCookie("u=" + uCookie);
//		}

//		SharedPreferences.Editor editor = prefs.edit();

		webView.loadUrl(LOGIN_URL);
	}

	private DialogInterface.OnCancelListener onDialogCancelled = new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			if (downloader != null) {
				Log.v(Config.TAG, "User pressed on cancel");
				downloader.abort();
				downloader = null;
			}
		}
	};

	/**
	 * Display an AlertDialog on screen
	 *
	 * @param message
	 * @return
	 */
	private void alertWithMessage(String message)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setCancelable(true);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.show();
	}

	/**
	 * Start downloading APK from url
	 *
	 * @param url
	 * @return
	 */
	private void startDownload(String url) {

		// find where to keep files locally
		File storageDir = getExternalFilesDir(null);
		if (storageDir == null) {
			Log.i(Config.TAG, "getExternalFilesDir() returned null");
			storageDir = getFilesDir();
			if (storageDir == null) {
				Log.e(Config.TAG, "getFilesDir() also returned null!");
				alertWithMessage("Could not download app to device");
				return;
			}
		}

		localFile = new File(storageDir.getAbsolutePath() + "/" + TEMP_DOWNLOAD_FILE);
		Log.v(Config.TAG, "Using " + localFile.getAbsolutePath() + " for storing apk locally");

		dialog = new ProgressDialog(this);
//		dialog.setMax(0);
//		dialog.setTitle("Please Wait");
		dialog.setMessage("Downloading..");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setIndeterminate(true);
		dialog.setOnCancelListener(onDialogCancelled);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(false);

		if (Build.VERSION.SDK_INT >= 14) {
			dialog.setProgressNumberFormat("%1d KB/%2d KB");
		}

		dialog.show();

		//buildUpgradeUrl = "http://app.testfairy.com/download/64VK8C1D68S2VP1RT7NQA30Z145VWJA5ACJNNZTF5TFAC/MakeMeBald_v1.1-testfairy.apk";
		downloader = new FileDownloader(url, localFile);
		downloader.setDownloadListener(downloadListener);

		// get cookies from web client
		String cookies = CookieManager.getInstance().getCookie(url);
		if (cookies != null) {
			CookieUtils utils = new CookieUtils();
			Map<String, String> map = utils.parseCookieString(cookies);
//				Log.v(Config.TAG, "COOKIE: " + map.get("u") + ", url=" + url);

			for (String key: map.keySet()) {
				Log.v(Config.TAG, "Copying cookie " + key + " = " + map.get(key) + " to file downloader");
				downloader.addCookie(key, map.get(key));
			}
		}

		downloader.start();
	}

	private FileDownloader.DownloadListener downloadListener = new FileDownloader.DownloadListener() {

		private long fileSize;
		private long lastPrintout;
		private long downloadStartedTimestamp;

		private static final long THRESHOLD = 256*1024;

		@Override
		public void onDownloadStarted() {
			fileSize = 0;
			lastPrintout = 0;
			downloadStartedTimestamp = System.currentTimeMillis();
		}

		@Override
		public void onDownloadFailed() {
		}

		@Override
		public void onDownloadCompleted() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.setIndeterminate(true);
					dialog.dismiss();
				}
			});

			Log.v(Config.TAG, String.format("Done downloading %.2f KB", fileSize/1000.0f));

			long diff = System.currentTimeMillis() - downloadStartedTimestamp;
			float secs = diff/1000.0f;
			Log.v(Config.TAG, String.format("Downloaded completed in %.2f seconds", secs));

			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(localFile), MIME_TYPE_APK);
			startActivity(intent);
		}

		@Override
		public void onDownloadProgress(int offset, int total) {
			dialog.setIndeterminate(false);
			dialog.setMax(total >> 10);
			dialog.setProgress(offset >> 10);

			if ((offset - lastPrintout) >= THRESHOLD) {
				lastPrintout = offset;
				Log.d(Config.TAG, "Download progress: " + offset + " / " + total);
			}

			this.fileSize = total;
		}
	};
}
