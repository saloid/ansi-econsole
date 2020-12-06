package mnita.ansiconsole;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import mnita.ansiconsole.preferences.AnsiConsolePreferenceUtils;

public class AnsiConsoleActivator extends AbstractUIPlugin {

    private static AnsiConsoleActivator plugin;
    private static boolean showWarning = true;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static AnsiConsoleActivator getDefault() {
        return plugin;
    }

    private final Map<StyledText, IConsolePageParticipant> viewers = new HashMap<>();

    public void addViewer(StyledText viewer, IConsolePageParticipant participant) {
        viewers.put(viewer, participant);
        checkConsolePerformanceSettings();
    }

    public void removeViewerWithPageParticipant(IConsolePageParticipant participant) {
        Set<StyledText> toRemove = new HashSet<>();

        for (Entry<StyledText, IConsolePageParticipant> entry : viewers.entrySet()) {
            if (entry.getValue() == participant)
                toRemove.add(entry.getKey());
        }

        for (StyledText viewer : toRemove)
            viewers.remove(viewer);
    }

    // Check some of the console settings that I know are bad for performance
    private void checkConsolePerformanceSettings() {
        if (!showWarning) return;
        if (!AnsiConsolePreferenceUtils.isPerformanceWarningEnabled()) return;

        StringBuffer where = new StringBuffer(isMacOs()
                ? "Main menu 🡒 Eclipse 🡒 Preferences... 🡒 Run Debug 🡒 Console\n"
                : "Main menu 🡒 Window 🡒 Preferences 🡒 Run/Debug 🡒 Console\n");
        StringBuffer text = new StringBuffer();
        int wattermarkLevel = AnsiConsolePreferenceUtils.getWattermarkLevel();
        if (wattermarkLevel < 150_000) {
            NumberFormat nf = NumberFormat.getInstance();
            text.append(String.format("\n"
                    + "Console buffer size too low (%s). About 2 times slower.\n\n"
                    + where
                    + "\u00a0\u00a0\u00a0\u00a0\u2022 Check \"Limit console output\"\n"
                    + "\u00a0\u00a0\u00a0\u00a0\u2022 Set \"Console buffer size (characters)\" to a bigger value.\n"
                    + "\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0The sweet spot seems to be about %s\n"
                    + "or\n"
                    + "\u00a0\u00a0\u00a0\u00a0\u2022 Uncheck \"Limit console output\"\n"
                    + "\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0No visible performance difference compared to %s\n",
                    nf.format(wattermarkLevel), nf.format(1_000_000), nf.format(1_000_000)));
        }
        if (AnsiConsolePreferenceUtils.isWordWrapEnabled()) {
            text.append("\n"
                    + "Word wrap enabled. Up to 20 times slower!!!\n\n"
                    + where
                    + "\u00a0\u00a0\u00a0\u00a0\u2022 Uncheck \"Enable word wrap\"\n");
        }

        if (text.length() > 0) {
            showWarning = false;
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

            String[] buttons = { "Remind me later", "Never remind me again" };
            int answer = MessageDialog.open(MessageDialog.WARNING, window.getShell(),
                    "Ansi Console", "CONSOLE PERFORMANCE WARNING (from Ansi Console)!\n" + text,
                    SWT.NONE, buttons);
            switch (answer) {
                case 1: // Never again
                    AnsiConsolePreferenceUtils.setEnablePerformanceWarning(false);
                    break;
                default: // Next time
                    break;
            }
        }
    }

    private static boolean isMacOs() {
        final String os = System.getProperty("os.name");
        return os != null && os.startsWith("Mac");
    }
}
