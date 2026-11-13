import auth.Autenticacao;
import views.ConsoleView;
import views.gui.LoginWindow;
import views.gui.Tema;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/** Inicia o acesso autenticado na interface gráfica ou no console. */
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
            Tema.instalar();
            new LoginWindow(new Autenticacao()).setVisible(true);
        });
    }
}
