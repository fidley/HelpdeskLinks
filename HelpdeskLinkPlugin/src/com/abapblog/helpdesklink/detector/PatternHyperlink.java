package com.abapblog.helpdesklink.detector;

import java.net.URL;
import java.util.UUID;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.abapblog.helpdesklink.Activator;
import com.abapblog.helpdesklink.preferences.BrowserSettingsPreferencePage;

public class PatternHyperlink implements IHyperlink {
	private final IRegion region;
	private final String url;
	private final String label;
	private final String[] programSchemes = { "mailto:", "tel:", "sms:", "ftp:", "file:", "news:", "callto:", "sip:",
			"skype:" };

	public PatternHyperlink(IRegion region, String url, String label) {
		this.region = region;
		this.url = url;
		this.label = label;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getTypeLabel() {
		return label;
	}

	@Override
	public String getHyperlinkText() {
		return label;
	}

	@Override
	public void open() {
		String behavior = Activator.getDefault().getPreferenceStore()
				.getString(BrowserSettingsPreferencePage.BROWSER_BEHAVIOR);
		try {
			boolean useProgramLaunch = shouldBeCalledDirectlyBySystem();
			if (useProgramLaunch) {
				Program.launch(url);
				return;
			}
			URL linkUrl = new URL(url);
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			switch (behavior) {
			case BrowserSettingsPreferencePage.INTERNAL:
				browserSupport
						.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, UUID.randomUUID().toString(), label, label)
						.openURL(linkUrl);
				break;
			case BrowserSettingsPreferencePage.EXTERNAL:
				browserSupport.getExternalBrowser().openURL(linkUrl);
				break;
			case BrowserSettingsPreferencePage.DEFAULT:
				try {
					PlatformUI.getWorkbench().getBrowserSupport().createBrowser(UUID.randomUUID().toString())
							.openURL(linkUrl);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
				break;
			default:
				Program.launch(url);
			}
		} catch (Exception e) {
			// fallback
			Program.launch(url);
		}
	}

	private boolean shouldBeCalledDirectlyBySystem() {

		boolean useProgramLaunch = false;
		for (String scheme : programSchemes) {
			if (url.startsWith(scheme)) {
				useProgramLaunch = true;
				break;
			}
		}
		return useProgramLaunch;
	}
}
