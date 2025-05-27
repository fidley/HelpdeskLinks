package com.abapblog.helpdesklink.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

import com.abapblog.helpdesklink.detector.ScopeType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class HyperlinkPatternPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Table table;
	private List<PatternConfig> patterns = new ArrayList<>();
	public static final String PATTERNS_FILE = "hyperlinkPatterns.json";
	public static final String PREDEFINEDS_FILE = "hyperlinkPredefineds.json";
	private static List<String[]> PREDEFINED_REGEXES = new ArrayList<>();
	private static List<String[]> PREDEFINED_LINKS = new ArrayList<>();

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Composite createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		String[] headers = { "Label", "Pattern", "Link Pattern", "Scope" };
		for (String header : headers) {
			org.eclipse.swt.widgets.TableColumn col = new org.eclipse.swt.widgets.TableColumn(table, SWT.NONE);
			col.setText(header);
			col.setWidth(150);
		}

		Button addButton = new Button(container, SWT.PUSH);
		addButton.setText("Add");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEditDialog(null);
			}
		});

		Button editButton = new Button(container, SWT.PUSH);
		editButton.setText("Edit");
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = table.getSelectionIndex();
				if (idx >= 0)
					openEditDialog(patterns.get(idx));
			}
		});

		Button removeButton = new Button(container, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = table.getSelectionIndex();
				if (idx >= 0) {
					patterns.remove(idx);
					refreshTable();
				}
			}
		});

		loadPatterns();
		loadPredefineds();
		refreshTable();
		return container;
	}

	private void openEditDialog(PatternConfig config) {
		PatternConfigDialog dialog = new PatternConfigDialog(getShell(), config, PREDEFINED_REGEXES, PREDEFINED_LINKS);
		PatternConfig result = dialog.openDialog();
		if (result != null && config == null) {
			patterns.add(result);
		}
		refreshTable();
	}

	private Text createLabeledText(Composite parent, String label, String value) {
		new Label(parent, SWT.NONE).setText(label);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		return text;
	}

	private void refreshTable() {
		table.removeAll();
		for (PatternConfig config : patterns) {
			String scopeStr = config.scopeType != null ? config.scopeType.toString() : "independent";
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(new String[] { config.label, config.pattern, config.linkPattern, scopeStr });
		}
	}

	public static String getFilePath(String fileName) {
		try {
			Bundle bundle = Platform.getBundle("com.abapblog.helpdesklink");
			IPath stateLoc = Platform.getStateLocation(bundle);
			File file = new File(stateLoc.toFile(), fileName);
			if (!file.exists()) {
				file.createNewFile();
				getDefaultFile(fileName, bundle, file);
			}
			return file.getAbsolutePath();
		} catch (Exception e) {
			return fileName; // fallback
		}
	}

	private static void getDefaultFile(String fileName, Bundle bundle, File file)
			throws IOException, FileNotFoundException {
		File defaultFile = new File(FileLocator.toFileURL(bundle.getEntry("/" + fileName)).getFile());
		if (defaultFile.exists()) {
			try (FileReader reader = new FileReader(defaultFile); FileWriter writer = new FileWriter(file)) {
				char[] buffer = new char[1024];
				int bytesRead;
				while ((bytesRead = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	private void loadPatterns() {
		try {
			String configPath = getFilePath(PATTERNS_FILE);
			Gson gson = new Gson();
			FileReader reader = new FileReader(configPath);
			patterns = gson.fromJson(reader, new TypeToken<List<PatternConfig>>() {
			}.getType());
			reader.close();
			if (patterns == null)
				patterns = new ArrayList<>();
			// Convert legacy string scopeType to enum
			for (PatternConfig config : patterns) {
				if (config.scopeType == null) {
					try {
						java.lang.reflect.Field f = config.getClass().getDeclaredField("scopeType");
						f.setAccessible(true);
						Object val = f.get(config);
						if (val instanceof String) {
							config.scopeType = ScopeType.fromString((String) val);
						}
					} catch (Exception ignore) {
					}
					if (config.scopeType == null)
						config.scopeType = ScopeType.INDEPENDENT;
				}
			}
		} catch (Exception e) {
			patterns = new ArrayList<>();
		}
	}

	private void savePatterns() {
		try {
			String configPath = getFilePath(PATTERNS_FILE);
			Gson gson = new Gson();
			FileWriter writer = new FileWriter(configPath);
			// Convert enum to string for JSON
			List<PatternConfig> toSave = new ArrayList<>();
			for (PatternConfig config : patterns) {
				PatternConfig copy = new PatternConfig();
				copy.label = config.label;
				copy.pattern = config.pattern;
				copy.patternName = config.patternName;
				copy.linkPattern = config.linkPattern;
				copy.linkName = config.linkName;
				copy.scopeType = config.scopeType;
				copy.projects = config.projects;
				copy.workingsets = config.workingsets;
				toSave.add(copy);
			}
			gson.toJson(toSave, writer);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadPredefineds() {
		try {
			Bundle bundle = Platform.getBundle("com.abapblog.helpdesklink");
			File file = new File(FileLocator.toFileURL(bundle.getEntry("/" + PREDEFINEDS_FILE)).getFile());
			String configPath = file.getAbsolutePath();
			Gson gson = new Gson();
			FileReader reader = new FileReader(configPath);
			Predefineds predefineds = gson.fromJson(reader, Predefineds.class);
			reader.close();
			if (predefineds != null) {
				PREDEFINED_REGEXES = predefineds.regexes;
				PREDEFINED_LINKS = predefineds.links;
			}

		} catch (Exception e) {
			PREDEFINED_REGEXES = new ArrayList<>();
			PREDEFINED_LINKS = new ArrayList<>();
		}
	}

	@Override
	public boolean performOk() {
		savePatterns();
		return true;
	}

	@Override
	protected void performDefaults() {
		loadPatterns();
		loadPredefineds();
		refreshTable();
		super.performDefaults();
	}

	// Helper class for JSON structure
	private static class Predefineds {
		List<String[]> regexes;
		List<String[]> links;
	}
}
