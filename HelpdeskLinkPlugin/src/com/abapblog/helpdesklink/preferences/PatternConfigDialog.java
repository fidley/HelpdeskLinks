package com.abapblog.helpdesklink.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import com.abapblog.helpdesklink.detector.ScopeType;

public class PatternConfigDialog extends TitleAreaDialog {
	private PatternConfig config;
	private final List<String[]> predefinedRegexes;
	private final List<String[]> predefinedLinks;
	private PatternConfig resultConfig;

	// UI fields for access in okPressed
	private Text labelText;
	private Combo regexCombo;
	private Text patternText;
	private Combo linkCombo;
	private Text linkPatternText;
	private Combo scopeCombo;
	private CheckboxTreeViewer treeViewer;

	public PatternConfigDialog(Shell parent) {
		super(parent);
		this.predefinedRegexes = null;
		this.predefinedLinks = null;

	}

	/**
	 * @wbp.parser.constructor
	 */
	public PatternConfigDialog(Shell parent, PatternConfig config, List<String[]> predefinedRegexes,
			List<String[]> predefinedLinks) {
		super(parent);
		this.config = config;
		this.predefinedRegexes = predefinedRegexes;
		this.predefinedLinks = predefinedLinks;
	}

	@Override
	public void create() {
		super.create();
		setTitle(config == null ? "Add Pattern" : "Edit Pattern");
		setMessage("Please fill all fields", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl_container = new GridLayout(1, false);
		gl_container.marginHeight = 0;
		gl_container.horizontalSpacing = 0;
		container.setLayout(gl_container);

		Composite labelContainer = new Composite(container, SWT.NONE);
		labelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		labelContainer.setLayout(new GridLayout(2, false));
		labelText = createLabeledText(labelContainer, "Label:", config != null ? config.label : "");

		Composite regexContainer = new Composite(container, SWT.NONE);
		regexContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		regexContainer.setLayout(new GridLayout(3, false));

		createRegexFields(regexContainer);

		Composite linkContainer = new Composite(container, SWT.NONE);
		linkContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		linkContainer.setLayout(new GridLayout(3, false));

		createLinkFields(linkContainer);

		Composite scopeContainer = new Composite(container, SWT.NONE);
		scopeContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl_scopeContainer = new GridLayout(2, false);
		gl_scopeContainer.marginHeight = 0;
		scopeContainer.setLayout(gl_scopeContainer);
		// Scope selection
		createScopeFields(scopeContainer);
		new Label(scopeContainer, SWT.NONE);

		createTreeViewer(area);

		setFieldsValues();

		return area;
	}

	private void setFieldsValues() {
		if (config != null && config.scopeType != null) {
			switch (config.scopeType) {
			case PROJECT:
				scopeCombo.setText("Project(s)");
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				treeViewer.setInput(projects);
				treeViewer.getTree().setVisible(true);
				for (IProject p : projects) {
					if (config.projects != null && config.projects.contains(p.getName())) {
						treeViewer.setChecked(p, true);
					}
				}
				break;
			case WORKINGSET:
				scopeCombo.setText("Working Set(s)");
				IWorkingSetManager wsm = org.eclipse.ui.PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSet[] sets = wsm.getWorkingSets();
				String[] names = new String[sets.length];
				for (int i = 0; i < sets.length; i++)
					names[i] = sets[i].getName();
				treeViewer.setInput(names);
				treeViewer.getTree().setVisible(true);
				if (config.workingsets != null) {
					for (String ws : names) {
						if (config.workingsets.contains(ws)) {
							treeViewer.setChecked(ws, true);
						}
					}
				}
				break;
			default:
				scopeCombo.setText("Independent");
				treeViewer.getTree().setVisible(false);
				break;
			}
		} else {
			scopeCombo.setText("Independent");
			treeViewer.getTree().setVisible(false);
		}
	}

	private void createTreeViewer(Composite area) {
		treeViewer = new CheckboxTreeViewer(area, SWT.BORDER);
		treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IProject)
					return ((IProject) element).getName();
				if (element instanceof String)
					return (String) element;
				return super.getText(element);
			}
		});
		treeViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}
		});
		treeViewer.getTree().setVisible(false);
	}

	private void createScopeFields(Composite container) {
		new Label(container, SWT.NONE).setText("Scope:");
		scopeCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		scopeCombo.add("Independent");
		scopeCombo.add("Project(s)");
		scopeCombo.add("Working Set(s)");
		new Label(container, SWT.NONE);

		scopeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (scopeCombo.getSelectionIndex() == 1) { // Project(s)
					IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
					treeViewer.setInput(projects);
					treeViewer.getTree().setVisible(true);
				} else if (scopeCombo.getSelectionIndex() == 2) { // Working Set(s)
					IWorkingSetManager wsm = org.eclipse.ui.PlatformUI.getWorkbench().getWorkingSetManager();
					IWorkingSet[] sets = wsm.getWorkingSets();
					String[] names = new String[sets.length];
					for (int i = 0; i < sets.length; i++)
						names[i] = sets[i].getName();
					treeViewer.setInput(names);
					treeViewer.getTree().setVisible(true);
				} else {
					treeViewer.getTree().setVisible(false);
				}
				// scopeDetails.layout();
			}
		});
	}

	private void createLinkFields(Composite container) {
		// Link dropdown
		new Label(container, SWT.NONE).setText("Link Pattern:");
		linkCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (String[] entry : predefinedLinks)
			linkCombo.add(entry[0]);
		linkCombo.add("Custom");
		linkPatternText = new Text(container, SWT.BORDER);
		linkPatternText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		linkPatternText.setVisible(true);
		linkCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (String[] entry : predefinedLinks) {
					if (entry[0].equals(linkCombo.getText())) {
						linkPatternText.setText(entry[1]);
						break;
					}
				}

				container.layout();
			}
		});
		if (config != null && config.linkName != null) {
			linkCombo.setText(config.linkName);
			linkPatternText.setText(config.linkPattern != null ? config.linkPattern : "");
		} else if (config != null) {
			linkCombo.setText("Custom");
			linkPatternText.setText(config.linkPattern != null ? config.linkPattern : "");
		}
	}

	private void createRegexFields(Composite regexContainer) {
		// Regex dropdown
		new Label(regexContainer, SWT.NONE).setText("Pattern:");
		regexCombo = new Combo(regexContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (String[] entry : predefinedRegexes)
			regexCombo.add(entry[0]);
		regexCombo.add("Custom");
		patternText = new Text(regexContainer, SWT.BORDER);
		patternText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patternText.setVisible(true);
		regexCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				for (String[] entry : predefinedRegexes) {
					if (entry[0].equals(regexCombo.getText())) {
						patternText.setText(entry[1]);
						break;
					}
				}

				if (regexCombo.getSelectionIndex() < predefinedRegexes.size()) {
					patternText.setEditable(false);
				} else {
					patternText.setEditable(true);
				}
				regexContainer.layout();
			}
		});
		if (config != null && config.patternName != null) {
			regexCombo.setText(config.patternName);
			patternText.setText(config.pattern != null ? config.pattern : "");
		} else if (config != null) {
			regexCombo.setText("Custom");
			patternText.setText(config.pattern != null ? config.pattern : "");
		}
	}

	@Override
	protected void okPressed() {
		PatternConfig newConfig = (config == null) ? new PatternConfig() : config;
		newConfig.label = labelText.getText();
		if (regexCombo.getSelectionIndex() < predefinedRegexes.size()) {
			newConfig.patternName = regexCombo.getText();
			newConfig.pattern = predefinedRegexes.get(regexCombo.getSelectionIndex())[1];
		} else {
			newConfig.patternName = null;
			newConfig.pattern = patternText.getText();
		}
		if (linkCombo.getSelectionIndex() < predefinedLinks.size()) {
			newConfig.linkName = linkCombo.getText();
			newConfig.linkPattern = linkPatternText.getText();
		} else {
			newConfig.linkName = null;
			newConfig.linkPattern = linkPatternText.getText();
		}
		int scopeIdx = scopeCombo.getSelectionIndex();
		if (scopeIdx == 1)
			newConfig.scopeType = ScopeType.PROJECT;
		else if (scopeIdx == 2)
			newConfig.scopeType = ScopeType.WORKINGSET;
		else
			newConfig.scopeType = ScopeType.INDEPENDENT;
		if (scopeCombo.getSelectionIndex() == 1) { // Project(s)
			Object[] checked = treeViewer.getCheckedElements();
			List<String> selected = new ArrayList<>();
			for (Object o : checked) {
				if (o instanceof IProject)
					selected.add(((IProject) o).getName());
			}
			newConfig.projects = selected;
			newConfig.workingsets = null;
		} else if (scopeCombo.getSelectionIndex() == 2) { // Working Set(s)
			Object[] checked = treeViewer.getCheckedElements();
			List<String> selected = new ArrayList<>();
			for (Object o : checked) {
				if (o instanceof String)
					selected.add((String) o);
			}
			newConfig.workingsets = selected;
			newConfig.projects = null;
		} else {
			newConfig.projects = null;
			newConfig.workingsets = null;
		}
		resultConfig = newConfig;
		super.okPressed();
	}

	public PatternConfig openDialog() {
		if (super.open() == TitleAreaDialog.OK) {
			return resultConfig;
		}
		return null;
	}

	private Text createLabeledText(Composite parent, String label, String value) {
		new Label(parent, SWT.NONE).setText(label);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		return text;
	}
}
