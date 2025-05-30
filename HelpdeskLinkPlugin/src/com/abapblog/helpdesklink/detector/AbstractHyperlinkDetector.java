package com.abapblog.helpdesklink.detector;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.abapblog.helpdesklink.preferences.HyperlinkPatternPreferencePage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AbstractHyperlinkDetector implements IHyperlinkDetector {

	private static final ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
	private static final IWorkingSetManager wsm = PlatformUI.getWorkbench().getWorkingSetManager();
	private static List<PatternConfig> patterns;

	static {
		loadPatterns();
	}

	private static class PatternConfig {
		String label;
		String pattern;
		String patternName;
		String linkPattern;
		String linkName;
		ScopeType scopeType; // "independent", "project", "workspace"
		List<String> projects;
		List<String> workingsets;
	}

	public static void loadPatterns() {
		try {
			String configPath = HyperlinkPatternPreferencePage
					.getFilePath(HyperlinkPatternPreferencePage.PATTERNS_FILE);
			Gson gson = new Gson();
			FileReader reader = new FileReader(configPath);
			patterns = gson.fromJson(reader, new TypeToken<List<PatternConfig>>() {
			}.getType());
			reader.close();
			// Convert scopeType from string to enum if needed
			for (PatternConfig config : patterns) {
				if (config.scopeType == null)
					config.scopeType = ScopeType.INDEPENDENT;
			}

		} catch (Exception e) {
			e.printStackTrace();
			patterns = new ArrayList<>();
		}
	}

	private boolean isPatternApplicable(PatternConfig config, ITextViewer viewer) {
		if (config.scopeType == null || config.scopeType == ScopeType.INDEPENDENT) {
			return true;
		}
		IProject currentProject = null;
		List<String> currentWorkingSets = new ArrayList<>();
		try {
			IEditorInput input = findEditorInputForViewer(viewer);
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				currentProject = file.getProject();

				for (IWorkingSet ws : wsm.getWorkingSets()) {
					if (ws != null && ws.getElements() != null) {
						for (Object element : ws.getElements()) {
							if (element instanceof IFile && ((IFile) element).getProject().equals(currentProject)) {
								currentWorkingSets.add(ws.getName());
							} else if (element instanceof IProject && ((IProject) element).equals(currentProject)) {
								currentWorkingSets.add(ws.getName());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// fallback: not in a file editor
		}
		if (config.scopeType == ScopeType.PROJECT && currentProject != null && config.projects != null) {
			return config.projects.contains(currentProject.getName());
		}
		if (config.scopeType == ScopeType.WORKINGSET && config.workingsets != null && !currentWorkingSets.isEmpty()) {
			for (String ws : currentWorkingSets) {
				if (config.workingsets.contains(ws)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

	private IEditorInput findEditorInputForViewer(ITextViewer viewer) {
		IDocument targetDoc = viewer.getDocument();
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		for (IEditorReference ref : page.getEditorReferences()) {
			IEditorPart ed = ref.getEditor(false);
			if (ed == null) {
				continue;
			}
			IFileEditorInput input = ed.getEditorInput().getAdapter(IFileEditorInput.class);
			if (input != null) {
				IFile iFile = input.getFile();
				ITextFileBuffer iTextFileBuffer = null;
				try {
					iTextFileBufferManager.connect(iFile.getFullPath(), LocationKind.IFILE, null);
					iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(iFile.getFullPath(), LocationKind.IFILE);
					IDocument editorDoc = iTextFileBuffer.getDocument();
					iTextFileBufferManager.disconnect(iFile.getFullPath(), LocationKind.IFILE, null);
					if (editorDoc == targetDoc) {
						return ed.getEditorInput();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			ITextEditor editor = ref.getEditor(false) instanceof ITextEditor ? (ITextEditor) ref.getEditor(false)
					: null;
			if (editor != null) {
				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				if (doc == targetDoc) {
					return editor.getEditorInput();
				}
			}
		}
		return null;

	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
		List<IHyperlink> hyperlinks = new ArrayList<>();
		try {
			IDocument document = viewer.getDocument();
			int offset = region.getOffset();
			IRegion lineInfo = document.getLineInformationOfOffset(offset);
			String line = document.get(lineInfo.getOffset(), lineInfo.getLength());

			for (PatternConfig config : patterns) {
				if (!isPatternApplicable(config, viewer))
					continue;
				Pattern pattern = Pattern.compile(config.pattern);
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) {
					int start = matcher.start();
					int end = matcher.end();
					if (offset >= start + lineInfo.getOffset() && offset <= end + lineInfo.getOffset()) {
						String url = config.linkPattern;
						String label = config.label;
						for (int i = 1; i <= matcher.groupCount(); i++) {
							url = url.replace("${" + i + "}", matcher.group(i));
							label = label.replace("${" + i + "}", matcher.group(i));
						}
						url = url.replace("${match}", matcher.group());
						label = label.replace("${match}", matcher.group());
						// check URL validity
						hyperlinks.add(new PatternHyperlink(new Region(start + lineInfo.getOffset(), end - start), url,
								label));
					}
				}
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		if (hyperlinks.isEmpty()) {
			return null; // No hyperlinks found
		}
		return hyperlinks.toArray(new IHyperlink[0]);
	}
}
