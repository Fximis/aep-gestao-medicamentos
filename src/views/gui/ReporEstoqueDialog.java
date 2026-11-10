package views.gui;

import models.Medicamento;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo rápido para reposição de estoque (compra de caixas novas).
 */
public class ReporEstoqueDialog extends JDialog {

    private final JSpinner spnAdicionar = new JSpinner(new SpinnerNumberModel(30, 1, 1000, 1));
    private boolean confirmado = false;
    private int quantidadeAdicionada = 0;

    public ReporEstoqueDialog(Frame parent, Medicamento medicamento) {
        super(parent, "Repor Estoque: " + medicamento.getNome(), true);

        setLayout(new BorderLayout(10, 10));

        JPanel pnlInfo = new JPanel(new GridLayout(3, 1, 5, 5));
        pnlInfo.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        pnlInfo.add(new JLabel("Medicamento: " + medicamento.getNome()));
        pnlInfo.add(new JLabel(String.format("Estoque Atual: %d comprimidos (Autonomia: %d dias)",
                medicamento.getQuantidadeEstoque(), medicamento.calcularDiasRestantes())));

        JPanel pnlInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlInput.add(new JLabel("Quantidade a adicionar: "));
        pnlInput.add(spnAdicionar);
        pnlInfo.add(pnlInput);

        add(pnlInfo, BorderLayout.CENTER);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnConfirmar = new JButton("Confirmar Reposição");
        btnConfirmar.setFont(btnConfirmar.getFont().deriveFont(Font.BOLD));

        btnCancelar.addActionListener(e -> dispose());
        btnConfirmar.addActionListener(e -> {
            quantidadeAdicionada = (Integer) spnAdicionar.getValue();
            confirmado = true;
            dispose();
        });

        pnlBotoes.add(btnCancelar);
        pnlBotoes.add(btnConfirmar);
        add(pnlBotoes, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public int getQuantidadeAdicionada() {
        return quantidadeAdicionada;
    }
}
