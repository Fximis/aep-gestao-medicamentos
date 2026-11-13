package views.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Componentes visuais compartilhados, sem bibliotecas externas. */
public final class Tema {
    public static final Color FUNDO = new Color(244, 247, 246);
    public static final Color TEXTO = new Color(31, 54, 51);
    public static final Color SUAVE = new Color(105, 124, 119);
    public static final Color VERDE = new Color(20, 125, 110);
    public static final Color MENU = new Color(23, 58, 53);
    public static final Color BORDA = new Color(225, 233, 229);
    public static final Color ALERTA = new Color(173, 74, 54);

    private Tema() { }

    public static void instalar() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        Font fonte = new Font("SansSerif", Font.PLAIN, 13);
        for (String chave : new String[]{"Label.font", "Button.font", "ToggleButton.font",
                "TextField.font", "Spinner.font", "Table.font", "TableHeader.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "ProgressBar.font"}) {
            UIManager.put(chave, fonte);
        }
        UIManager.put("Panel.background", FUNDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("OptionPane.background", FUNDO);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.selectionBackground", new Color(201, 232, 222));
        UIManager.put("ProgressBar.foreground", VERDE);
        UIManager.put("ProgressBar.background", new Color(226, 237, 231));
        UIManager.put("ProgressBar.selectionBackground", TEXTO);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
    }

    public static JLabel texto(String texto, int tamanho, boolean negrito, Color cor) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho));
        label.setForeground(cor);
        return label;
    }

    public static JPanel card(LayoutManager layout) {
        JPanel painel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(BORDA);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        painel.setOpaque(false);
        painel.setBackground(Color.WHITE);
        painel.setBorder(new EmptyBorder(18, 20, 18, 20));
        return painel;
    }

    public static JButton botao(String texto, boolean principal) {
        JButton botao = new JButton(texto);
        estilizarBotao(botao, principal);
        return botao;
    }

    public static void estilizarBotao(AbstractButton botao, boolean principal) {
        botao.setUI(new BasicButtonUI());
        botao.setFont(new Font("SansSerif", Font.BOLD, 12));
        botao.setForeground(principal ? Color.WHITE : TEXTO);
        botao.setBackground(principal ? VERDE : Color.WHITE);
        botao.setOpaque(false);
        botao.setContentAreaFilled(false);
        botao.setFocusPainted(false);
        botao.setRolloverEnabled(true);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setBorder(new EmptyBorder(12, 16, 12, 16));
        botao.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = b.getBackground();
                g2.setColor(b.getModel().isPressed() ? base.darker()
                        : b.getModel().isRollover() ? (principal ? base.brighter() : FUNDO) : base);
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
                if (!principal) {
                    g2.setColor(BORDA);
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
                }
                if (b.hasFocus()) {
                    g2.setColor(principal ? new Color(169, 226, 209) : VERDE);
                    g2.drawRoundRect(3, 3, c.getWidth() - 7, c.getHeight() - 7, 9, 9);
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    public static void tabela(JTable tabela, int colunaStatus, String palavraAlerta) {
        tabela.setRowHeight(54);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setBackground(Color.WHITE);
        tabela.setForeground(TEXTO);
        tabela.setSelectionBackground(new Color(222, 240, 232));
        tabela.setSelectionForeground(TEXTO);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tabela.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                    boolean f, int row, int col) {
                super.getTableCellRendererComponent(t, v, s, f, row, col);
                setBackground(FUNDO);
                setForeground(SUAVE);
                setFont(new Font("SansSerif", Font.BOLD, 11));
                setBorder(new EmptyBorder(10, 12, 10, 12));
                return this;
            }
        });
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            { putClientProperty("html.disable", true); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean selecionado,
                    boolean foco, int row, int col) {
                super.getTableCellRendererComponent(t, v, selecionado, foco, row, col);
                boolean alerta = String.valueOf(t.getValueAt(row, colunaStatus)).contains(palavraAlerta);
                setBackground(selecionado ? t.getSelectionBackground()
                        : row % 2 == 0 ? Color.WHITE : new Color(249, 251, 250));
                setForeground(col == colunaStatus ? (alerta ? ALERTA : VERDE) : TEXTO);
                setFont(new Font("SansSerif", col == 1 || col == colunaStatus ? Font.BOLD : Font.PLAIN,
                        col == colunaStatus ? 11 : 12));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(239, 243, 240)),
                        new EmptyBorder(8, 12, 8, 12)));
                setToolTipText(v == null ? null : "Valor: " + v);
                return this;
            }
        });
    }

    public static JScrollPane rolagem(Component conteudo) {
        JScrollPane scroll = new JScrollPane(conteudo);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) { return botaoInvisivel(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return botaoInvisivel(); }
            private JButton botaoInvisivel() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(Color.WHITE);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !c.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(181, 202, 192));
                g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 7, 7);
                g2.dispose();
            }
        });
        return scroll;
    }
}
