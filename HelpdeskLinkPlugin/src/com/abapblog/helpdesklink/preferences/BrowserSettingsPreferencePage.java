package com.abapblog.helpdesklink.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.abapblog.helpdesklink.Activator;

public class BrowserSettingsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public static final String BROWSER_BEHAVIOR = "browserBehavior";
    public static final String DEFAULT = "default";
    public static final String INTERNAL = "internal";
    public static final String EXTERNAL = "external";

    public BrowserSettingsPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Choose how hyperlinks should be opened:");
    }

    @Override
    public void createFieldEditors() {
        addField(new RadioGroupFieldEditor(
            BROWSER_BEHAVIOR,
            "Open hyperlinks in:",
            1,
            new String[][] {
                {"Eclipse default setting", DEFAULT},
                {"Internal browser", INTERNAL},
                {"External browser", EXTERNAL}
            },
            getFieldEditorParent(),
            true
        ));
    }

    @Override
    public void init(IWorkbench workbench) {
        // No initialization needed
    }
}
