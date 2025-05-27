package com.abapblog.helpdesklink.detector;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.program.Program;

public class PatternHyperlink implements IHyperlink {
	private final IRegion region;
	private final String url;
	private final String label;

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
		Program.launch(url);
	}
}
