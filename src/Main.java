import views.ConsoleView;
import views.gui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;

/** Integra o menu de Carlos Henrique sem alterar o ponto de entrada da GUI existente. */
public class Main {
    public static void main(String[] args) {
        boolean console = GraphicsEnvironment.isHeadless();
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg)) console = true;
        }
        if (console) {
            new ConsoleView().iniciar();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // O tema padrao do Swing continua disponivel.
            }
            new MainWindow().setVisible(true);
        });
    }
}
