package views.gui;

import models.Medicamento;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal amigável para cadastro e edição de medicamentos.
 */
public class MedicamentoDialog extends JDialog {

    private final JTextField txtNome = new JTextField(20);
    private final JSpinner spnEstoque = new JSpinner(new SpinnerNumberModel(30, 0, 10000, 1));
    private final JSpinner spnFrequencia = new JSpinner(new SpinnerNumberModel(8, 1, 168, 1));
    private final JSpinner spnQtdDose = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));

    private boolean confirmado = false;
    private final Medicamento medicamento;

    public MedicamentoDialog(Frame parent, String titulo, Medicamento medicamentoExistente, int pacienteId) {
        super(parent, titulo, true);
        this.medicamento = medicamentoExistente != null ? medicamentoExistente : new Medicamento();
        if (medicamentoExistente == null) {
            this.medicamento.setUsuarioId(pacienteId);
        }

        configurarLayout();
        preencherDados();

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void configurarLayout() {
        setLayout(new BorderLayout(10, 10));

        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        pnlForm.add(new JLabel("Nome Comercial e Dosagem:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        pnlForm.add(txtNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        pnlForm.add(new JLabel("Quantidade Inicial em Estoque:"), gbc);
        gbc.gridx = 1;
        pnlForm.add(spnEstoque, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        pnlForm.add(new JLabel("Frequência de Doses (em horas):"), gbc);
        gbc.gridx = 1;
        pnlForm.add(spnFrequencia, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        pnlForm.add(new JLabel("Quantidade por Dose (comprimidos):"), gbc);
        gbc.gridx = 1;
        pnlForm.add(spnQtdDose, gbc);

        add(pnlForm, BorderLayout.CENTER);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnSalvar = new JButton("Salvar Medicamento");
        btnSalvar.setFont(btnSalvar.getFont().deriveFont(Font.BOLD));

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> validarESalvar());

        pnlBotoes.add(btnCancelar);
        pnlBotoes.add(btnSalvar);
        add(pnlBotoes, BorderLayout.SOUTH);
    }

    private void preencherDados() {
        if (medicamento.getId() > 0) {
            txtNome.setText(medicamento.getNome());
            spnEstoque.setValue(medicamento.getQuantidadeEstoque());
            spnFrequencia.setValue(medicamento.getFrequenciaHoras());
            spnQtdDose.setValue(medicamento.getQuantidadePorDose());
        }
    }

    private void validarESalvar() {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, informe o nome do medicamento.",
                    "Campo Obrigatório", JOptionPane.WARNING_MESSAGE);
            txtNome.requestFocus();
            return;
        }

        medicamento.setNome(nome);
        medicamento.setQuantidadeEstoque((Integer) spnEstoque.getValue());
        medicamento.setFrequenciaHoras((Integer) spnFrequencia.getValue());
        medicamento.setQuantidadePorDose((Integer) spnQtdDose.getValue());

        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }
}
